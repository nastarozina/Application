package ru.rozhi;

import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mongodb.MongoDBContainer;
import org.testcontainers.utility.DockerImageName;
import org.wiremock.spring.EnableWireMock;
import ru.rozhi.repository.BannerRepository;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@EnableWireMock
@Testcontainers
public abstract class BaseControllerTest {

    @Container
    @ServiceConnection
    protected static MongoDBContainer mongoDBContainer = new MongoDBContainer(
            DockerImageName.parse("mongo:8.3.11")
    );

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected BannerRepository bannerRepository;

    @Autowired
    protected ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        bannerRepository.deleteAll();
    }

    @SneakyThrows
    protected  <T> T getResponse(MvcResult result, Class<T> clazz) {
        return objectMapper.readValue(result.getResponse().getContentAsString(), clazz);
    }
}
