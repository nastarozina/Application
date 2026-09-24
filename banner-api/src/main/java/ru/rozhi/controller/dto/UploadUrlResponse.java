package ru.rozhi.controller.dto;

public record UploadUrlResponse(
        String imageId,
        String objectKey,
        String uploadUrl
) {}