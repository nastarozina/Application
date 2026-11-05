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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mongodb.MongoDBContainer;
import org.testcontainers.utility.DockerImageName;
import ru.rozhi.configuration.TestMongoAuditingConfig;
import ru.rozhi.repository.ChatRepository;
import ru.rozhi.repository.MessageRepository;
import ru.rozhi.repository.model.Chat;
import ru.rozhi.repository.model.Message;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.rozhi.configuration.TestMongoAuditingConfig.NOW;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Import(TestMongoAuditingConfig.class)
class MessageControllerTest {

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
    private static final String OTHER = "user-3";
    private static final String STRANGER = "user-9";

    @BeforeEach
    void cleanUp() {
        messageRepository.deleteAll();
        chatRepository.deleteAll();
        TestMongoAuditingConfig.clearFixedInstant();
    }

    private Chat createChat(String userLow, String userHigh, Instant lastModifiedAt,
                            String lastMessagePreview, String lastMessageSenderId,
                            boolean lastMessageUnread) {
        TestMongoAuditingConfig.setFixedInstant(lastModifiedAt);
        Chat chat = chatRepository.save(Chat.builder()
                .userLow(userLow)
                .userHigh(userHigh)
                .lastMessagePreview(lastMessagePreview)
                .lastMessageSenderId(lastMessageSenderId)
                .lastMessageUnread(lastMessageUnread)
                .build());
        TestMongoAuditingConfig.clearFixedInstant();
        return chat;
    }

    private Message createMessage(String senderId, Chat chat, String text, Instant createdAt) {
        TestMongoAuditingConfig.setFixedInstant(createdAt);
        Message message = messageRepository.save(Message.builder()
                .senderId(senderId)
                .chatId(chat.getId())
                .text(text)
                .build());
        TestMongoAuditingConfig.clearFixedInstant();
        return message;
    }

    // ------------------------------------------------------- GET /message/{userId}

    @Nested
    @DisplayName("GET /message/{userId} — последние сообщения")
    class GetLatestMessages {

