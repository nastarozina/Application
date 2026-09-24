package ru.rozhi.controller.dto;

public record ImageDto(
        String id,

        String objectKey,

        String contentType,

        Long size,

        Integer position
) {}
