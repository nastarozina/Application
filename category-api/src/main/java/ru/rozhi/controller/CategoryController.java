package ru.rozhi.controller;

import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import ru.rozhi.controller.dto.CategoryResponse;
import ru.rozhi.service.CategoryService;

import java.util.List;

@RestController
public class CategoryController {

    private static final String BANNER_PATH = "/{id}";

    @Autowired
    private CategoryService categoryService;

    @GetMapping(BANNER_PATH)
    public ResponseEntity<@NonNull CategoryResponse> getCategoryById(@PathVariable String id) {
        CategoryResponse banner = categoryService.getCategoryById(id);
        if (banner == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(banner);
    }

    @GetMapping
    public List<CategoryResponse> getAllCategories() {
        return categoryService.getAllCategories();
    }
}
