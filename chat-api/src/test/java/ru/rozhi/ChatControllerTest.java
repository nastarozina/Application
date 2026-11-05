package ru.rozhi;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mongodb.MongoDBContainer;
import org.testcontainers.utility.DockerImageName;
import ru.rozhi.configuration.TestMongoAuditingConfig;
import ru.rozhi.repository.ChatRepository;
import ru.rozhi.repository.MessageRepository;
import ru.rozhi.repository.model.Chat;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.rozhi.configuration.TestMongoAuditingConfig.NOW;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Import(TestMongoAuditingConfig.class)
class ChatControllerTest {

    @Container
    @ServiceConnection
    static MongoDBContainer mongoDBContainer = new MongoDBContainer(
            DockerImageName.parse("mongo:7.0")
    );

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ChatRepository chatRepository;

    @Autowired
    private MessageRepository messageRepository;

    private static final String HEADER = "X-User-Id";

    private static final String ME = "user-2";
    private static final String LOWER = "user-1";
    private static final String HIGHER = "user-3";
    private static final String STRANGER_A = "user-8";
    private static final String STRANGER_B = "user-9";

    @BeforeEach
    void cleanUp() {
        messageRepository.deleteAll();
        chatRepository.deleteAll();
        TestMongoAuditingConfig.clearFixedInstant();
    }

    private Chat saveChat(String userLow, String userHigh, Instant lastModifiedAt,
                          String lastMessagePreview, String lastMessageSenderId,
                          boolean lastMessageUnread) {
        TestMongoAuditingConfig.setFixedInstant(lastModifiedAt);
        Chat chat = chatRepository.save(Chat.builder()
                .userLow(userLow)
                .userHigh(userHigh)
                .lastMessagePreview(lastMessagePreview)
                .lastMessageSenderId(lastMessageSenderId)
                .lastMessageAt(lastModifiedAt)
                .lastMessageUnread(lastMessageUnread)
                .build());
        TestMongoAuditingConfig.clearFixedInstant();
        return chat;
    }

    // ------------------------------------------------------- GET /

    @Nested
    @DisplayName("GET / — первая страница чатов")
    class GetChats {

        @Test
        @DisplayName("пустой список, если чатов нет")
        void shouldReturnEmptyList() throws Exception {
            mockMvc.perform(get("/").header(HEADER, ME))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.chats").isArray())
                    .andExpect(jsonPath("$.chats", hasSize(0)));
        }

        @Test
        @DisplayName("возвращает чаты с обеих сторон (userLow и userHigh)")
        void shouldReturnChatsFromBothSides() throws Exception {
            saveChat(ME, HIGHER, NOW, "Hi", HIGHER, true);
            saveChat(LOWER, ME, NOW.minus(1, ChronoUnit.HOURS), "Hello", LOWER, true);

            mockMvc.perform(get("/").header(HEADER, ME))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.chats", hasSize(2)))
                    .andExpect(jsonPath("$.chats[0].userId").value(HIGHER))
                    .andExpect(jsonPath("$.chats[1].userId").value(LOWER));
        }

        @Test
        @DisplayName("не возвращает чужие чаты")
        void shouldNotReturnForeignChats() throws Exception {
            saveChat(STRANGER_A, STRANGER_B, NOW, "Hi", STRANGER_A, true);

            mockMvc.perform(get("/").header(HEADER, ME))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.chats", hasSize(0)));
        }

