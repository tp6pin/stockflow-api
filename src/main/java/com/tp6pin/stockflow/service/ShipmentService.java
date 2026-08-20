package com.tp6pin.stockflow.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tp6pin.stockflow.dto.request.InventoryShipmentRequest;
import com.tp6pin.stockflow.dto.request.ShipmentCreateRequest;
import com.tp6pin.stockflow.dto.request.ShipmentShipRequest;
import com.tp6pin.stockflow.dto.response.ShipmentResponse;
import com.tp6pin.stockflow.entity.Order;
import com.tp6pin.stockflow.entity.OrderItemAllocation;
import com.tp6pin.stockflow.entity.Shipment;
import com.tp6pin.stockflow.entity.ShipmentItem;
import com.tp6pin.stockflow.entity.User;
import com.tp6pin.stockflow.enums.AllocationStatus;
import com.tp6pin.stockflow.enums.OrderStatus;
import com.tp6pin.stockflow.enums.ShipmentStatus;
import com.tp6pin.stockflow.exception.BusinessException;
import com.tp6pin.stockflow.exception.ErrorCode;
import com.tp6pin.stockflow.exception.ResourceNotFoundException;
import com.tp6pin.stockflow.repository.OrderItemAllocationRepository;
import com.tp6pin.stockflow.repository.OrderRepository;
import com.tp6pin.stockflow.repository.ShipmentItemRepository;
import com.tp6pin.stockflow.repository.ShipmentRepository;
import com.tp6pin.stockflow.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ShipmentService {

    private static final DateTimeFormatter
        SHIPMENT_NUMBER_DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMdd");

    private final InventoryService inventoryService;
    
    private final ShipmentRepository shipmentRepository;

    private final ShipmentItemRepository
        shipmentItemRepository;

    private final OrderRepository orderRepository;

    private final OrderItemAllocationRepository
        orderItemAllocationRepository;

    private final UserRepository userRepository;

    /**
     * 為已確認訂單建立出貨單。
     *
     * 建立時會：
     * 1. 鎖定訂單
     * 2. 驗證訂單狀態
     * 3. 取得所有 ACTIVE allocation
     * 4. 建立 ShipmentItem
     * 5. 將訂單更新為 PROCESSING
     */
    @Transactional
    public ShipmentResponse createShipment(
            Long orderId,
            ShipmentCreateRequest request,
            Long createdById
    ) {
        Order order =
            orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() ->
                    new ResourceNotFoundException(
                        "找不到 ID 為 "
                            + orderId
                            + " 的訂單"
                    )
                );

        validateOrderCanCreateShipment(order);

        User createdBy =
            findActiveUser(createdById);

        List<OrderItemAllocation> activeAllocations =
            orderItemAllocationRepository
                .findAllByOrderItem_Order_IdAndStatus(
                    orderId,
                    AllocationStatus.ACTIVE
                );

        if (activeAllocations.isEmpty()) {
            throw new BusinessException(
                ErrorCode.DATA_CONFLICT,
                "訂單沒有可建立出貨明細的庫存配置"
            );
        }

        Shipment shipment = new Shipment();

        shipment.setShipmentNumber(
            generateShipmentNumber()
        );

        shipment.setOrder(order);

        shipment.setStatus(
            ShipmentStatus.PREPARING
        );

        shipment.setCarrier(
            normalizeNullableText(request.getCarrier())
        );

        shipment.setRecipientName(
            normalizeRequiredText(
                request.getRecipientName()
            )
        );

        shipment.setRecipientPhone(
            normalizeNullableText(
                request.getRecipientPhone()
            )
        );

        shipment.setShippingAddress(
            normalizeRequiredText(
                request.getShippingAddress()
            )
        );

        shipment.setNote(
            normalizeNullableText(request.getNote())
        );

        shipment.setCreatedBy(createdBy);

        for (OrderItemAllocation allocation
                : activeAllocations) {

            if (
                shipmentItemRepository
                    .existsByAllocation_Id(
                        allocation.getId()
                    )
            ) {
                throw new BusinessException(
                    ErrorCode.DATA_CONFLICT,
                    "庫存配置 ID "
                        + allocation.getId()
                        + " 已存在出貨明細"
                );
            }

            ShipmentItem shipmentItem =
                new ShipmentItem();

            shipmentItem.setAllocation(allocation);

            shipmentItem.setQuantity(
                allocation.getAllocatedQuantity()
            );

            shipment.addItem(shipmentItem);
        }

        LocalDateTime processingAt =
            LocalDateTime.now();

        order.setStatus(OrderStatus.PROCESSING);
        order.setProcessingAt(processingAt);

        orderRepository.save(order);

        Shipment savedShipment =
            shipmentRepository.save(shipment);

        return ShipmentResponse.from(savedShipment);
    }

    /**
     * 執行實際出貨。
     *
     * 流程：
     * 1. 鎖定出貨單。
     * 2. 驗證出貨單與訂單狀態。
     * 3. 逐筆扣除已預留的批次庫存。
     * 4. 將 Allocation 改為 SHIPPED。
     * 5. 將 Shipment 改為 SHIPPED。
     * 6. 將 Order 改為 SHIPPED。
     *
     * 因為使用 @Transactional，
     * 任一出貨項目失敗時，整個交易都會回滾。
     */
    @Transactional
    public ShipmentResponse shipShipment(
            Long shipmentId,
            ShipmentShipRequest request
    ) {
    	/*
    	 * 固定按照 Order → Shipment 的順序加鎖，
    	 * 防止出貨與取消同時修改相同資料。
    	 */
    	Shipment shipment =
    	    findShipmentWithOrderLock(shipmentId);

    	Order order = shipment.getOrder();

        /*
         * 只有備貨中的出貨單可以實際出貨。
         */
        if (
            shipment.getStatus()
                != ShipmentStatus.PREPARING
        ) {
            throw new BusinessException(
                ErrorCode.INVALID_STATUS_TRANSITION,
                "只有 PREPARING 狀態的出貨單可以執行出貨"
            );
        }

        /*
         * 出貨單所屬訂單必須處於處理中。
         */
        if (order.getStatus() != OrderStatus.PROCESSING) {
            throw new BusinessException(
                ErrorCode.INVALID_STATUS_TRANSITION,
                "只有 PROCESSING 狀態的訂單可以執行出貨"
            );
        }

        String trackingNumber =
            request.getTrackingNumber().trim();

        /*
         * 物流追蹤編號必須唯一。
         */
        if (
            shipmentRepository.existsByTrackingNumber(
                trackingNumber
            )
        ) {
            throw new BusinessException(
                ErrorCode.DUPLICATE_RESOURCE,
                "物流追蹤編號已存在："
                    + trackingNumber
            );
        }

        /*
         * 防止空出貨單進入 SHIPPED。
         */
        if (
            shipment.getItems() == null
                || shipment.getItems().isEmpty()
        ) {
            throw new BusinessException(
                ErrorCode.DATA_CONFLICT,
                "出貨單沒有任何出貨項目"
            );
        }

        /*
         * 所有狀態與時間欄位使用同一個時間。
         */
        LocalDateTime shippedAt =
            LocalDateTime.now();

        /*
         * 逐筆處理出貨單項目。
         */
        for (ShipmentItem shipmentItem
                : shipment.getItems()) {

            OrderItemAllocation allocation =
                shipmentItem.getAllocation();

            /*
             * 只有 ACTIVE Allocation 可以出貨。
             *
             * RELEASED：已釋放
             * SHIPPED：已出貨
             */
            if (
                allocation.getStatus()
                    != AllocationStatus.ACTIVE
            ) {
                throw new BusinessException(
                    ErrorCode.DATA_CONFLICT,
                    "庫存配置已不是 ACTIVE 狀態，"
                        + "allocationId："
                        + allocation.getId()
                );
            }

            /*
             * 建立實際出庫 Request。
             */
            InventoryShipmentRequest inventoryRequest =
                new InventoryShipmentRequest();

            /*
             * 使用訂單確認時透過 FEFO
             * 所預留的庫存批次。
             */
            inventoryRequest.setBatchId(
                allocation.getBatch().getId()
            );

            /*
             * 實際出庫數量使用 ShipmentItem 的數量。
             */
            inventoryRequest.setQuantity(
                shipmentItem.getQuantity()
            );

            /*
             * 必須與預留庫存時使用的
             * referenceType 完全相同。
             */
            inventoryRequest.setReferenceType(
                "ORDER_ITEM"
            );

            /*
             * 必須與預留庫存時使用的
             * referenceId 完全相同。
             */
            inventoryRequest.setReferenceId(
                allocation.getOrderItem().getId()
            );

            /*
             * 在庫存異動紀錄留下出貨單號。
             */
            inventoryRequest.setNote(
                "出貨單："
                    + shipment.getShipmentNumber()
            );

            /*
             * 實際出庫：
             * 1. quantityOnHand 減少
             * 2. quantityReserved 減少
             * 3. 建立 SHIPMENT 庫存異動紀錄
             */
            inventoryService.shipInventory(
                inventoryRequest
            );

            /*
             * 實際出庫成功後，
             * 將 Allocation 標記為已出貨。
             */
            allocation.setStatus(
                AllocationStatus.SHIPPED
            );

            allocation.setShippedAt(shippedAt);
        }

        /*
         * 所有項目出庫成功後，
         * 更新出貨單狀態。
         */
        shipment.setStatus(ShipmentStatus.SHIPPED);
        shipment.setTrackingNumber(trackingNumber);
        shipment.setShippedAt(shippedAt);

        /*
         * 更新訂單狀態。
         */
        order.setStatus(OrderStatus.SHIPPED);
        order.setShippedAt(shippedAt);

        orderRepository.save(order);

        Shipment savedShipment =
            shipmentRepository.save(shipment);

        return ShipmentResponse.from(savedShipment);
    }
    
    /**
     * 完成配送。
     *
     * 狀態變化：
     * Shipment：SHIPPED → DELIVERED
     * Order：SHIPPED → COMPLETED
     *
     * 商品已在 shipShipment() 實際出庫，
     * 因此這裡不再修改庫存與 Allocation。
     */
    @Transactional
    public ShipmentResponse completeDelivery(
            Long shipmentId
    ) {
    	/*
    	 * 固定按照 Order → Shipment 的順序加鎖。
    	 */
    	Shipment shipment =
    	    findShipmentWithOrderLock(shipmentId);

    	Order order = shipment.getOrder();

        /*
         * 只有已出貨的出貨單才能完成配送。
         */
        if (
            shipment.getStatus()
                != ShipmentStatus.SHIPPED
        ) {
            throw new BusinessException(
                ErrorCode.INVALID_STATUS_TRANSITION,
                "只有 SHIPPED 狀態的出貨單可以完成配送"
            );
        }

        /*
         * 所屬訂單也必須是已出貨狀態。
         */
        if (order.getStatus() != OrderStatus.SHIPPED) {
            throw new BusinessException(
                ErrorCode.INVALID_STATUS_TRANSITION,
                "只有 SHIPPED 狀態的訂單可以完成配送"
            );
        }

        /*
         * 出貨單與訂單使用相同的完成時間。
         */
        LocalDateTime completedAt =
            LocalDateTime.now();

        /*
         * 出貨單標記為已送達。
         */
        shipment.setStatus(
            ShipmentStatus.DELIVERED
        );
        shipment.setDeliveredAt(completedAt);

        /*
         * 訂單標記為已完成。
         */
        order.setStatus(OrderStatus.COMPLETED);
        order.setCompletedAt(completedAt);

        orderRepository.save(order);

        Shipment savedShipment =
            shipmentRepository.save(shipment);

        return ShipmentResponse.from(savedShipment);
    }
    
    /**
     * 驗證訂單是否可以建立出貨單。
     */
    private void validateOrderCanCreateShipment(
            Order order
    ) {
        if (order.getStatus() != OrderStatus.CONFIRMED) {
            throw new BusinessException(
                ErrorCode.INVALID_STATUS_TRANSITION,
                "只有已確認的訂單可以建立出貨單"
            );
        }

        if (
            shipmentRepository.existsByOrder_Id(
                order.getId()
            )
        ) {
            throw new BusinessException(
                ErrorCode.DUPLICATE_RESOURCE,
                "此訂單已建立出貨單"
            );
        }
    }

    /**
     * 按照固定順序鎖定訂單與出貨單。
     *
     * 鎖定順序：
     * 1. 先查詢 Shipment 所屬的 orderId
     * 2. 鎖定 Order
     * 3. 鎖定 Shipment
     *
     * 取消、出貨與配送完成都使用相同順序，
     * 避免不同交易互相等待而產生死鎖。
     */
    private Shipment findShipmentWithOrderLock(
            Long shipmentId
    ) {
        /*
         * 此查詢只取得 orderId，不會鎖定資料。
         */
        Long orderId = shipmentRepository
            .findOrderIdByShipmentId(shipmentId)
            .orElseThrow(() ->
                new ResourceNotFoundException(
                    "找不到 ID 為 "
                        + shipmentId
                        + " 的出貨單"
                )
            );

        /*
         * 第一個悲觀鎖：Order。
         */
        orderRepository
            .findByIdForUpdate(orderId)
            .orElseThrow(() ->
                new ResourceNotFoundException(
                    "找不到 ID 為 "
                        + orderId
                        + " 的訂單"
                )
            );

        /*
         * 第二個悲觀鎖：Shipment。
         *
         * 等待取得鎖後會重新讀取最新狀態，
         * 後續的狀態驗證可以阻止重複出貨、
         * 已取消後出貨等操作。
         */
        return shipmentRepository
            .findByIdForUpdate(shipmentId)
            .orElseThrow(() ->
                new ResourceNotFoundException(
                    "找不到 ID 為 "
                        + shipmentId
                        + " 的出貨單"
                )
            );
    }
    
    /**
     * 查詢並驗證啟用中的建立人。
     */
    private User findActiveUser(Long userId) {
        User user =
            userRepository.findById(userId)
                .orElseThrow(() ->
                    new ResourceNotFoundException(
                        "找不到 ID 為 "
                            + userId
                            + " 的使用者"
                    )
                );

        if (!Boolean.TRUE.equals(user.getActive())) {
            throw new BusinessException(
                ErrorCode.DATA_CONFLICT,
                "停用中的使用者不可建立出貨單"
            );
        }

        return user;
    }

    /**
     * 產生出貨單編號。
     *
     * 格式：
     * SHP-20260821-A1B2C3D4
     */
    private String generateShipmentNumber() {
        String datePart =
            LocalDateTime.now()
                .format(
                    SHIPMENT_NUMBER_DATE_FORMAT
                );

        String randomPart =
            UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 8)
                .toUpperCase();

        return "SHP-"
            + datePart
            + "-"
            + randomPart;
    }

    private String normalizeRequiredText(
            String value
    ) {
        return value.trim();
    }

    private String normalizeNullableText(
            String value
    ) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }
}