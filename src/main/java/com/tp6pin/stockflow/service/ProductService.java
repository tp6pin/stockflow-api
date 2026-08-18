package com.tp6pin.stockflow.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tp6pin.stockflow.dto.request.ProductCreateRequest;
import com.tp6pin.stockflow.dto.request.ProductUpdateRequest;
import com.tp6pin.stockflow.dto.response.PageResponse;
import com.tp6pin.stockflow.dto.response.ProductResponse;
import com.tp6pin.stockflow.entity.Category;
import com.tp6pin.stockflow.entity.Product;
import com.tp6pin.stockflow.exception.BusinessException;
import com.tp6pin.stockflow.exception.ErrorCode;
import com.tp6pin.stockflow.exception.ResourceNotFoundException;
import com.tp6pin.stockflow.repository.CategoryRepository;
import com.tp6pin.stockflow.repository.ProductRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    /**
     * 建立商品。
     */
    @Transactional
    public ProductResponse create(
            ProductCreateRequest request
    ) {
        String normalizedSku =
            normalizeRequiredText(request.getSku());

        if (
            productRepository.existsBySkuIgnoreCase(
                normalizedSku
            )
        ) {
            throw new BusinessException(
                ErrorCode.DUPLICATE_RESOURCE,
                "商品編號已存在"
            );
        }

        Category category =
            findActiveCategoryById(request.getCategoryId());

        Product product = new Product();
        product.setSku(normalizedSku);
        product.setName(
            normalizeRequiredText(request.getName())
        );
        product.setDescription(
            normalizeNullableText(request.getDescription())
        );
        product.setCategory(category);
        product.setUnit(
            normalizeRequiredText(request.getUnit())
        );
        product.setCost(request.getCost());
        product.setPrice(request.getPrice());
        product.setSafetyStock(request.getSafetyStock());
        product.setActive(true);

        Product savedProduct =
            productRepository.save(product);

        return ProductResponse.from(savedProduct);
    }

    /**
     * 使用 ID 查詢單一商品。
     */
    @Transactional(readOnly = true)
    public ProductResponse getById(Long id) {
        Product product = findProductById(id);

        return ProductResponse.from(product);
    }

    /**
     * 分頁查詢商品。
     *
     * keyword 可搜尋商品名稱或 SKU。
     * categoryId 可限制商品分類。
     */
    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> search(
            String keyword,
            Long categoryId,
            int page,
            int size
    ) {
        Pageable pageable = PageRequest.of(
            page,
            size,
            Sort.by(
                Sort.Order.desc("active"),
                Sort.Order.asc("name")
            )
        );

        String normalizedKeyword =
            normalizeSearchKeyword(keyword);

        Page<Product> productPage =
            productRepository.search(
                normalizedKeyword,
                categoryId,
                pageable
            );

        Page<ProductResponse> responsePage =
            productPage.map(ProductResponse::from);

        return PageResponse.from(responsePage);
    }

    /**
     * 查詢所有啟用中的商品。
     *
     * 可提供前端訂單商品下拉選單使用。
     */
    @Transactional(readOnly = true)
    public List<ProductResponse> getActiveProducts() {
        return productRepository
            .findAllByActiveTrueOrderByNameAsc()
            .stream()
            .map(ProductResponse::from)
            .toList();
    }

    /**
     * 更新商品。
     */
    @Transactional
    public ProductResponse update(
            Long id,
            ProductUpdateRequest request
    ) {
        Product product = findProductById(id);

        String normalizedSku =
            normalizeRequiredText(request.getSku());

        productRepository
            .findBySkuIgnoreCase(normalizedSku)
            .filter(existingProduct ->
                !existingProduct.getId().equals(id)
            )
            .ifPresent(existingProduct -> {
                throw new BusinessException(
                    ErrorCode.DUPLICATE_RESOURCE,
                    "商品編號已存在"
                );
            });

        Category category =
            findCategoryForUpdate(
                product,
                request.getCategoryId()
            );

        product.setSku(normalizedSku);
        product.setName(
            normalizeRequiredText(request.getName())
        );
        product.setDescription(
            normalizeNullableText(request.getDescription())
        );
        product.setCategory(category);
        product.setUnit(
            normalizeRequiredText(request.getUnit())
        );
        product.setCost(request.getCost());
        product.setPrice(request.getPrice());
        product.setSafetyStock(request.getSafetyStock());
        product.setActive(request.getActive());

        return ProductResponse.from(product);
    }

    /**
     * 停用商品。
     *
     * 商品不直接從資料庫刪除，
     * 避免破壞庫存與訂單歷史資料。
     */
    @Transactional
    public ProductResponse deactivate(Long id) {
        Product product = findProductById(id);

        if (!Boolean.TRUE.equals(product.getActive())) {
            return ProductResponse.from(product);
        }

        product.setActive(false);

        return ProductResponse.from(product);
    }

    /**
     * 啟用商品。
     *
     * 商品所屬分類必須處於啟用狀態。
     */
    @Transactional
    public ProductResponse activate(Long id) {
        Product product = findProductById(id);

        if (Boolean.TRUE.equals(product.getActive())) {
            return ProductResponse.from(product);
        }

        if (
            !Boolean.TRUE.equals(
                product.getCategory().getActive()
            )
        ) {
            throw new BusinessException(
                ErrorCode.DATA_CONFLICT,
                "商品所屬分類已停用，無法啟用商品"
            );
        }

        product.setActive(true);

        return ProductResponse.from(product);
    }

    /**
     * 查詢商品 Entity。
     */
    private Product findProductById(Long id) {
        return productRepository.findById(id)
            .orElseThrow(() ->
                new ResourceNotFoundException(
                    "找不到 ID 為 " + id + " 的商品"
                )
            );
    }

    /**
     * 建立商品時，指定的分類必須存在且為啟用狀態。
     */
    private Category findActiveCategoryById(
            Long categoryId
    ) {
        Category category =
            categoryRepository.findById(categoryId)
                .orElseThrow(() ->
                    new ResourceNotFoundException(
                        "找不到 ID 為 "
                            + categoryId
                            + " 的商品分類"
                    )
                );

        if (!Boolean.TRUE.equals(category.getActive())) {
            throw new BusinessException(
                ErrorCode.DATA_CONFLICT,
                "指定的商品分類已停用"
            );
        }

        return category;
    }

    /**
     * 更新商品分類。
     *
     * 如果沒有更換分類，允許商品繼續保留在原分類，
     * 即使該分類目前已被停用。
     *
     * 如果更換分類，新分類必須是啟用狀態。
     */
    private Category findCategoryForUpdate(
            Product product,
            Long categoryId
    ) {
        if (
            product.getCategory()
                .getId()
                .equals(categoryId)
        ) {
            return product.getCategory();
        }

        return findActiveCategoryById(categoryId);
    }

    /**
     * 整理必填文字。
     */
    private String normalizeRequiredText(String value) {
        return value.trim();
    }

    /**
     * 整理選填文字。
     *
     * null 或空白字串統一儲存為 null。
     */
    private String normalizeNullableText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }

    /**
     * 整理搜尋關鍵字。
     *
     * null 或空白代表不限制關鍵字。
     */
    private String normalizeSearchKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }

        return keyword.trim();
    }
}