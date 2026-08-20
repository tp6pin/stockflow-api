package com.tp6pin.stockflow.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tp6pin.stockflow.dto.request.InventoryReleaseRequest;
import com.tp6pin.stockflow.dto.request.InventoryReservationRequest;
import com.tp6pin.stockflow.dto.request.OrderCreateRequest;
import com.tp6pin.stockflow.dto.request.OrderItemCreateRequest;
import com.tp6pin.stockflow.dto.request.OrderItemUpdateRequest;
import com.tp6pin.stockflow.dto.request.OrderUpdateRequest;
import com.tp6pin.stockflow.dto.response.InventoryReservationBatchResponse;
import com.tp6pin.stockflow.dto.response.InventoryReservationResponse;
import com.tp6pin.stockflow.dto.response.OrderResponse;
import com.tp6pin.stockflow.entity.Customer;
import com.tp6pin.stockflow.entity.InventoryBatch;
import com.tp6pin.stockflow.entity.Order;
import com.tp6pin.stockflow.entity.OrderItem;
import com.tp6pin.stockflow.entity.OrderItemAllocation;
import com.tp6pin.stockflow.entity.Product;
import com.tp6pin.stockflow.entity.Shipment;
import com.tp6pin.stockflow.entity.User;
import com.tp6pin.stockflow.enums.AllocationStatus;
import com.tp6pin.stockflow.enums.OrderStatus;
import com.tp6pin.stockflow.enums.ShipmentStatus;
import com.tp6pin.stockflow.exception.BusinessException;
import com.tp6pin.stockflow.exception.ErrorCode;
import com.tp6pin.stockflow.exception.ResourceNotFoundException;
import com.tp6pin.stockflow.repository.CustomerRepository;
import com.tp6pin.stockflow.repository.InventoryBatchRepository;
import com.tp6pin.stockflow.repository.OrderItemAllocationRepository;
import com.tp6pin.stockflow.repository.OrderRepository;
import com.tp6pin.stockflow.repository.ProductRepository;
import com.tp6pin.stockflow.repository.ShipmentRepository;
import com.tp6pin.stockflow.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrderService {

    /**
     * 台灣一般營業稅率 5%。
     */
    private static final BigDecimal TAX_RATE =
        new BigDecimal("0.05");

    private static final DateTimeFormatter
        ORDER_NUMBER_DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMdd");

    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final InventoryBatchRepository inventoryBatchRepository;
    private final OrderItemAllocationRepository orderItemAllocationRepository;
    private final ShipmentRepository shipmentRepository;
    private final InventoryService inventoryService;



    /**
     * 建立草稿訂單。
     *
     * createdById 暫時由 Controller 傳入。
     * JWT 完成後，改為從登入使用者取得。
     */
    @Transactional
    public OrderResponse createOrder(
            OrderCreateRequest request,
            Long createdById
    ) {
        Customer customer =
            findActiveCustomer(request.getCustomerId());

        User createdBy =
            findActiveUser(createdById);

        validateDuplicateProducts(request);

        Order order = new Order();

        order.setOrderNumber(generateOrderNumber());
        order.setCustomer(customer);
        order.setCreatedBy(createdBy);
        order.setStatus(OrderStatus.DRAFT);
        order.setOrderDate(LocalDateTime.now());
        order.setExpectedDeliveryDate(
            request.getExpectedDeliveryDate()
        );
        order.setNote(
            normalizeNullableText(request.getNote())
        );

        BigDecimal subtotal = BigDecimal.ZERO;

        for (OrderItemCreateRequest itemRequest
                : request.getItems()) {

            Product product =
                findActiveProduct(
                    itemRequest.getProductId()
                );

            BigDecimal unitPrice =
                product.getPrice();

            BigDecimal lineAmount =
                calculateLineAmount(
                    unitPrice,
                    itemRequest.getQuantity()
                );

            OrderItem orderItem = new OrderItem();

            orderItem.setProduct(product);
            orderItem.setQuantity(
                itemRequest.getQuantity()
            );
            orderItem.setUnitPrice(unitPrice);
            orderItem.setLineAmount(lineAmount);

            order.addItem(orderItem);

            subtotal = subtotal.add(lineAmount);
        }

        setOrderAmounts(order, subtotal);

        Order savedOrder =
            orderRepository.save(order);

        return OrderResponse.from(savedOrder);
    }

    /**
     * 使用 ID 查詢單筆訂單。
     */
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long orderId) {
        Order order =
            orderRepository.findById(orderId)
                .orElseThrow(() ->
                    new ResourceNotFoundException(
                        "找不到 ID 為 "
                            + orderId
                            + " 的訂單"
                    )
                );

        return OrderResponse.from(order);
    }

    /**
     * 修改草稿訂單基本資料。
     */
    @Transactional
    public OrderResponse updateOrder(
            Long orderId,
            OrderUpdateRequest request
    ) {
        Order order = findDraftOrder(orderId);

        order.setExpectedDeliveryDate(
            request.getExpectedDeliveryDate()
        );

        order.setNote(
            normalizeNullableText(request.getNote())
        );

        Order savedOrder =
            orderRepository.save(order);

        return OrderResponse.from(savedOrder);
    }

    /**
     * 在草稿訂單中新增商品。
     */
    @Transactional
    public OrderResponse addOrderItem(
            Long orderId,
            OrderItemCreateRequest request
    ) {
        Order order = findDraftOrder(orderId);

        boolean productAlreadyExists =
            order.getItems()
                .stream()
                .anyMatch(item ->
                    item.getProduct()
                        .getId()
                        .equals(request.getProductId())
                );

        if (productAlreadyExists) {
            throw new BusinessException(
                ErrorCode.DUPLICATE_RESOURCE,
                "此商品已存在於訂單中"
            );
        }

        Product product =
            findActiveProduct(request.getProductId());

        BigDecimal unitPrice =
            product.getPrice();

        BigDecimal lineAmount =
            calculateLineAmount(
                unitPrice,
                request.getQuantity()
            );

        OrderItem orderItem = new OrderItem();

        orderItem.setProduct(product);
        orderItem.setQuantity(request.getQuantity());
        orderItem.setUnitPrice(unitPrice);
        orderItem.setLineAmount(lineAmount);

        order.addItem(orderItem);

        recalculateOrderAmounts(order);

        Order savedOrder =
            orderRepository.save(order);

        return OrderResponse.from(savedOrder);
    }

    /**
     * 修改草稿訂單中的商品數量。
     */
    @Transactional
    public OrderResponse updateOrderItem(
            Long orderId,
            Long itemId,
            OrderItemUpdateRequest request
    ) {
        Order order = findDraftOrder(orderId);

        OrderItem orderItem =
            findOrderItem(order, itemId);

        orderItem.setQuantity(request.getQuantity());

        orderItem.setLineAmount(
            calculateLineAmount(
                orderItem.getUnitPrice(),
                request.getQuantity()
            )
        );

        recalculateOrderAmounts(order);

        Order savedOrder =
            orderRepository.save(order);

        return OrderResponse.from(savedOrder);
    }

    /**
     * 刪除草稿訂單中的商品。
     */
    @Transactional
    public OrderResponse removeOrderItem(
            Long orderId,
            Long itemId
    ) {
        Order order = findDraftOrder(orderId);

        if (order.getItems().size() <= 1) {
            throw new BusinessException(
                ErrorCode.DATA_CONFLICT,
                "訂單至少需要保留一項商品"
            );
        }

        OrderItem orderItem =
            findOrderItem(order, itemId);

        order.removeItem(orderItem);

        recalculateOrderAmounts(order);

        Order savedOrder =
            orderRepository.save(order);

        return OrderResponse.from(savedOrder);
    }
    
    /**
     * 確認訂單。
     *
     * 確認時會依每一筆訂單明細執行：
     * 1. FEFO 庫存預留
     * 2. 建立 RESERVE 庫存異動
     * 3. 建立訂單批次配置
     * 4. 將訂單狀態更新為 CONFIRMED
     *
     * 任一商品庫存不足時，整筆交易會回滾。
     */
    @Transactional
    public OrderResponse confirmOrder(Long orderId) {
        Order order =
            orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() ->
                    new ResourceNotFoundException(
                        "找不到 ID 為 "
                            + orderId
                            + " 的訂單"
                    )
                );

        validateOrderCanBeConfirmed(order);

        for (OrderItem orderItem : order.getItems()) {
            InventoryReservationRequest reservationRequest =
                createReservationRequest(
                    order,
                    orderItem
                );

            InventoryReservationResponse reservationResponse =
                inventoryService.reserveInventory(
                    reservationRequest
                );

            createOrderItemAllocations(
                orderItem,
                reservationResponse
            );
        }

        order.setStatus(OrderStatus.CONFIRMED);
        order.setConfirmedAt(LocalDateTime.now());

        Order savedOrder =
            orderRepository.save(order);

        return OrderResponse.from(savedOrder);
    }
    
    /**
     * 取消訂單。
     *
     * 可取消狀態：
     * 1. DRAFT：直接取消
     * 2. CONFIRMED：釋放預留庫存後取消
     * 3. PROCESSING：取消 PREPARING 出貨單，
     *    並釋放預留庫存
     *
     * 不可取消狀態：
     * 1. SHIPPED：商品已實際出庫
     * 2. COMPLETED：訂單已完成
     * 3. CANCELLED：訂單已取消
     *
     * 因為使用 @Transactional，
     * 任何一筆庫存釋放失敗時，整個取消流程都會回滾。
     */
    @Transactional
    public OrderResponse cancelOrder(Long orderId) {

        /*
         * 鎖定訂單，避免取消、出貨或其他狀態操作
         * 同時修改同一張訂單。
         */
        Order order = orderRepository
            .findByIdForUpdate(orderId)
            .orElseThrow(() ->
                new ResourceNotFoundException(
                    "找不到 ID 為 "
                        + orderId
                        + " 的訂單"
                )
            );

        OrderStatus currentStatus =
            order.getStatus();

        /*
         * 已出貨後不能使用一般取消流程，
         * 未來若需要處理，應另外建立退貨流程。
         */
        if (currentStatus == OrderStatus.SHIPPED) {
            throw new BusinessException(
                ErrorCode.INVALID_STATUS_TRANSITION,
                "已出貨的訂單不可取消，請使用退貨流程"
            );
        }

        /*
         * 已完成的訂單不可取消。
         */
        if (currentStatus == OrderStatus.COMPLETED) {
            throw new BusinessException(
                ErrorCode.INVALID_STATUS_TRANSITION,
                "已完成的訂單不可取消"
            );
        }

        /*
         * 避免重複取消。
         */
        if (currentStatus == OrderStatus.CANCELLED) {
            throw new BusinessException(
                ErrorCode.INVALID_STATUS_TRANSITION,
                "訂單已經取消"
            );
        }

        /*
         * 只有 DRAFT、CONFIRMED、PROCESSING
         * 可以進入取消流程。
         */
        if (
            currentStatus != OrderStatus.DRAFT
                && currentStatus != OrderStatus.CONFIRMED
                && currentStatus != OrderStatus.PROCESSING
        ) {
            throw new BusinessException(
                ErrorCode.INVALID_STATUS_TRANSITION,
                "目前訂單狀態不可取消："
                    + currentStatus
            );
        }

        LocalDateTime cancelledAt =
            LocalDateTime.now();

        /*
         * PROCESSING 代表已建立 PREPARING 出貨單，
         * 因此必須先將出貨單取消。
         */
        if (currentStatus == OrderStatus.PROCESSING) {
            cancelPreparingShipment(
                orderId
            );
        }

        /*
         * CONFIRMED 與 PROCESSING 都已經預留庫存，
         * 因此取消時必須釋放所有 ACTIVE Allocation。
         *
         * DRAFT 尚未預留庫存，不需要執行。
         */
        if (
            currentStatus == OrderStatus.CONFIRMED
                || currentStatus == OrderStatus.PROCESSING
        ) {
            releaseActiveAllocations(
                orderId,
                cancelledAt
            );
        }

        /*
         * 最後更新訂單狀態與取消時間。
         */
        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelledAt(cancelledAt);

        Order savedOrder =
            orderRepository.save(order);

        return OrderResponse.from(savedOrder);
    }
    
    /**
     * 驗證同一張訂單中沒有重複商品。
     *
     * 資料庫本身也有 order_id + product_id
     * 的唯一限制，這裡先提供較清楚的錯誤訊息。
     */
    private void validateDuplicateProducts(
            OrderCreateRequest request
    ) {
        Set<Long> productIds = new HashSet<>();

        for (OrderItemCreateRequest item
                : request.getItems()) {

            boolean added =
                productIds.add(item.getProductId());

            if (!added) {
                throw new BusinessException(
                    ErrorCode.INVALID_INPUT,
                    "同一張訂單不可重複加入商品 ID："
                        + item.getProductId()
                );
            }
        }
    }

    /**
     * 計算單筆訂單明細金額。
     */
    private BigDecimal calculateLineAmount(
            BigDecimal unitPrice,
            Integer quantity
    ) {
        return unitPrice
            .multiply(BigDecimal.valueOf(quantity))
            .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 設定訂單小計、稅額與總額。
     */
    private void setOrderAmounts(
            Order order,
            BigDecimal subtotal
    ) {
        BigDecimal normalizedSubtotal =
            subtotal.setScale(
                2,
                RoundingMode.HALF_UP
            );

        BigDecimal taxAmount =
            normalizedSubtotal
                .multiply(TAX_RATE)
                .setScale(
                    2,
                    RoundingMode.HALF_UP
                );

        BigDecimal totalAmount =
            normalizedSubtotal
                .add(taxAmount)
                .setScale(
                    2,
                    RoundingMode.HALF_UP
                );

        order.setSubtotal(normalizedSubtotal);
        order.setTaxAmount(taxAmount);
        order.setTotalAmount(totalAmount);
    }

    /**
     * 查詢並驗證啟用中的客戶。
     */
    private Customer findActiveCustomer(
            Long customerId
    ) {
        Customer customer =
            customerRepository.findById(customerId)
                .orElseThrow(() ->
                    new ResourceNotFoundException(
                        "找不到 ID 為 "
                            + customerId
                            + " 的客戶"
                    )
                );

        if (!Boolean.TRUE.equals(customer.getActive())) {
            throw new BusinessException(
                ErrorCode.DATA_CONFLICT,
                "停用中的客戶不可建立訂單"
            );
        }

        return customer;
    }

    /**
     * 查詢並驗證啟用中的商品。
     */
    private Product findActiveProduct(
            Long productId
    ) {
        Product product =
            productRepository.findById(productId)
                .orElseThrow(() ->
                    new ResourceNotFoundException(
                        "找不到 ID 為 "
                            + productId
                            + " 的商品"
                    )
                );

        if (!Boolean.TRUE.equals(product.getActive())) {
            throw new BusinessException(
                ErrorCode.DATA_CONFLICT,
                "停用中的商品不可加入訂單："
                    + product.getName()
            );
        }

        return product;
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
                "停用中的使用者不可建立訂單"
            );
        }

        return user;
    }

    /**
     * 產生訂單編號。
     *
     * 格式：
     * ORD-20260819-A1B2C3D4
     */
    private String generateOrderNumber() {
        String datePart =
            LocalDateTime.now()
                .format(ORDER_NUMBER_DATE_FORMAT);

        String randomPart =
            UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 8)
                .toUpperCase();

        return "ORD-"
            + datePart
            + "-"
            + randomPart;
    }

    /**
     * 清除選填文字前後空白。
     */
    private String normalizeNullableText(
            String value
    ) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();

        return normalized.isEmpty()
            ? null
            : normalized;
    }
    
    /**
     * 查詢訂單並確認訂單仍為草稿狀態。
     */
    private Order findDraftOrder(Long orderId) {
        Order order =
            orderRepository.findById(orderId)
                .orElseThrow(() ->
                    new ResourceNotFoundException(
                        "找不到 ID 為 "
                            + orderId
                            + " 的訂單"
                    )
                );

        if (order.getStatus() != OrderStatus.DRAFT) {
            throw new BusinessException(
                ErrorCode.INVALID_STATUS_TRANSITION,
                "只有草稿狀態的訂單可以修改"
            );
        }

        return order;
    }

    /**
     * 從指定訂單中查詢明細。
     *
     * 同時避免使用其他訂單的 itemId。
     */
    private OrderItem findOrderItem(
            Order order,
            Long itemId
    ) {
        return order.getItems()
            .stream()
            .filter(item ->
                item.getId().equals(itemId)
            )
            .findFirst()
            .orElseThrow(() ->
                new ResourceNotFoundException(
                    "此訂單中找不到 ID 為 "
                        + itemId
                        + " 的訂單明細"
                )
            );
    }

    /**
     * 根據目前所有訂單明細重新計算訂單金額。
     */
    private void recalculateOrderAmounts(
            Order order
    ) {
        BigDecimal subtotal =
            order.getItems()
                .stream()
                .map(OrderItem::getLineAmount)
                .reduce(
                    BigDecimal.ZERO,
                    BigDecimal::add
                );

        setOrderAmounts(order, subtotal);
    }
    
    /**
     * 驗證訂單是否可以確認。
     */
    private void validateOrderCanBeConfirmed(
            Order order
    ) {
        if (order.getStatus() != OrderStatus.DRAFT) {
            throw new BusinessException(
                ErrorCode.INVALID_STATUS_TRANSITION,
                "只有草稿狀態的訂單可以確認"
            );
        }

        if (order.getItems().isEmpty()) {
            throw new BusinessException(
                ErrorCode.DATA_CONFLICT,
                "沒有商品明細的訂單不可確認"
            );
        }

        boolean hasExistingAllocations =
            order.getItems()
                .stream()
                .anyMatch(item ->
                    !item.getAllocations().isEmpty()
                );

        if (hasExistingAllocations) {
            throw new BusinessException(
                ErrorCode.DATA_CONFLICT,
                "訂單已存在庫存配置，不可重複確認"
            );
        }
    }
    
    /**
     * 將訂單明細轉成庫存預留 Request。
     */
    private InventoryReservationRequest
            createReservationRequest(
                Order order,
                OrderItem orderItem
    ) {
        InventoryReservationRequest request =
            new InventoryReservationRequest();

        request.setProductId(
            orderItem.getProduct().getId()
        );

        request.setQuantity(
            orderItem.getQuantity()
        );

        request.setReferenceType("ORDER_ITEM");

        request.setReferenceId(
            orderItem.getId()
        );

        request.setNote(
            "訂單確認預留："
                + order.getOrderNumber()
        );

        return request;
    }
    
    /**
     * 根據 FEFO 預留結果建立訂單批次配置。
     */
    private void createOrderItemAllocations(
            OrderItem orderItem,
            InventoryReservationResponse reservationResponse
    ) {
        for (InventoryReservationBatchResponse batchResponse
                : reservationResponse.getBatches()) {

            InventoryBatch batch =
                inventoryBatchRepository
                    .findById(batchResponse.getBatchId())
                    .orElseThrow(() ->
                        new ResourceNotFoundException(
                            "找不到 ID 為 "
                                + batchResponse.getBatchId()
                                + " 的庫存批次"
                        )
                    );

            OrderItemAllocation allocation =
                new OrderItemAllocation();

            allocation.setBatch(batch);

            allocation.setAllocatedQuantity(
                batchResponse.getReservedQuantity()
            );

            allocation.setStatus(
                AllocationStatus.ACTIVE
            );

            allocation.setAllocatedAt(
                LocalDateTime.now()
            );

            orderItem.addAllocation(allocation);

            orderItemAllocationRepository.save(
                allocation
            );
        }
    }
    
    /**
     * 取消處理中的訂單所建立的備貨出貨單。
     */
    private void cancelPreparingShipment(Long orderId) {

        Shipment shipment = shipmentRepository
            .findByOrder_Id(orderId)
            .orElseThrow(() ->
                new BusinessException(
                    ErrorCode.DATA_CONFLICT,
                    "PROCESSING 訂單找不到對應的出貨單"
                )
            );

        /*
         * PROCESSING 階段的出貨單應該是 PREPARING。
         *
         * 如果已經是 SHIPPED，代表商品已實際出庫，
         * 不可再使用取消訂單流程。
         */
        if (
            shipment.getStatus()
                != ShipmentStatus.PREPARING
        ) {
            throw new BusinessException(
                ErrorCode.INVALID_STATUS_TRANSITION,
                "只有 PREPARING 狀態的出貨單可以取消"
            );
        }

        shipment.setStatus(
            ShipmentStatus.CANCELLED
        );

        shipmentRepository.save(shipment);
    }
    
    /**
     * 釋放訂單所有仍為 ACTIVE 的庫存配置。
     */
    private void releaseActiveAllocations(
            Long orderId,
            LocalDateTime releasedAt
    ) {
        List<OrderItemAllocation> allocations =
            orderItemAllocationRepository
                .findAllByOrderItem_Order_IdAndStatus(
                    orderId,
                    AllocationStatus.ACTIVE
                );

        /*
         * CONFIRMED 或 PROCESSING 正常情況下
         * 應該至少存在一筆 ACTIVE Allocation。
         */
        if (allocations.isEmpty()) {
            throw new BusinessException(
                ErrorCode.DATA_CONFLICT,
                "訂單沒有可釋放的有效庫存配置"
            );
        }

        for (OrderItemAllocation allocation
                : allocations) {

            InventoryReleaseRequest releaseRequest =
                new InventoryReleaseRequest();

            /*
             * 指定訂單確認時透過 FEFO
             * 所預留的庫存批次。
             */
            releaseRequest.setBatchId(
                allocation.getBatch().getId()
            );

            /*
             * 釋放此 Allocation 的全部預留數量。
             */
            releaseRequest.setQuantity(
                allocation.getAllocatedQuantity()
            );

            /*
             * 必須與預留時的 referenceType 相同。
             */
            releaseRequest.setReferenceType(
                "ORDER_ITEM"
            );

            /*
             * 必須與預留時的 referenceId 相同。
             */
            releaseRequest.setReferenceId(
                allocation.getOrderItem().getId()
            );

            releaseRequest.setNote(
                "取消訂單："
                    + allocation
                        .getOrderItem()
                        .getOrder()
                        .getOrderNumber()
            );

            /*
             * 釋放預留庫存：
             * quantityReserved 減少，
             * quantityOnHand 不變，
             * 並新增 RELEASE 異動紀錄。
             */
            inventoryService.releaseInventory(
                releaseRequest
            );

            /*
             * 庫存釋放成功後更新 Allocation。
             */
            allocation.setStatus(
                AllocationStatus.RELEASED
            );
            allocation.setReleasedAt(releasedAt);
        }

        orderItemAllocationRepository.saveAll(
            allocations
        );
    }
}