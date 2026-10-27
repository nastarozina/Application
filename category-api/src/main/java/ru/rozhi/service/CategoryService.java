package ru.rozhi.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import ru.rozhi.controller.dto.CategoryRequest;
import ru.rozhi.controller.dto.CategoryResponse;
import ru.rozhi.exception.CategoryNotFoundException;
import ru.rozhi.repository.CategoryRepository;
import ru.rozhi.repository.model.Category;
import ru.rozhi.utils.MapperUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CategoryService {

    @Autowired
    private CategoryRepository repository;

    public CategoryResponse getCategoryById(String id) {
        Category category = repository.findById(id)
                .orElseThrow(() -> new CategoryNotFoundException(id));

        return MapperUtils.getCategoryResponse(category);
    }

    public List<CategoryResponse> getAllCategories() {
        List<Category> categories = repository.findAll();
        return categories.stream().map(MapperUtils::getCategoryResponse).collect(Collectors.toList());
    }

    public Map<String, CategoryResponse> getCategoryNames(List<String> categoryIds) {
        return repository.findAllById(categoryIds)
                .stream()
                .collect(Collectors.toMap(
                        Category::getId,
                        MapperUtils::getCategoryResponse
                ));
    }

    public CategoryResponse createCategory(CategoryRequest category) {
        Category createdCategory =  repository.save(Category.builder()
                .name(category.name())
                .build());

        return MapperUtils.getCategoryResponse(createdCategory);
    }

    @CacheEvict(value = "categoryChains", allEntries = true)
    public CategoryResponse updateCategory(String id, CategoryRequest categoryToUpdate) {
        Category category = repository.findById(id)
                .orElseThrow(() -> new CategoryNotFoundException(id));

        if (categoryToUpdate.name() != null && !categoryToUpdate.name().isBlank()) {
            category.setName(categoryToUpdate.name());
        }

        category = repository.save(category);
        return MapperUtils.getCategoryResponse(category);
    }

    @CacheEvict(value = "categoryChains", allEntries = true)
    public void deleteCategory(String id) {
        boolean isCategoryExist = repository.existsById(id);
        if (!isCategoryExist) {
            throw new CategoryNotFoundException(id);
        }

        repository.deleteById(id);
    }

    @Cacheable(value = "categoryChains")
    public List<CategoryResponse> getCategoryChain(String startCategoryId) {
        List<CategoryResponse> categoryChain = new ArrayList<>();

        CategoryResponse current = getCategoryById(startCategoryId);

        while (current != null) {
            categoryChain.add(current);

            if (current.parentId() == null) {
                break;
            }
            current = getCategoryById(current.parentId());
        }

        Collections.reverse(categoryChain);
        return categoryChain;
    }
}
