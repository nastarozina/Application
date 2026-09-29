package ru.rozhi.controller.dto;

import java.util.List;

public record BannerResponse(
        String id,
        String name,
        String description,
        String categoryName,
        List<ImageDto> images
) {}
