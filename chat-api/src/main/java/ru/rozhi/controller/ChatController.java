package ru.rozhi.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.rozhi.controller.dto.ChatResponse;
import ru.rozhi.controller.dto.ChatsResponse;
import ru.rozhi.service.ChatService;

import java.time.Instant;

@RestController
public class ChatController {

    @Autowired
    private ChatService chatService;

    @GetMapping
    public ChatsResponse getChats(
            @RequestHeader("X-User-Id") String initiatorUserId,
            @RequestParam(required = false) Instant after,
            @RequestParam(defaultValue = "50") int size
    ) {
        return chatService.getChats(initiatorUserId, after, size);
    }


    @GetMapping("/new")
    public ChatsResponse getChatsUpdatedSince(
            @RequestHeader("X-User-Id") String initiatorUserId,
            @RequestParam Instant since
    ) {
        return chatService.getChatsUpdatedSince(initiatorUserId, since);
    }

    @GetMapping("/{userId}")
    public ChatResponse getChat(
            @RequestHeader("X-User-Id") String initiatorUserId,
            @PathVariable("userId") String targetUserId
    ) {
        return chatService.getChat(initiatorUserId, targetUserId);
    }
}