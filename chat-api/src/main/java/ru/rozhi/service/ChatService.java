package ru.rozhi.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import ru.rozhi.controller.dto.ChatResponse;
import ru.rozhi.controller.dto.ChatsResponse;
import ru.rozhi.exception.ChatNotFoundException;
import ru.rozhi.repository.ChatRepository;
import ru.rozhi.repository.model.Chat;
import ru.rozhi.utils.Utils;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static ru.rozhi.utils.MapperUtils.toChat;
import static ru.rozhi.utils.MapperUtils.toChatResponse;
import static ru.rozhi.utils.Utils.getChatUsers;

@Service
public class ChatService {

    @Autowired
    private ChatRepository chatRepository;

    public ChatsResponse getChats(String initiatorUserId, Instant until, int size) {
        List<Chat> chats;

        if (until == null) {
            // Первая загрузка
            chats = chatRepository.findLatestChatsByUser(initiatorUserId, size);
        } else {
            // Подгрузка
            chats = chatRepository.findChatsByUserUntil(initiatorUserId, until, size);
        }

        return new ChatsResponse(
                chats.stream()
                        .map(chat -> toChatResponse(chat, initiatorUserId))
                        .toList()
        );
    }

    public ChatsResponse getChatsUpdatedSince(String initiatorUserId, Instant since) {
        List<Chat> chats = chatRepository.findChatsUpdatedSince(initiatorUserId, since);

        return new ChatsResponse(
                chats.stream()
                        .map(chat -> toChatResponse(chat, initiatorUserId))
                        .toList()
        );
    }

    public ChatResponse getChat(String initiatorUserId, String targetUserId) {
        Chat chat = findChat(initiatorUserId, targetUserId)
                .orElseThrow(() -> new ChatNotFoundException(initiatorUserId, targetUserId));

        return toChatResponse(chat, initiatorUserId);
    }

    public Chat getOrCreateChat(String initiatorUserId, String targetUserId) {
        Optional<Chat> existingChat = findChat(initiatorUserId, targetUserId);

        if (existingChat.isPresent()) {
            return existingChat.get();
        }

        Chat chat = toChat(initiatorUserId, targetUserId);

        try {
            return chatRepository.save(chat);
        } catch (DuplicateKeyException _) {
            return findChat(initiatorUserId, targetUserId).orElseThrow();
        }
    }

    public void markAsRead(String initiatorUserId, String targetUserId) {
        Chat chat = findChat(initiatorUserId, targetUserId).orElseThrow();

        if (chat.isLastMessageUnread() && chat.getLastMessageSenderId().equals(targetUserId)) {
            chat.setLastMessageUnread(false);
            chatRepository.save(chat);
        }
    }

    public void updateLastMessageInfo(Chat chat, String senderId, String preview, Instant messageDate) {
        chat.setLastMessagePreview(preview);
        chat.setLastMessageSenderId(senderId);
        chat.setLastMessageAt(messageDate);
        chat.setLastMessageUnread(true);

        chatRepository.save(chat);
    }

    public Optional<Chat> findChat(String initiatorUserId, String targetUserId) {
        Utils.ChatUsers users = getChatUsers(initiatorUserId, targetUserId);
        return chatRepository.findByUserLowAndUserHigh(users.userLow(), users.userHigh());
    }
}