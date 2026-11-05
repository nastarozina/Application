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

    @Aggregation(pipeline = {
            "{ $match: { $or: [ { 'userLow': ?0 }, { 'userHigh': ?0 } ] } }",
            "{ $sort: { 'lastModifiedAt': -1, '_id': -1 } }",
            "{ $limit: ?1 }"
    })
    List<Chat> findLatestChatsByUser(String userId, int limit);


    @Aggregation(pipeline = {
            "{ $match: { $or: [ { 'userLow': ?0 }, { 'userHigh': ?0 } ], 'lastModifiedAt': { $lt: ?1 } } }",
            "{ $sort: { 'lastModifiedAt': -1, '_id': -1 } }",
            "{ $limit: ?2 }"
    })
    List<Chat> findChatsByUserUntil(String userId, Instant until, int limit);


    @Aggregation(pipeline = {
            "{ $match: { $or: [ { 'userLow': ?0 }, { 'userHigh': ?0 } ], 'lastModifiedAt': { $gt: ?1 } } }",
            "{ $sort: { 'lastModifiedAt': -1, '_id': -1 } }"
    })
    List<Chat> findChatsUpdatedSince(String userId, Instant since);
}