package ru.rozhi.client;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import ru.rozhi.controller.dto.CategoryResponse;

import java.util.List;
import java.util.Map;

@HttpExchange("${clients.category-api:category-api}")
public interface CategoryClient {
    @GetExchange("/{id}")
    CategoryResponse getCategory(@PathVariable String id);

    @GetExchange("/names")
    Map<String, String> getCategoriesNames(
            @RequestParam("categoriesId") List<String> categoriesId
    );
}
