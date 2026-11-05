package ru.rozhi.controller.dto;

import java.time.Instant;

public record ChatResponse(
        String id,
        String userId,
        String lastMessagePreview,
        String lastMessageSenderId,
        Instant lastMessageAt,
        boolean unread
) {}
