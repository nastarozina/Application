package ru.rozhi.utils;

import ru.rozhi.controller.dto.CategoryResponse;
import ru.rozhi.repository.model.Category;

public class MapperUtils {
    public static CategoryResponse getCategoryResponse(Category banner) {
        return new CategoryResponse(
                banner.getId(),
                banner.getName()
        );
    }
}