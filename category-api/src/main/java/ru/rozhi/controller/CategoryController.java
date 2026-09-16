package ru.rozhi.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;
import ru.rozhi.service.CategoryService;

@RestController
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

}
