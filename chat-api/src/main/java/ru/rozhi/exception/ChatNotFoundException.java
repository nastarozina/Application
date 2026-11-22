package ru.rozhi.exception;

public class ChatNotFoundException extends ResourceNotFoundException {

    public ChatNotFoundException(String initiatorUserId, String targetUserId) {
        super("CHAT_NOT_FOUND", String.format("Chat not found for users: {} and {}", initiatorUserId, targetUserId));
    }
}
