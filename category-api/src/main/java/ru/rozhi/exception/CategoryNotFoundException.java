package ru.rozhi.exception;

public class CategoryNotFoundException extends ResourceNotFoundException {

    public CategoryNotFoundException(String categoryId) {
        super("CATEGORY_NOT_FOUND", "Category not found: " + categoryId);
    }
}