package ru.rozhi.controller;

import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import ru.rozhi.controller.dto.CategoryRequest;
import ru.rozhi.controller.dto.CategoryResponse;
import ru.rozhi.service.CategoryService;

import java.net.URI;
import java.util.List;

@RestController
public class CategoryController {

    private static final String CATEGORY_PATH = "/{id}";

    @Autowired
    private CategoryService categoryService;

    @GetMapping(CATEGORY_PATH)
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

    @PostMapping
    public ResponseEntity<@NonNull CategoryResponse> createCategory(@RequestBody CategoryRequest category) {
        CategoryResponse createdCategory = categoryService.createCategory(category);
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path(CATEGORY_PATH)
                .buildAndExpand(createdCategory.id())
                .toUri();

        return ResponseEntity.created(location).body(createdCategory);
    }
}
