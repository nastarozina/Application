package ru.rozhi.repository;

import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import ru.rozhi.repository.model.Chat;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface ChatRepository extends MongoRepository<Chat, String> {

    Optional<Chat> findByUserLowAndUserHigh(String userLow, String userHigh);

    /**
     * Первая страница: самые свежие чаты пользователя.
     * ?0 = userId, ?1 = limit (size)
     */
    @Aggregation(pipeline = {
            "{ $match: { $or: [ { 'userLow': ?0 }, { 'userHigh': ?0 } ] } }",
            "{ $sort: { 'lastModifiedAt': -1, '_id': -1 } }",
            "{ $limit: ?1 }"
    })
    List<Chat> findLatestChatsByUser(String userId, int limit);

    /**
     * Следующие страницы: чаты, обновленные строго после указанного момента (after).
     * ?0 = userId, ?1 = after (Instant), ?2 = limit (size)
     */
    @Aggregation(pipeline = {
            "{ $match: { $or: [ { 'userLow': ?0 }, { 'userHigh': ?0 } ], 'lastModifiedAt': { $lt: ?1 } } }",
            "{ $sort: { 'lastModifiedAt': -1, '_id': -1 } }",
            "{ $limit: ?2 }"
    })
    List<Chat> findChatsByUserSince(String userId, Instant after, int limit);

    /**
     * Пуллинг: чаты, обновленные после указанного момента.
     * ?0 = userId, ?1 = since (Instant)
     */
    @Aggregation(pipeline = {
            "{ $match: { $or: [ { 'userLow': ?0 }, { 'userHigh': ?0 } ], 'lastModifiedAt': { $gt: ?1 } } }",
            "{ $sort: { 'lastModifiedAt': -1, '_id': -1 } }"
    })
    List<Chat> findChatsUpdatedSince(String userId, Instant since);
}