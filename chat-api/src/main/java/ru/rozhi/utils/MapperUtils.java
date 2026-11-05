package ru.rozhi.utils;

import ru.rozhi.controller.dto.ChatResponse;
import ru.rozhi.controller.dto.MessageRequest;
import ru.rozhi.controller.dto.MessageResponse;
import ru.rozhi.repository.model.Chat;
import ru.rozhi.repository.model.Message;
import ru.rozhi.utils.Utils.ChatUsers;

import static ru.rozhi.utils.Utils.getChatUsers;

public class MapperUtils {

    public static Chat toChat(String initiatorUserId, String targetUserId) {
        ChatUsers users = getChatUsers(initiatorUserId, targetUserId);

        return Chat.builder()
                .userLow(users.userLow())
                .userHigh(users.userHigh())
                .build();
    }

    public static Message toMessage(String senderId, Chat chat, MessageRequest request) {
        String text = request.text().trim();

        if (text.isEmpty()) {
            throw new IllegalArgumentException("text is empty");
        }

        return Message.builder()
                .text(text)
                .senderId(senderId)
                .chatId(chat.getId())
                .build();
    }

    public static MessageResponse toMessageResponse(Message message) {
        return new MessageResponse(
                message.getId(),
                message.getChatId(),
                message.getSenderId(),
                message.getText(),
                message.getCreatedAt()
        );
    }

    public static ChatResponse toChatResponse(Chat chat, String initiatorUserId) {
        String targetUserId = chat.getUserLow().equals(initiatorUserId) ? chat.getUserHigh() : chat.getUserLow();
        return new ChatResponse(
                chat.getId(),
                targetUserId,
                chat.getLastMessagePreview(),
                chat.getLastMessageSenderId(),
                chat.getLastMessageAt(),
                chat.getLastMessageSenderId().equals(targetUserId) && chat.isLastMessageUnread()
        );
    }
}
