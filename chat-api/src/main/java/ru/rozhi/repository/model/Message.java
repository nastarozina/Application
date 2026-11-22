package ru.rozhi.repository.model;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document("messages")
@CompoundIndex(
        name = "idx_messages_chat_created",
        def = "{'chatId': 1, 'createdAt': -1, '_id': -1}"
)
@Data
@Builder
public class Message {

    @Id
    private String id;

    private String chatId;
    private String senderId;
    private String text;

    @CreatedDate
    private Instant createdAt;
}
