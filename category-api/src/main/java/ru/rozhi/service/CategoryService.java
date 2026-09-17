package ru.rozhi.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.rozhi.controller.dto.CategoryResponse;
import ru.rozhi.repository.CategoryRepository;
import ru.rozhi.repository.model.Category;
import ru.rozhi.utils.MapperUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CategoryService {

    @Autowired
    private CategoryRepository repository;

    public CategoryResponse getCategoryById(String id) {
        Category banner = repository.findById(id).orElse(null);
        if (banner == null) {
            return null;
        }

        return MapperUtils.getCategoryResponse(banner);
    }

    public List<CategoryResponse> getAllCategories() {
        List<Category> banners = repository.findAll();
        return banners.stream().map(MapperUtils::getCategoryResponse).collect(Collectors.toList());
    }
}
