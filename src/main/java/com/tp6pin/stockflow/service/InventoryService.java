package com.tp6pin.stockflow.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tp6pin.stockflow.dto.request.InventoryAdjustmentRequest;
import com.tp6pin.stockflow.dto.request.InventoryInboundRequest;
import com.tp6pin.stockflow.dto.request.InventoryReleaseRequest;
import com.tp6pin.stockflow.dto.request.InventoryReservationRequest;
import com.tp6pin.stockflow.dto.request.InventoryShipmentRequest;
import com.tp6pin.stockflow.dto.response.InventoryBatchResponse;
import com.tp6pin.stockflow.dto.response.InventoryReservationBatchResponse;
import com.tp6pin.stockflow.dto.response.InventoryReservationResponse;
import com.tp6pin.stockflow.dto.response.InventoryTransactionResponse;
import com.tp6pin.stockflow.dto.response.PageResponse;
import com.tp6pin.stockflow.entity.InventoryBatch;
import com.tp6pin.stockflow.entity.InventoryTransaction;
import com.tp6pin.stockflow.entity.Product;
import com.tp6pin.stockflow.entity.Supplier;
import com.tp6pin.stockflow.enums.InventoryTransactionType;
import com.tp6pin.stockflow.exception.BusinessException;
import com.tp6pin.stockflow.exception.ErrorCode;
import com.tp6pin.stockflow.exception.ResourceNotFoundException;
import com.tp6pin.stockflow.repository.InventoryBatchRepository;
import com.tp6pin.stockflow.repository.InventoryTransactionRepository;
import com.tp6pin.stockflow.repository.ProductRepository;
import com.tp6pin.stockflow.repository.SupplierRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private static final String INBOUND_REFERENCE_TYPE =
        "INBOUND";

    private static final String ADJUSTMENT_REFERENCE_TYPE =
        "ADJUSTMENT";

    private final InventoryBatchRepository
        inventoryBatchRepository;

    private final InventoryTransactionRepository
        inventoryTransactionRepository;

    private final ProductRepository productRepository;
    private final SupplierRepository supplierRepository;

    /**
     * 商品入庫。
     */
    @Transactional
    public InventoryBatchResponse inbound(
            InventoryInboundRequest request
    ) {
        validateInboundDates(request);

        Product product =
            findActiveProductById(request.getProductId());

        Supplier supplier =
            findActiveSupplierById(request.getSupplierId());

        String normalizedBatchNumber =
            normalizeRequiredText(request.getBatchNumber());

        String normalizedNote =
            normalizeNullableText(request.getNote());

        Optional<InventoryBatch> existingBatch =
            inventoryBatchRepository
                .findByProductAndBatchNumberForUpdate(
                    product.getId(),
                    normalizedBatchNumber
                );

        InventoryBatch batch;

        if (existingBatch.isPresent()) {
            batch = existingBatch.get();

            validateExistingBatch(
                batch,
                supplier,
                request
            );

            increaseQuantityOnHand(
                batch,
                request.getQuantity()
            );
        } else {
            batch = createNewBatch(
                product,
                supplier,
                normalizedBatchNumber,
                request
            );
        }

        InventoryBatch savedBatch =
            inventoryBatchRepository.save(batch);

        createInboundTransaction(
            savedBatch,
            request.getQuantity(),
            normalizedNote
        );

        return InventoryBatchResponse.from(savedBatch);
    }

    /**
     * 手動調整庫存。
     *
     * quantityChange：
     * 正數代表增加庫存。
     * 負數代表減少庫存。
     */
    @Transactional
    public InventoryBatchResponse adjustInventory(
            InventoryAdjustmentRequest request
    ) {
        validateAdjustmentQuantity(
            request.getQuantityChange()
        );

        InventoryBatch batch =
            inventoryBatchRepository
                .findByIdForUpdate(request.getBatchId())
                .orElseThrow(() ->
                    new ResourceNotFoundException(
                        "找不到 ID 為 "
                            + request.getBatchId()
                            + " 的庫存批次"
                    )
                );

        int quantityOnHandAfter =
            calculateAdjustedQuantity(
                batch.getQuantityOnHand(),
                request.getQuantityChange()
            );

        validateAdjustedQuantity(
            quantityOnHandAfter,
            batch.getQuantityReserved()
        );

        batch.setQuantityOnHand(quantityOnHandAfter);

        InventoryBatch savedBatch =
            inventoryBatchRepository.save(batch);

        createAdjustmentTransaction(
            savedBatch,
            request.getQuantityChange(),
            normalizeRequiredText(request.getReason())
        );

        return InventoryBatchResponse.from(savedBatch);
    }

    /**
     * 使用 FEFO 規則預留商品庫存。
     *
     * FEFO：
     * 優先使用有效期限最早的庫存批次。
     */
    @Transactional
    public InventoryReservationResponse reserveInventory(
            InventoryReservationRequest request
    ) {
        Product product =
            findActiveProductForReservation(
                request.getProductId()
            );

        String referenceType =
            normalizeReferenceType(
                request.getReferenceType()
            );

        String note =
            normalizeNullableText(request.getNote());

        /*
         * 查詢可使用的批次並加上悲觀寫入鎖。
         *
         * Repository 已按照：
         * 1. expirationDate
         * 2. receivedDate
         * 3. id
         * 排序。
         */
        List<InventoryBatch> availableBatches =
            inventoryBatchRepository
                .findAvailableBatchesForUpdate(
                    product.getId(),
                    LocalDate.now()
                );

        validateSufficientAvailableStock(
            availableBatches,
            request.getQuantity()
        );

        List<InventoryReservationBatchResponse>
            reservationBatches = new ArrayList<>();

        int remainingQuantity = request.getQuantity();

        for (InventoryBatch batch : availableBatches) {
            if (remainingQuantity == 0) {
                break;
            }

            int quantityAvailable =
                batch.getQuantityOnHand()
                    - batch.getQuantityReserved();

            int quantityToReserve =
                Math.min(
                    quantityAvailable,
                    remainingQuantity
                );

            int quantityReservedAfter =
                batch.getQuantityReserved()
                    + quantityToReserve;

            batch.setQuantityReserved(
                quantityReservedAfter
            );

            InventoryBatch savedBatch =
                inventoryBatchRepository.save(batch);

            createReserveTransaction(
                savedBatch,
                quantityToReserve,
                referenceType,
                request.getReferenceId(),
                note
            );

            reservationBatches.add(
                InventoryReservationBatchResponse.from(
                    savedBatch,
                    quantityToReserve
                )
            );

            remainingQuantity -= quantityToReserve;
        }

        return InventoryReservationResponse.of(
            product,
            request.getQuantity(),
            request.getQuantity() - remainingQuantity,
            referenceType,
            request.getReferenceId(),
            reservationBatches
        );
    }
    
    /**
     * 釋放指定來源在指定批次的預留庫存。
     */
    @Transactional
    public InventoryBatchResponse releaseInventory(
            InventoryReleaseRequest request
    ) {
        String referenceType =
            normalizeReferenceType(
                request.getReferenceType()
            );

        String note =
            normalizeNullableText(request.getNote());

        /*
         * 先鎖定批次，避免兩個釋放請求同時修改
         * quantityReserved。
         */
        InventoryBatch batch =
            inventoryBatchRepository
                .findByIdForUpdate(request.getBatchId())
                .orElseThrow(() ->
                    new ResourceNotFoundException(
                        "找不到 ID 為 "
                            + request.getBatchId()
                            + " 的庫存批次"
                    )
                );

        /*
         * 查詢這個來源在此批次尚未釋放的預留量。
         */
        long sourceReservedQuantity =
            inventoryTransactionRepository
                .sumReservedChangeByReference(
                    batch.getId(),
                    referenceType,
                    request.getReferenceId()
                );

        validateReleaseQuantity(
            batch,
            request.getQuantity(),
            sourceReservedQuantity,
            referenceType,
            request.getReferenceId()
        );

        int quantityReservedAfter =
            batch.getQuantityReserved()
                - request.getQuantity();

        batch.setQuantityReserved(
            quantityReservedAfter
        );

        InventoryBatch savedBatch =
            inventoryBatchRepository.save(batch);

        createReleaseTransaction(
            savedBatch,
            request.getQuantity(),
            referenceType,
            request.getReferenceId(),
            note
        );

        return InventoryBatchResponse.from(savedBatch);
    }
    
    /**
     * 將指定來源已預留的庫存實際出庫。
     *
     * 實際出庫會同時減少：
     * 1. quantityOnHand
     * 2. quantityReserved
     */
    @Transactional
    public InventoryBatchResponse shipInventory(
            InventoryShipmentRequest request
    ) {
        String referenceType =
            normalizeReferenceType(
                request.getReferenceType()
            );

        String note =
            normalizeNullableText(request.getNote());

        /*
         * 鎖定庫存批次，避免多個出庫請求
         * 同時扣除相同批次的庫存。
         */
        InventoryBatch batch =
            inventoryBatchRepository
                .findByIdForUpdate(request.getBatchId())
                .orElseThrow(() ->
                    new ResourceNotFoundException(
                        "找不到 ID 為 "
                            + request.getBatchId()
                            + " 的庫存批次"
                    )
                );

        /*
         * 計算此來源在目前批次尚未釋放、
         * 也尚未實際出庫的預留數量。
         */
        long sourceReservedQuantity =
            inventoryTransactionRepository
                .sumReservedChangeByReference(
                    batch.getId(),
                    referenceType,
                    request.getReferenceId()
                );

        validateShipmentQuantity(
            batch,
            request.getQuantity(),
            sourceReservedQuantity,
            referenceType,
            request.getReferenceId()
        );

        int quantityOnHandAfter =
            batch.getQuantityOnHand()
                - request.getQuantity();

        int quantityReservedAfter =
            batch.getQuantityReserved()
                - request.getQuantity();

        batch.setQuantityOnHand(
            quantityOnHandAfter
        );

        batch.setQuantityReserved(
            quantityReservedAfter
        );

        InventoryBatch savedBatch =
            inventoryBatchRepository.save(batch);

        createShipmentTransaction(
            savedBatch,
            request.getQuantity(),
            referenceType,
            request.getReferenceId(),
            note
        );

        return InventoryBatchResponse.from(savedBatch);
    }
    
    /**
     * 使用 ID 查詢單一庫存批次。
     */
    @Transactional(readOnly = true)
    public InventoryBatchResponse getBatchById(Long id) {
        InventoryBatch batch =
            inventoryBatchRepository.findById(id)
                .orElseThrow(() ->
                    new ResourceNotFoundException(
                        "找不到 ID 為 "
                            + id
                            + " 的庫存批次"
                    )
                );

        return InventoryBatchResponse.from(batch);
    }

    /**
     * 分頁與條件查詢庫存批次。
     */
    @Transactional(readOnly = true)
    public PageResponse<InventoryBatchResponse> searchBatches(
            String keyword,
            Long productId,
            Long supplierId,
            int page,
            int size
    ) {
        Pageable pageable = PageRequest.of(
            page,
            size,
            Sort.by(
                Sort.Order.asc("expirationDate"),
                Sort.Order.asc("id")
            )
        );

        Page<InventoryBatch> batchPage =
            inventoryBatchRepository.search(
                normalizeSearchKeyword(keyword),
                productId,
                supplierId,
                pageable
            );

        Page<InventoryBatchResponse> responsePage =
            batchPage.map(InventoryBatchResponse::from);

        return PageResponse.from(responsePage);
    }

    /**
     * 查詢今天起指定天數內到期的批次。
     */
    @Transactional(readOnly = true)
    public List<InventoryBatchResponse> getExpiringBatches(
            int days
    ) {
        if (days < 0) {
            throw new BusinessException(
                ErrorCode.INVALID_INPUT,
                "查詢天數不可小於 0"
            );
        }

        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusDays(days);

        return inventoryBatchRepository
            .findExpiringBatches(startDate, endDate)
            .stream()
            .map(InventoryBatchResponse::from)
            .toList();
    }

    /**
     * 使用 ID 查詢單一庫存異動紀錄。
     */
    @Transactional(readOnly = true)
    public InventoryTransactionResponse getTransactionById(
            Long id
    ) {
        InventoryTransaction transaction =
            inventoryTransactionRepository.findById(id)
                .orElseThrow(() ->
                    new ResourceNotFoundException(
                        "找不到 ID 為 "
                            + id
                            + " 的庫存異動紀錄"
                    )
                );

        return InventoryTransactionResponse.from(transaction);
    }

    /**
     * 分頁與條件查詢庫存異動紀錄。
     */
    @Transactional(readOnly = true)
    public PageResponse<InventoryTransactionResponse>
            searchTransactions(
                Long productId,
                Long batchId,
                String transactionType,
                int page,
                int size
            ) {
        InventoryTransactionType parsedType =
            parseTransactionType(transactionType);

        Pageable pageable = PageRequest.of(
            page,
            size,
            Sort.by(
                Sort.Order.desc("createdAt"),
                Sort.Order.desc("id")
            )
        );

        Page<InventoryTransaction> transactionPage =
            inventoryTransactionRepository.search(
                productId,
                batchId,
                parsedType,
                pageable
            );

        Page<InventoryTransactionResponse> responsePage =
            transactionPage.map(
                InventoryTransactionResponse::from
            );

        return PageResponse.from(responsePage);
    }

    /**
     * 建立新的庫存批次。
     */
    private InventoryBatch createNewBatch(
            Product product,
            Supplier supplier,
            String batchNumber,
            InventoryInboundRequest request
    ) {
        InventoryBatch batch = new InventoryBatch();

        batch.setProduct(product);
        batch.setSupplier(supplier);
        batch.setBatchNumber(batchNumber);
        batch.setQuantityOnHand(request.getQuantity());
        batch.setQuantityReserved(0);
        batch.setReceivedDate(request.getReceivedDate());
        batch.setManufactureDate(
            request.getManufactureDate()
        );
        batch.setExpirationDate(
            request.getExpirationDate()
        );

        return batch;
    }

    /**
     * 增加既有批次的實際庫存。
     */
    private void increaseQuantityOnHand(
            InventoryBatch batch,
            Integer quantity
    ) {
        try {
            int quantityAfter = Math.addExact(
                batch.getQuantityOnHand(),
                quantity
            );

            batch.setQuantityOnHand(quantityAfter);
        } catch (ArithmeticException exception) {
            throw new BusinessException(
                ErrorCode.DATA_CONFLICT,
                "入庫後的庫存數量超過系統可儲存範圍"
            );
        }
    }

    /**
     * 驗證庫存調整數量。
     */
    private void validateAdjustmentQuantity(
            Integer quantityChange
    ) {
        if (quantityChange == 0) {
            throw new BusinessException(
                ErrorCode.INVALID_INPUT,
                "庫存調整數量不可為 0"
            );
        }
    }

    /**
     * 驗證所有可用批次的庫存總數是否足夠。
     */
    private void validateSufficientAvailableStock(
            List<InventoryBatch> availableBatches,
            Integer requestedQuantity
    ) {
        long totalAvailableQuantity =
            availableBatches.stream()
                .mapToLong(batch ->
                    (long) batch.getQuantityOnHand()
                        - batch.getQuantityReserved()
                )
                .sum();

        if (totalAvailableQuantity < requestedQuantity) {
            throw new BusinessException(
                ErrorCode.INSUFFICIENT_STOCK,
                "可用庫存不足，目前可用數量為 "
                    + totalAvailableQuantity
                    + "，要求預留數量為 "
                    + requestedQuantity
            );
        }
    }
    
    /**
     * 計算庫存調整後的實際庫存。
     */
    private int calculateAdjustedQuantity(
            Integer quantityOnHand,
            Integer quantityChange
    ) {
        try {
            return Math.addExact(
                quantityOnHand,
                quantityChange
            );
        } catch (ArithmeticException exception) {
            throw new BusinessException(
                ErrorCode.DATA_CONFLICT,
                "調整後的庫存數量超過系統可儲存範圍"
            );
        }
    }

    /**
     * 驗證庫存調整後的數量。
     */
    private void validateAdjustedQuantity(
            int quantityOnHandAfter,
            Integer quantityReserved
    ) {
        if (quantityOnHandAfter < 0) {
            throw new BusinessException(
                ErrorCode.INSUFFICIENT_STOCK,
                "調整後的實際庫存不可小於 0"
            );
        }

        if (quantityOnHandAfter < quantityReserved) {
            throw new BusinessException(
                ErrorCode.DATA_CONFLICT,
                "調整後的實際庫存不可小於預留庫存"
            );
        }
    }

    /**
     * 建立 INBOUND 庫存異動紀錄。
     */
    private void createInboundTransaction(
            InventoryBatch batch,
            Integer inboundQuantity,
            String note
    ) {
        InventoryTransaction transaction =
            new InventoryTransaction();

        transaction.setProduct(batch.getProduct());
        transaction.setBatch(batch);
        transaction.setTransactionType(
            InventoryTransactionType.INBOUND
        );
        transaction.setOnHandChange(inboundQuantity);
        transaction.setReservedChange(0);
        transaction.setOnHandAfter(
            batch.getQuantityOnHand()
        );
        transaction.setReservedAfter(
            batch.getQuantityReserved()
        );
        transaction.setReferenceType(
            INBOUND_REFERENCE_TYPE
        );
        transaction.setReferenceId(null);
        transaction.setNote(note);

        /*
         * JWT 尚未完成，目前不設定操作人。
         */
        transaction.setCreatedBy(null);

        inventoryTransactionRepository.save(transaction);
    }

    /**
     * 建立 ADJUSTMENT 庫存異動紀錄。
     */
    private void createAdjustmentTransaction(
            InventoryBatch batch,
            Integer quantityChange,
            String reason
    ) {
        InventoryTransaction transaction =
            new InventoryTransaction();

        transaction.setProduct(batch.getProduct());
        transaction.setBatch(batch);
        transaction.setTransactionType(
            InventoryTransactionType.ADJUSTMENT
        );
        transaction.setOnHandChange(quantityChange);
        transaction.setReservedChange(0);
        transaction.setOnHandAfter(
            batch.getQuantityOnHand()
        );
        transaction.setReservedAfter(
            batch.getQuantityReserved()
        );
        transaction.setReferenceType(
            ADJUSTMENT_REFERENCE_TYPE
        );
        transaction.setReferenceId(null);
        transaction.setNote(reason);

        /*
         * JWT 尚未完成，目前不設定操作人。
         */
        transaction.setCreatedBy(null);

        inventoryTransactionRepository.save(transaction);
    }

    /**
     * 建立 RESERVE 庫存異動紀錄。
     */
    private void createReserveTransaction(
            InventoryBatch batch,
            Integer reservedQuantity,
            String referenceType,
            Long referenceId,
            String note
    ) {
        InventoryTransaction transaction =
            new InventoryTransaction();

        transaction.setProduct(batch.getProduct());
        transaction.setBatch(batch);
        transaction.setTransactionType(
            InventoryTransactionType.RESERVE
        );

        /*
         * 預留不會改變實際庫存，
         * 因此 onHandChange 為 0。
         */
        transaction.setOnHandChange(0);

        /*
         * 增加預留庫存，因此為正數。
         */
        transaction.setReservedChange(
            reservedQuantity
        );

        transaction.setOnHandAfter(
            batch.getQuantityOnHand()
        );

        transaction.setReservedAfter(
            batch.getQuantityReserved()
        );

        transaction.setReferenceType(
            referenceType
        );

        transaction.setReferenceId(
            referenceId
        );

        transaction.setNote(note);

        /*
         * JWT 尚未完成，目前不設定操作人。
         */
        transaction.setCreatedBy(null);

        inventoryTransactionRepository.save(transaction);
    }
    
    /**
     * 建立 RELEASE 庫存異動紀錄。
     */
    private void createReleaseTransaction(
            InventoryBatch batch,
            Integer releasedQuantity,
            String referenceType,
            Long referenceId,
            String note
    ) {
        InventoryTransaction transaction =
            new InventoryTransaction();

        transaction.setProduct(batch.getProduct());
        transaction.setBatch(batch);

        transaction.setTransactionType(
            InventoryTransactionType.RELEASE
        );

        /*
         * 釋放預留不影響實際庫存。
         */
        transaction.setOnHandChange(0);

        /*
         * 釋放會減少預留數量，
         * 因此 reservedChange 必須是負數。
         */
        transaction.setReservedChange(
            -releasedQuantity
        );

        transaction.setOnHandAfter(
            batch.getQuantityOnHand()
        );

        transaction.setReservedAfter(
            batch.getQuantityReserved()
        );

        transaction.setReferenceType(
            referenceType
        );

        transaction.setReferenceId(
            referenceId
        );

        transaction.setNote(note);

        /*
         * JWT 尚未完成，目前不設定操作人。
         */
        transaction.setCreatedBy(null);

        inventoryTransactionRepository.save(transaction);
    }
    
    /**
     * 建立 SHIPMENT 庫存異動紀錄。
     */
    private void createShipmentTransaction(
            InventoryBatch batch,
            Integer shipmentQuantity,
            String referenceType,
            Long referenceId,
            String note
    ) {
        InventoryTransaction transaction =
            new InventoryTransaction();

        transaction.setProduct(batch.getProduct());
        transaction.setBatch(batch);

        transaction.setTransactionType(
            InventoryTransactionType.SHIPMENT
        );

        /*
         * 實際出庫會減少帳面庫存。
         */
        transaction.setOnHandChange(
            -shipmentQuantity
        );

        /*
         * 已預留的庫存完成出庫，
         * 因此同時減少預留數量。
         */
        transaction.setReservedChange(
            -shipmentQuantity
        );

        transaction.setOnHandAfter(
            batch.getQuantityOnHand()
        );

        transaction.setReservedAfter(
            batch.getQuantityReserved()
        );

        transaction.setReferenceType(
            referenceType
        );

        transaction.setReferenceId(
            referenceId
        );

        transaction.setNote(note);

        /*
         * JWT 尚未完成，目前不設定操作人。
         */
        transaction.setCreatedBy(null);

        inventoryTransactionRepository.save(transaction);
    }
    
    /**
     * 驗證既有批次資料是否一致。
     */
    private void validateExistingBatch(
            InventoryBatch batch,
            Supplier supplier,
            InventoryInboundRequest request
    ) {
        if (
            !batch.getSupplier()
                .getId()
                .equals(supplier.getId())
        ) {
            throw new BusinessException(
                ErrorCode.DATA_CONFLICT,
                "相同商品及批次編號已屬於其他供應商"
            );
        }

        if (
            !Objects.equals(
                batch.getReceivedDate(),
                request.getReceivedDate()
            )
        ) {
            throw new BusinessException(
                ErrorCode.DATA_CONFLICT,
                "相同批次的收貨日期不一致"
            );
        }

        if (
            !Objects.equals(
                batch.getManufactureDate(),
                request.getManufactureDate()
            )
        ) {
            throw new BusinessException(
                ErrorCode.DATA_CONFLICT,
                "相同批次的製造日期不一致"
            );
        }

        if (
            !Objects.equals(
                batch.getExpirationDate(),
                request.getExpirationDate()
            )
        ) {
            throw new BusinessException(
                ErrorCode.DATA_CONFLICT,
                "相同批次的有效期限不一致"
            );
        }
    }

    /**
     * 驗證入庫日期關係。
     */
    private void validateInboundDates(
            InventoryInboundRequest request
    ) {
        LocalDate today = LocalDate.now();

        LocalDate receivedDate =
            request.getReceivedDate();

        LocalDate manufactureDate =
            request.getManufactureDate();

        LocalDate expirationDate =
            request.getExpirationDate();

        if (expirationDate.isBefore(today)) {
            throw new BusinessException(
                ErrorCode.INVALID_INPUT,
                "有效期限不可早於今天"
            );
        }

        if (receivedDate.isAfter(expirationDate)) {
            throw new BusinessException(
                ErrorCode.INVALID_INPUT,
                "收貨日期不可晚於有效期限"
            );
        }

        if (
            manufactureDate != null
                && manufactureDate.isAfter(expirationDate)
        ) {
            throw new BusinessException(
                ErrorCode.INVALID_INPUT,
                "製造日期不可晚於有效期限"
            );
        }

        if (
            manufactureDate != null
                && manufactureDate.isAfter(receivedDate)
        ) {
            throw new BusinessException(
                ErrorCode.INVALID_INPUT,
                "製造日期不可晚於收貨日期"
            );
        }
    }
    
    /**
     * 驗證指定來源可以釋放的預留數量。
     */
    private void validateReleaseQuantity(
            InventoryBatch batch,
            Integer releaseQuantity,
            long sourceReservedQuantity,
            String referenceType,
            Long referenceId
    ) {
        if (sourceReservedQuantity <= 0) {
            throw new BusinessException(
                ErrorCode.DATA_CONFLICT,
                "指定來源在此批次沒有可釋放的預留庫存："
                    + referenceType
                    + " / "
                    + referenceId
            );
        }

        if (releaseQuantity > sourceReservedQuantity) {
            throw new BusinessException(
                ErrorCode.DATA_CONFLICT,
                "釋放數量不可超過此來源的剩餘預留數量，"
                    + "目前可釋放數量為 "
                    + sourceReservedQuantity
            );
        }

        /*
         * 正常情況下，來源預留量一定不會超過
         * 批次的總預留量。
         *
         * 此檢查可以避免資料異常時，
         * quantityReserved 被扣成負數。
         */
        if (releaseQuantity > batch.getQuantityReserved()) {
            throw new BusinessException(
                ErrorCode.DATA_CONFLICT,
                "釋放數量不可超過批次目前的總預留數量，"
                    + "批次目前預留數量為 "
                    + batch.getQuantityReserved()
            );
        }
    }

    /**
     * 驗證指定來源是否有足夠的預留庫存可供出庫。
     */
    private void validateShipmentQuantity(
            InventoryBatch batch,
            Integer shipmentQuantity,
            long sourceReservedQuantity,
            String referenceType,
            Long referenceId
    ) {
        if (sourceReservedQuantity <= 0) {
            throw new BusinessException(
                ErrorCode.DATA_CONFLICT,
                "指定來源在此批次沒有可出庫的預留庫存："
                    + referenceType
                    + " / "
                    + referenceId
            );
        }

        if (shipmentQuantity > sourceReservedQuantity) {
            throw new BusinessException(
                ErrorCode.DATA_CONFLICT,
                "出庫數量不可超過此來源的剩餘預留數量，"
                    + "目前可出庫數量為 "
                    + sourceReservedQuantity
            );
        }

        if (shipmentQuantity > batch.getQuantityReserved()) {
            throw new BusinessException(
                ErrorCode.DATA_CONFLICT,
                "出庫數量不可超過批次目前的總預留數量，"
                    + "批次目前預留數量為 "
                    + batch.getQuantityReserved()
            );
        }

        if (shipmentQuantity > batch.getQuantityOnHand()) {
            throw new BusinessException(
                ErrorCode.INSUFFICIENT_STOCK,
                "出庫數量不可超過批次目前的實際庫存，"
                    + "批次目前實際庫存為 "
                    + batch.getQuantityOnHand()
            );
        }
    }
    
    /**
     * 查詢並驗證啟用中的商品。
     */
    private Product findActiveProductById(Long productId) {
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
                "指定的商品已停用，無法進行入庫"
            );
        }

        return product;
    }

    /**
     * 查詢並驗證可以進行庫存預留的商品。
     */
    private Product findActiveProductForReservation(
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
                "指定的商品已停用，無法預留庫存"
            );
        }

        return product;
    }
    
    /**
     * 查詢並驗證啟用中的供應商。
     */
    private Supplier findActiveSupplierById(
            Long supplierId
    ) {
        Supplier supplier =
            supplierRepository.findById(supplierId)
                .orElseThrow(() ->
                    new ResourceNotFoundException(
                        "找不到 ID 為 "
                            + supplierId
                            + " 的供應商"
                    )
                );

        if (!Boolean.TRUE.equals(supplier.getActive())) {
            throw new BusinessException(
                ErrorCode.DATA_CONFLICT,
                "指定的供應商已停用，無法進行入庫"
            );
        }

        return supplier;
    }

    /**
     * 將字串轉換成庫存異動類型。
     */
    private InventoryTransactionType parseTransactionType(
            String transactionType
    ) {
        if (
            transactionType == null
                || transactionType.isBlank()
        ) {
            return null;
        }

        try {
            return InventoryTransactionType.valueOf(
                transactionType
                    .trim()
                    .toUpperCase(Locale.ROOT)
            );
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(
                ErrorCode.INVALID_INPUT,
                "庫存異動類型不正確"
            );
        }
    }

    private String normalizeRequiredText(String value) {
        return value.trim();
    }

    private String normalizeNullableText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }

    private String normalizeSearchKeyword(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }
    
    /**
     * 統一參考來源類型格式。
     *
     * 例如：
     * order_item → ORDER_ITEM
     */
    private String normalizeReferenceType(String value) {
        return value
            .trim()
            .toUpperCase(Locale.ROOT);
    }
}