        @Test
        @DisplayName("сортировка по lastModifiedAt DESC")
        void shouldSortByLastModifiedAtDesc() throws Exception {
            saveChat(ME, "user-4", NOW.minus(2, ChronoUnit.HOURS), "m1", ME, false);
            saveChat(ME, "user-5", NOW, "m2", ME, false);
            saveChat(ME, "user-6", NOW.minus(5, ChronoUnit.HOURS), "m3", ME, false);

            mockMvc.perform(get("/").header(HEADER, ME))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.chats[*].userId",
                            org.hamcrest.Matchers.contains("user-5", "user-4", "user-6")));
        }

        @Test
        @DisplayName("возвращает превью и признак непрочитанности")
        void shouldReturnPreviewAndUnreadFlag() throws Exception {
            saveChat(ME, HIGHER, NOW, "Hello world", HIGHER, true);

            mockMvc.perform(get("/").header(HEADER, ME))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.chats[0].lastMessagePreview").value("Hello world"))
                    .andExpect(jsonPath("$.chats[0].lastMessageSenderId").value(HIGHER))
                    .andExpect(jsonPath("$.chats[0].unread").value(true));
        }

        @Test
        @DisplayName("ограничение по size")
        void shouldLimitBySize() throws Exception {
            for (int i = 1; i <= 5; i++) {
                saveChat(ME, "user-1" + i, NOW.minus(i, ChronoUnit.HOURS), "msg", ME, false);
            }

            mockMvc.perform(get("/").header(HEADER, ME).param("size", "2"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.chats", hasSize(2)));
        }

        @Test
        @DisplayName("400 без заголовка X-User-Id")
        void shouldFailWithoutHeader() throws Exception {
            mockMvc.perform(get("/"))
                    .andExpect(status().isBadRequest());
        }
    }

    // ------------------------------------------------------- GET /?after= (Пагинация)

    @Nested
    @DisplayName("GET /?after= — пагинация по времени")
    class GetChatsPaginatedBySince {

        @Test
        @DisplayName("возвращает следующие size чатов, строго старше after (lastModifiedAt < after)")
        void shouldReturnNextPageUsingSince() throws Exception {
            Chat chat1 = saveChat(ME, "user-11", NOW.minus(1, ChronoUnit.HOURS), "m1", ME, false);
            Chat chat2 = saveChat(ME, "user-12", NOW.minus(2, ChronoUnit.HOURS), "m2", ME, false);
            Chat chat3 = saveChat(ME, "user-13", NOW.minus(3, ChronoUnit.HOURS), "m3", ME, false);
            Chat chat4 = saveChat(ME, "user-14", NOW.minus(4, ChronoUnit.HOURS), "m4", ME, false);
            Chat chat5 = saveChat(ME, "user-15", NOW.minus(5, ChronoUnit.HOURS), "m5", ME, false);

            // Первая страница (без after)
            mockMvc.perform(get("/").header(HEADER, ME).param("size", "2"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.chats", hasSize(2)))
                    .andExpect(jsonPath("$.chats[0].id").value(chat1.getId()))
                    .andExpect(jsonPath("$.chats[1].id").value(chat2.getId()));

            // Вторая страница: передаем lastModifiedAt второго чата как after
            // Ожидаем chat3 и chat4, так как их время строго меньше chat2
            mockMvc.perform(get("/").header(HEADER, ME)
                            .param("after", chat2.getLastModifiedAt().toString())
                            .param("size", "2"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.chats", hasSize(2)))
                    .andExpect(jsonPath("$.chats[0].id").value(chat3.getId()))
                    .andExpect(jsonPath("$.chats[1].id").value(chat4.getId()));

            // Третья страница
            mockMvc.perform(get("/").header(HEADER, ME)
                            .param("after", chat4.getLastModifiedAt().toString())
                            .param("size", "2"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.chats", hasSize(1)))
                    .andExpect(jsonPath("$.chats[0].id").value(chat5.getId()));
        }

        @Test
        @DisplayName("пустой список, если after старше всех чатов пользователя")
        void shouldReturnEmptyListWhenSinceIsOlderThanAllChats() throws Exception {
            saveChat(ME, HIGHER, NOW, "m", ME, false);

            mockMvc.perform(get("/").header(HEADER, ME)
                            .param("after", NOW.minus(10, ChronoUnit.HOURS).toString())
                            .param("size", "50"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.chats", hasSize(0)));
        }
    }

    // ------------------------------------------------------- GET /new?since=

    @Nested
    @DisplayName("GET /new?since= — пуллинг обновлений")
    class GetChatsUpdatedSince {

        @Test
        @DisplayName("возвращает чаты, обновлённые после since (lastModifiedAt > since)")
        void shouldReturnChatsUpdatedSince() throws Exception {
            saveChat(ME, "user-4", NOW.minus(5, ChronoUnit.HOURS), "old", ME, false);
            Chat newChat = saveChat(ME, "user-5", NOW.minus(1, ChronoUnit.HOURS), "new", "user-5", true);

            mockMvc.perform(get("/new").header(HEADER, ME)
                            .param("since", NOW.minus(2, ChronoUnit.HOURS).toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.chats", hasSize(1)))
                    .andExpect(jsonPath("$.chats[0].id").value(newChat.getId()));
        }

        @Test
        @DisplayName("пустой список, если нет обновлений")
        void shouldReturnEmptyListWhenNoUpdates() throws Exception {
            saveChat(ME, HIGHER, NOW.minus(5, ChronoUnit.HOURS), "m", ME, false);

            mockMvc.perform(get("/new").header(HEADER, ME)
                            .param("since", NOW.minus(1, ChronoUnit.HOURS).toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.chats", hasSize(0)));
        }

        @Test
        @DisplayName("не возвращает чужие обновлённые чаты")
        void shouldNotReturnForeignUpdatedChats() throws Exception {
            saveChat(STRANGER_A, STRANGER_B, NOW.minus(1, ChronoUnit.HOURS), "m", STRANGER_A, true);

            mockMvc.perform(get("/new").header(HEADER, ME)
                            .param("since", NOW.minus(2, ChronoUnit.HOURS).toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.chats", hasSize(0)));
        }
    }

    // ------------------------------------------------------- GET /{userId}

    @Nested
    @DisplayName("GET /{userId} — один чат")
    class GetChat {

        @Test
        @DisplayName("находит чат, когда инициатор — userLow")
        void shouldFindChatWhenInitiatorIsUserLow() throws Exception {
            saveChat(ME, HIGHER, NOW, "Hello", HIGHER, true);

            mockMvc.perform(get("/{userId}", HIGHER).header(HEADER, ME))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.userId").value(HIGHER))
                    .andExpect(jsonPath("$.unread").value(true));
        }

        @Test
        @DisplayName("находит чат, когда инициатор — userHigh")
        void shouldFindChatWhenInitiatorIsUserHigh() throws Exception {
            saveChat(LOWER, ME, NOW, "Hello", LOWER, true);

            mockMvc.perform(get("/{userId}", LOWER).header(HEADER, ME))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.userId").value(LOWER))
                    .andExpect(jsonPath("$.unread").value(true));
        }

        @Test
        @DisplayName("404, если чата нет")
        void shouldReturn404WhenChatDoesNotExist() throws Exception {
            mockMvc.perform(get("/{userId}", HIGHER).header(HEADER, ME))
                    .andExpect(status().isNotFound());
        }
    }
}