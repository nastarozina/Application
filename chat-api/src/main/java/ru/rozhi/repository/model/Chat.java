package ru.rozhi.repository.model;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "chats")
@CompoundIndex(
        name = "uk_chat_participants",
        def = "{'userLow': 1, 'userHigh': 1}",
        unique = true
)
@CompoundIndex(
        name = "idx_chat_user_last_modified",
        def = "{'userLow': 1, 'lastModifiedAt': -1}"
)
@CompoundIndex(
        name = "idx_chat_user_last_modified_2",
        def = "{'userHigh': 1, 'lastModifiedAt': -1}"
)
@Data
@Builder
public class Chat {

    @Id
    private String id;

    private String userLow;
    private String userHigh;

    private String lastMessagePreview;
    private String lastMessageSenderId;
    private boolean lastMessageUnread;
    private Instant lastMessageAt;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant lastModifiedAt;
}
