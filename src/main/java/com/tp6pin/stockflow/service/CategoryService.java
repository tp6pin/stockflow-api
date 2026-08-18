package com.tp6pin.stockflow.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tp6pin.stockflow.dto.request.CategoryCreateRequest;
import com.tp6pin.stockflow.dto.request.CategoryUpdateRequest;
import com.tp6pin.stockflow.dto.response.CategoryResponse;
import com.tp6pin.stockflow.dto.response.PageResponse;
import com.tp6pin.stockflow.entity.Category;
import com.tp6pin.stockflow.exception.BusinessException;
import com.tp6pin.stockflow.exception.ErrorCode;
import com.tp6pin.stockflow.exception.ResourceNotFoundException;
import com.tp6pin.stockflow.repository.CategoryRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    /**
     * 建立商品分類。
     */
    @Transactional
    public CategoryResponse create(CategoryCreateRequest request) {
        String normalizedName = request.getName().trim();

        if (categoryRepository.existsByNameIgnoreCase(normalizedName)) {
            throw new BusinessException(
                ErrorCode.DUPLICATE_RESOURCE,
                "分類名稱已存在"
            );
        }

        Category category = new Category();
        category.setName(normalizedName);
        category.setDescription(
            normalizeNullableText(request.getDescription())
        );
        category.setActive(true);

        Category savedCategory = categoryRepository.save(category);

        return CategoryResponse.from(savedCategory);
    }

    /**
     * 使用 ID 查詢單一分類。
     */
    @Transactional(readOnly = true)
    public CategoryResponse getById(Long id) {
        Category category = findCategoryById(id);

        return CategoryResponse.from(category);
    }

    /**
     * 分頁查詢分類，keyword 為空時查詢全部。
     */
    @Transactional(readOnly = true)
    public PageResponse<CategoryResponse> search(
            String keyword,
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

        Page<Category> categoryPage;

        if (keyword == null || keyword.isBlank()) {
            categoryPage = categoryRepository.findAll(pageable);
        } else {
            categoryPage =
                categoryRepository.findByNameContainingIgnoreCase(
                    keyword.trim(),
                    pageable
                );
        }

        Page<CategoryResponse> responsePage =
            categoryPage.map(CategoryResponse::from);

        return PageResponse.from(responsePage);
    }

    /**
     * 查詢目前啟用中的分類，提供商品表單下拉選單使用。
     */
    @Transactional(readOnly = true)
    public List<CategoryResponse> getActiveCategories() {
        return categoryRepository
            .findAllByActiveTrueOrderByNameAsc()
            .stream()
            .map(CategoryResponse::from)
            .toList();
    }

    /**
     * 更新分類。
     */
    @Transactional
    public CategoryResponse update(
            Long id,
            CategoryUpdateRequest request
    ) {
        Category category = findCategoryById(id);
        String normalizedName = request.getName().trim();

        categoryRepository
            .findByNameIgnoreCase(normalizedName)
            .filter(existingCategory ->
                !existingCategory.getId().equals(id)
            )
            .ifPresent(existingCategory -> {
                throw new BusinessException(
                    ErrorCode.DUPLICATE_RESOURCE,
                    "分類名稱已存在"
                );
            });

        category.setName(normalizedName);
        category.setDescription(
            normalizeNullableText(request.getDescription())
        );
        category.setActive(request.getActive());

        return CategoryResponse.from(category);
    }

    /**
     * 停用分類，不直接刪除資料。
     */
    @Transactional
    public CategoryResponse deactivate(Long id) {
        Category category = findCategoryById(id);

        if (!category.getActive()) {
            return CategoryResponse.from(category);
        }

        category.setActive(false);

        return CategoryResponse.from(category);
    }

    /**
     * 啟用分類。
     */
    @Transactional
    public CategoryResponse activate(Long id) {
        Category category = findCategoryById(id);

        if (category.getActive()) {
            return CategoryResponse.from(category);
        }

        category.setActive(true);

        return CategoryResponse.from(category);
    }

    private Category findCategoryById(Long id) {
        return categoryRepository.findById(id)
            .orElseThrow(() ->
                new ResourceNotFoundException(
                    "找不到 ID 為 " + id + " 的商品分類"
                )
            );
    }

    private String normalizeNullableText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }
}