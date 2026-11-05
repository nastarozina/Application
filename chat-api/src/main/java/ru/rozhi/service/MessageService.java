package ru.rozhi.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import ru.rozhi.controller.dto.MessageRequest;
import ru.rozhi.controller.dto.MessagesResponse;
import ru.rozhi.exception.ChatNotFoundException;
import ru.rozhi.repository.MessageRepository;
import ru.rozhi.repository.model.Chat;
import ru.rozhi.repository.model.Message;
import ru.rozhi.utils.MapperUtils;

import java.time.Instant;
import java.util.List;

@Service
public class MessageService {

    @Autowired
    private ChatService chatService;

    @Autowired
    private MessageRepository messageRepository;

    public MessagesResponse getLatestMessages(String initiatorUserId, String targetUserId, int size) {
        Chat chat = chatService.findChat(initiatorUserId, targetUserId)
                .orElseThrow(() -> new ChatNotFoundException(initiatorUserId, targetUserId));

        Pageable pageable = PageRequest.of(0, size);
        List<Message> messages = messageRepository.findLatestMessages(chat.getId(), pageable);

        // Помечаем чат как прочитанный
        chatService.markAsRead(initiatorUserId, targetUserId);

        return new MessagesResponse(
                messages.stream().map(MapperUtils::toMessageResponse).toList(),
                targetUserId,
                chat.getId()
        );
    }

    public MessagesResponse getMessagesBefore(String initiatorUserId, String targetUserId,
                                              String lastMessageId, int size) {
        Chat chat = chatService.findChat(initiatorUserId, targetUserId)
                .orElseThrow(() -> new ChatNotFoundException(initiatorUserId, targetUserId));

        Pageable pageable = PageRequest.of(0, size);
        List<Message> messages = messageRepository.findMessagesBefore(
                chat.getId(), lastMessageId, pageable);

        return new MessagesResponse(
                messages.stream().map(MapperUtils::toMessageResponse).toList(),
                targetUserId,
                chat.getId()
        );
    }

    public MessagesResponse getMessagesSince(String initiatorUserId, String targetUserId,
                                             Instant since) {
        Chat chat = chatService.findChat(initiatorUserId, targetUserId)
                .orElseThrow(() -> new ChatNotFoundException(initiatorUserId, targetUserId));

        List<Message> messages = messageRepository.findMessagesSince(chat.getId(), since);

        return new MessagesResponse(
                messages.stream().map(MapperUtils::toMessageResponse).toList(),
                targetUserId,
                chat.getId()
        );
    }

    public void sendMessage(String initiatorUserId, String targetUserId, MessageRequest request) {
        Chat chat = chatService.getOrCreateChat(initiatorUserId, targetUserId);

        Message message = MapperUtils.toMessage(initiatorUserId, chat, request);
        Message createdMessage = messageRepository.save(message);

        chatService.updateLastMessageInfo(
                chat,
                initiatorUserId,
                request.text(),
                createdMessage.getCreatedAt()
        );
    }
}