        @Test
        @DisplayName("404, если чата нет")
        void shouldReturn404WhenChatDoesNotExist() throws Exception {
            mockMvc.perform(get("/message/{userId}", OTHER).header(HEADER, ME))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("404, если инициатор не участвует в чате")
        void shouldReturn404WhenInitiatorIsNotParticipant() throws Exception {
            Chat chat = createChat(OTHER, STRANGER, NOW, "Hello", OTHER, false);
            createMessage(OTHER, chat, "Hello", NOW);

            mockMvc.perform(get("/message/{userId}", STRANGER).header(HEADER, ME))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("пустой список, если сообщений нет")
        void shouldReturnEmptyListWhenNoMessages() throws Exception {
            createChat(ME, OTHER, NOW, null, null, false);

            mockMvc.perform(get("/message/{userId}", OTHER).header(HEADER, ME))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.messages").isArray())
                    .andExpect(jsonPath("$.messages", hasSize(0)))
                    .andExpect(jsonPath("$.userId").value(OTHER));
        }

        @Test
        @DisplayName("возвращает последние size сообщений")
        void shouldReturnLatestMessages() throws Exception {
            Chat chat = createChat(ME, OTHER, NOW, null, null, false);
            createMessage(ME, chat, "First", NOW.minus(3, ChronoUnit.HOURS));
            createMessage(OTHER, chat, "Second", NOW.minus(2, ChronoUnit.HOURS));
            createMessage(ME, chat, "Third", NOW.minus(1, ChronoUnit.HOURS));
            createMessage(OTHER, chat, "Fourth", NOW);

            mockMvc.perform(get("/message/{userId}", OTHER)
                            .header(HEADER, ME).param("size", "2"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.messages", hasSize(2)))
                    .andExpect(jsonPath("$.messages[0].text").value("Fourth"))
                    .andExpect(jsonPath("$.messages[1].text").value("Third"));
        }

        @Test
        @DisplayName("помечает чат как прочитанный, если последнее сообщение от собеседника")
        void shouldMarkChatAsReadWhenLastMessageFromOther() throws Exception {
            Chat chat = createChat(ME, OTHER, NOW, "Hello from OTHER", OTHER, true);
            createMessage(OTHER, chat, "Hello from OTHER", NOW);

            mockMvc.perform(get("/message/{userId}", OTHER).header(HEADER, ME))
                    .andExpect(status().isOk());

            Chat updatedChat = chatRepository.findById(chat.getId()).orElseThrow();
            assertThat(updatedChat.isLastMessageUnread()).isFalse();
        }

        @Test
        @DisplayName("не помечает как прочитанный, если последнее сообщение от меня")
        void shouldNotMarkAsReadWhenLastMessageFromMe() throws Exception {
            Chat chat = createChat(ME, OTHER, NOW, "Hello from ME", ME, true);
            createMessage(ME, chat, "Hello from ME", NOW);

            mockMvc.perform(get("/message/{userId}", OTHER).header(HEADER, ME))
                    .andExpect(status().isOk());

            Chat updatedChat = chatRepository.findById(chat.getId()).orElseThrow();
            // lastMessageSenderId == ME (инициатор), значит флаг не меняется
            assertThat(updatedChat.isLastMessageUnread()).isTrue();
        }

        @Test
        @DisplayName("400 без заголовка X-User-Id")
        void shouldFailWithoutHeader() throws Exception {
            mockMvc.perform(get("/message/{userId}", OTHER))
                    .andExpect(status().isBadRequest());
        }
    }

    // ------------------------------------------------------- GET /message/{userId}?lastMessageId=

    @Nested
    @DisplayName("GET /message/{userId}?lastMessageId= — подгрузка старых")
    class GetMessagesBefore {

        @Test
        @DisplayName("возвращает сообщения до lastMessageId")
        void shouldReturnMessagesBeforeLastMessageId() throws Exception {
            Chat chat = createChat(ME, OTHER, NOW, null, null, false);
            Message msg1 = createMessage(ME, chat, "First", NOW.minus(4, ChronoUnit.HOURS));
            Message msg2 = createMessage(OTHER, chat, "Second", NOW.minus(3, ChronoUnit.HOURS));
            Message msg3 = createMessage(ME, chat, "Third", NOW.minus(2, ChronoUnit.HOURS));
            Message msg4 = createMessage(OTHER, chat, "Fourth", NOW.minus(1, ChronoUnit.HOURS));
            Message msg5 = createMessage(ME, chat, "Fifth", NOW);

            mockMvc.perform(get("/message/{userId}", OTHER)
                            .header(HEADER, ME)
                            .param("lastMessageId", msg3.getId())
                            .param("size", "2"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.messages", hasSize(2)))
                    .andExpect(jsonPath("$.messages[0].id").value(msg2.getId()))
                    .andExpect(jsonPath("$.messages[1].id").value(msg1.getId()));
        }

        @Test
        @DisplayName("пустой список, если нет сообщений до lastMessageId")
        void shouldReturnEmptyListWhenNoMessagesBefore() throws Exception {
            Chat chat = createChat(ME, OTHER, NOW, null, null, false);
            Message msg1 = createMessage(ME, chat, "First", NOW);

            mockMvc.perform(get("/message/{userId}", OTHER)
                            .header(HEADER, ME)
                            .param("lastMessageId", msg1.getId())
                            .param("size", "50"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.messages", hasSize(0)));
        }
    }

    // ------------------------------------------------------- GET /message/{userId}/new?since=

    @Nested
    @DisplayName("GET /message/{userId}/new?since= — новые сообщения")
    class GetMessagesSince {

        @Test
        @DisplayName("возвращает сообщения после since")
        void shouldReturnMessagesSince() throws Exception {
            Chat chat = createChat(ME, OTHER, NOW, null, null, false);
            createMessage(ME, chat, "Old", NOW.minus(5, ChronoUnit.HOURS));
            createMessage(OTHER, chat, "New1", NOW.minus(2, ChronoUnit.HOURS));
            createMessage(ME, chat, "New2", NOW.minus(1, ChronoUnit.HOURS));

            mockMvc.perform(get("/message/{userId}/new", OTHER)
                            .header(HEADER, ME)
                            .param("since", NOW.minus(3, ChronoUnit.HOURS).toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.messages", hasSize(2)))
                    .andExpect(jsonPath("$.messages[0].text").value("New1"))
                    .andExpect(jsonPath("$.messages[1].text").value("New2"));
        }

        @Test
        @DisplayName("пустой список, если нет новых сообщений")
        void shouldReturnEmptyListWhenNoNewMessages() throws Exception {
            Chat chat = createChat(ME, OTHER, NOW, null, null, false);
            createMessage(ME, chat, "Old", NOW.minus(5, ChronoUnit.HOURS));

            mockMvc.perform(get("/message/{userId}/new", OTHER)
                            .header(HEADER, ME)
                            .param("since", NOW.minus(1, ChronoUnit.HOURS).toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.messages", hasSize(0)));
        }
    }

    // ------------------------------------------------------- POST /message/{userId}

    @Nested
    @DisplayName("POST /message/{userId} — отправка сообщения")
    class SendMessage {

        @Test
        @DisplayName("создаёт сообщение и возвращает 200")
        void shouldCreateMessageAndReturn200() throws Exception {
            createChat(ME, OTHER, NOW, null, null, false);

            String requestBody = """
                    {
                        "text": "Hello, world!"
                    }
                    """;

            mockMvc.perform(post("/message/{userId}", OTHER)
                            .header(HEADER, ME)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk());

            mockMvc.perform(get("/message/{userId}", OTHER).header(HEADER, ME))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.messages", hasSize(1)))
                    .andExpect(jsonPath("$.messages[0].text").value("Hello, world!"))
                    .andExpect(jsonPath("$.messages[0].senderId").value(ME));
        }

        @Test
        @DisplayName("создаёт чат, если его нет")
        void shouldCreateChatIfNotExists() throws Exception {
            String requestBody = """
                    {
                        "text": "First message"
                    }
                    """;

            mockMvc.perform(post("/message/{userId}", OTHER)
                            .header(HEADER, ME)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk());

            mockMvc.perform(get("/message/{userId}", OTHER).header(HEADER, ME))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.messages", hasSize(1)));
        }

        @Test
        @DisplayName("обновляет lastMessageInfo в чате")
        void shouldUpdateLastMessageInfo() throws Exception {
            Chat chat = createChat(ME, OTHER, NOW, null, null, false);

            String requestBody = """
                    {
                        "text": "New message"
                    }
                    """;

            mockMvc.perform(post("/message/{userId}", OTHER)
                            .header(HEADER, ME)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk());

            Chat updatedChat = chatRepository.findById(chat.getId()).orElseThrow();
            assertThat(updatedChat.getLastMessagePreview()).isEqualTo("New message");
            assertThat(updatedChat.getLastMessageSenderId()).isEqualTo(ME);
            assertThat(updatedChat.getLastMessageAt()).isNotNull();
        }

        @Test
        @DisplayName("помечает чат как непрочитанный")
        void shouldMarkChatAsUnread() throws Exception {
            Chat chat = createChat(ME, OTHER, NOW, null, null, false);

            String requestBody = """
                    {
                        "text": "New message"
                    }
                    """;

            mockMvc.perform(post("/message/{userId}", OTHER)
                            .header(HEADER, ME)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk());

            Chat updatedChat = chatRepository.findById(chat.getId()).orElseThrow();
            assertThat(updatedChat.isLastMessageUnread()).isTrue();
        }

        @Test
        @DisplayName("400 при пустом теле запроса")
        void shouldFailWithEmptyBody() throws Exception {
            mockMvc.perform(post("/message/{userId}", OTHER)
                            .header(HEADER, ME)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("400 при невалидном JSON")
        void shouldFailWithInvalidJson() throws Exception {
            mockMvc.perform(post("/message/{userId}", OTHER)
                            .header(HEADER, ME)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{ invalid json }"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("400 без заголовка X-User-Id")
        void shouldFailWithoutHeader() throws Exception {
            String requestBody = """
                    {
                        "text": "Hello"
                    }
                    """;

            mockMvc.perform(post("/message/{userId}", OTHER)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isBadRequest());
        }
    }
}