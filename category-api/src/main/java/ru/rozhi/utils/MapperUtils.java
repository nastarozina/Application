package ru.rozhi.utils;

import ru.rozhi.controller.dto.CategoryResponse;
import ru.rozhi.repository.model.Category;

public class MapperUtils {
    public static CategoryResponse getCategoryResponse(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getParentId()
        );
    }
}