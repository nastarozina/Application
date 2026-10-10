package ru.rozhi.controller.dto;

public record CategoryRequest(
        String name,
        String parentId
) {}
