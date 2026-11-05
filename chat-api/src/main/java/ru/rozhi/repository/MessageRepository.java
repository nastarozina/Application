package ru.rozhi.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import ru.rozhi.repository.model.Message;

import java.time.Instant;
import java.util.List;

@Repository
public interface MessageRepository extends MongoRepository<Message, String> {

    @Query("{ 'chatId': ?0 }")
    List<Message> findLatestMessages(String chatId, Pageable pageable);


    @Query(value = "{ 'chatId': ?0, '_id': { $lt: ?1 } }", sort = "{ '_id': -1 }")
    List<Message> findMessagesBefore(String chatId, String lastMessageId, Pageable pageable);


    @Query(value = "{ 'chatId': ?0, 'createdAt': { $gt: ?1 } }", sort = "{ 'createdAt': 1, '_id': 1 }")
    List<Message> findMessagesSince(String chatId, Instant since);
}