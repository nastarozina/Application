package ru.rozhi.controller.dto;

import java.util.List;

public record MessagesResponse(
        List<MessageResponse> messages,
        String userId,
        String chatId
) {}
