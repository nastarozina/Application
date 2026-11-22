package ru.rozhi.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import ru.rozhi.controller.dto.MessageRequest;
import ru.rozhi.controller.dto.MessagesResponse;
import ru.rozhi.service.MessageService;

import java.time.Instant;

@RestController
@RequestMapping("/message")
public class MessageController {

    @Autowired
    private MessageService messageService;

    @GetMapping("/{userId}")
    public MessagesResponse getMessages(
            @RequestHeader("X-User-Id") String initiatorUserId,
            @PathVariable("userId") String targetUserId,
            @RequestParam(required = false) String lastMessageId,
            @RequestParam(defaultValue = "50") int size
    ) {
        if (lastMessageId == null) {
            return messageService.getLatestMessages(initiatorUserId, targetUserId, size);
        } else {
            return messageService.getMessagesBefore(
                    initiatorUserId, targetUserId, lastMessageId, size);
        }
    }

    @GetMapping("/{userId}/new")
    public MessagesResponse getMessagesSince(
            @RequestHeader("X-User-Id") String initiatorUserId,
            @PathVariable("userId") String targetUserId,
            @RequestParam Instant since
    ) {
        return messageService.getMessagesSince(initiatorUserId, targetUserId, since);
    }

    @PostMapping("/{userId}")
    public void sendMessage(
            @RequestHeader("X-User-Id") String initiatorUserId,
            @PathVariable("userId") String targetUserId,
            @RequestBody MessageRequest request
    ) {
        messageService.sendMessage(initiatorUserId, targetUserId, request);
    }
}