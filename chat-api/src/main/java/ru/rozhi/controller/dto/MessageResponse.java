package ru.rozhi.controller.dto;

import java.time.Instant;

public record MessageResponse(
        String id,
        String chatId,
        String senderId,
        String text,
        Instant createdAt
) {}
