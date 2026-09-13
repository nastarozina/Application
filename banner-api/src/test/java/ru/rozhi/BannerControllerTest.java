package ru.rozhi;

import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mongodb.MongoDBContainer;
import ru.rozhi.controller.dto.BannerRequest;
import tools.jackson.core.type.TypeReference;
import org.testcontainers.utility.DockerImageName;
import ru.rozhi.controller.dto.BannerResponse;
import ru.rozhi.repository.BannerRepository;
import ru.rozhi.repository.model.Banner;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
public class BannerControllerTest {

    @Container
    @ServiceConnection
    static MongoDBContainer mongoDBContainer = new MongoDBContainer(
            DockerImageName.parse("mongo:7.0.0") // Укажите актуальную версию образа
    );

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BannerRepository bannerRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        bannerRepository.deleteAll();
    }

    @Test
    @SneakyThrows
    void shouldReturnAllBanners() {
        bannerRepository.save(Banner.builder().id("banner1").name("NAME1").description("DESCRIPTION1").build());
        bannerRepository.save(Banner.builder().id("banner2").name("NAME2").description("DESCRIPTION2").build());
        List<BannerResponse> expected = List.of(new BannerResponse("banner1", "NAME1", "DESCRIPTION1"),
                new BannerResponse("banner2","NAME2", "DESCRIPTION2"));

        MvcResult result = mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andReturn();

        List<BannerResponse> response =
                objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {});

        assertThat(response).isEqualTo(expected);
    }

    @Test
    @SneakyThrows
    void shouldReturnBannerById() {
        bannerRepository.save(Banner.builder().id("banner1").name("NAME1").description("DESCRIPTION1").build());
        bannerRepository.save(Banner.builder().id("banner2").name("NAME2").description("DESCRIPTION2").build());
        BannerResponse expected = new BannerResponse("banner2","NAME2", "DESCRIPTION2");

        MvcResult result = mockMvc.perform(get("/banner2"))
                .andExpect(status().isOk())
                .andReturn();

        BannerResponse response =
                objectMapper.readValue(result.getResponse().getContentAsString(), BannerResponse.class);

        assertThat(response).isEqualTo(expected);
    }

    @Test
    @SneakyThrows
    void shouldReturn404WhenBannerNotFound() {
        bannerRepository.save(Banner.builder().id("banner1").name("NAME1").description("DESCRIPTION1").build());
        mockMvc.perform(get("/banner2")).andExpect(status().isNotFound());
    }

    @Test
    @SneakyThrows
    void shouldCreateBannerAndReturnItById() {
        BannerRequest bannerRequest = new BannerRequest("NAME2", "DESCRIPTION2");
        MvcResult resultOfPostRequest = mockMvc.perform(post("/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bannerRequest))
                )
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andReturn();

        BannerResponse responseOfPostRequest =
                objectMapper.readValue(resultOfPostRequest.getResponse().getContentAsString(), BannerResponse.class);

        assertThat(responseOfPostRequest.id()).isNotNull();
        assertThat(responseOfPostRequest.name()).isEqualTo(bannerRequest.name());
        assertThat(responseOfPostRequest.description()).isEqualTo(bannerRequest.description());

        Banner banner = bannerRepository.findById(responseOfPostRequest.id()).orElse(null);
        assertThat(banner).isNotNull();

        String bannerUrl = resultOfPostRequest.getResponse().getHeader("Location");
        MvcResult resultOfGetRequest = mockMvc.perform(get(bannerUrl))
                .andExpect(status().isOk())
                .andReturn();

        BannerResponse responseOfGetRequest =
                objectMapper.readValue(resultOfGetRequest.getResponse().getContentAsString(), BannerResponse.class);

        assertThat(responseOfGetRequest.id()).isNotNull();
        assertThat(responseOfGetRequest.name()).isEqualTo(bannerRequest.name());
        assertThat(responseOfGetRequest.description()).isEqualTo(bannerRequest.description());
    }
}
