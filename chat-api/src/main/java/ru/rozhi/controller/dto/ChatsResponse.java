package ru.rozhi.controller.dto;

import java.util.List;

public record ChatsResponse(
        List<ChatResponse> chats
) {}
