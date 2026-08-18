package com.tp6pin.stockflow.service;

import java.time.LocalDate;
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
import com.tp6pin.stockflow.dto.response.InventoryBatchResponse;
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
}