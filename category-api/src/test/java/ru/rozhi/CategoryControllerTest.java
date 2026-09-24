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
import org.testcontainers.utility.DockerImageName;
import ru.rozhi.controller.dto.CategoryRequest;
import ru.rozhi.controller.dto.CategoryResponse;
import ru.rozhi.repository.CategoryRepository;
import ru.rozhi.repository.model.Category;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
public class CategoryControllerTest {

    @Container
    @ServiceConnection
    static MongoDBContainer mongoDBContainer = new MongoDBContainer(
            DockerImageName.parse("mongo:8.3.11")
    );

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        categoryRepository.deleteAll();
    }

    @Test
    @SneakyThrows
    void shouldReturnAllCategories() {
        categoryRepository.save(Category.builder().id("category1").name("NAME1").build());
        categoryRepository.save(Category.builder().id("category2").name("NAME2").build());
        List<CategoryResponse> expected = List.of(new CategoryResponse("category1", "NAME1"),
                new CategoryResponse("category2","NAME2"));

        MvcResult result = mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andReturn();

        List<CategoryResponse> response =
                objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {});

        assertThat(response).isEqualTo(expected);
    }

    @Test
    @SneakyThrows
    void shouldReturnCategoryById() {
        categoryRepository.save(Category.builder().id("category1").name("NAME1").build());
        categoryRepository.save(Category.builder().id("category2").name("NAME2").build());
        CategoryResponse expected = new CategoryResponse("category2","NAME2");

        MvcResult result = mockMvc.perform(get("/category2"))
                .andExpect(status().isOk())
                .andReturn();

        CategoryResponse response =
                objectMapper.readValue(result.getResponse().getContentAsString(), CategoryResponse.class);

        assertThat(response).isEqualTo(expected);
    }

    @Test
    @SneakyThrows
    void shouldReturn404WhenCategoryNotFound() {
        categoryRepository.save(Category.builder().id("category1").name("NAME1").build());
        mockMvc.perform(get("/category2")).andExpect(status().isNotFound());
    }

    @Test
    @SneakyThrows
    void shouldCreateCategoryAndReturnItById() {
        CategoryRequest categoryRequest = new CategoryRequest("NAME2");
        MvcResult resultOfPostRequest =
                mockMvc.perform(
                                post("/")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(categoryRequest))
                        )
                        .andExpect(status().isCreated())
                        .andExpect(header().exists("Location"))
                        .andReturn();

        CategoryResponse responseOfPostRequest =
                objectMapper.readValue(resultOfPostRequest.getResponse().getContentAsString(), CategoryResponse.class);

        assertThat(responseOfPostRequest.id()).isNotNull();
        assertThat(responseOfPostRequest.name()).isEqualTo(categoryRequest.name());

        Category category = categoryRepository.findById(responseOfPostRequest.id()).orElse(null);
        assertThat(category).isNotNull();

        String categoryUrl = resultOfPostRequest.getResponse().getHeader("Location");
        MvcResult resultOfGetRequest = mockMvc.perform(get(categoryUrl))
                .andExpect(status().isOk())
                .andReturn();

        CategoryResponse responseOfGetRequest =
                objectMapper.readValue(resultOfGetRequest.getResponse().getContentAsString(), CategoryResponse.class);

        assertThat(responseOfGetRequest.id()).isNotNull();
        assertThat(responseOfGetRequest.name()).isEqualTo(categoryRequest.name());
    }

    @Test
    @SneakyThrows
    void shouldUpdateCategoryAndReturnIt() {
        categoryRepository.save(Category.builder().id("category1").name("NAME1").build());

        CategoryRequest updateCategoryRequest1 = new CategoryRequest("  ");
        CategoryResponse expected1 = new CategoryResponse("category1", "NAME1");

        CategoryRequest updateCategoryRequest2 = new CategoryRequest("NAME2");
        CategoryResponse expected2 = new CategoryResponse("category1", "NAME2");

        MvcResult result1 = mockMvc.perform(
                        put("/category1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(updateCategoryRequest1))
                )
                .andExpect(status().isOk())
                .andReturn();

        CategoryResponse response1 = objectMapper.readValue(result1.getResponse().getContentAsString(), CategoryResponse.class);

        assertThat(response1).isEqualTo(expected1);

        MvcResult result2 = mockMvc.perform(
                        put("/category1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(updateCategoryRequest2))
                )
                .andExpect(status().isOk())
                .andReturn();

        CategoryResponse response2 = objectMapper.readValue(result2.getResponse().getContentAsString(), CategoryResponse.class);

        assertThat(response2).isEqualTo(expected2);
    }

    @Test
    @SneakyThrows
    void shouldReturn404WhenCategoryToUpdateNotFound() {
        CategoryRequest updateCategoryRequest = new CategoryRequest("NAME1");

        mockMvc.perform(
                        put("/category1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(updateCategoryRequest))
                )
                .andExpect(status().isNotFound());
    }

    @Test
    @SneakyThrows
    void shouldDeleteCategoryById() {
        categoryRepository.save(Category.builder().id("category1").name("NAME1").build());

        mockMvc.perform(delete("/category1"))
                .andExpect(status().isOk());

        Category category = categoryRepository.findById("category1").orElse(null);
        assertThat(category).isNull();

        mockMvc.perform(get("/category1")).andExpect(status().isNotFound());
    }

    @Test
    @SneakyThrows
    void shouldReturn404WhenCategoryToDeleteNotFound() {
        categoryRepository.save(Category.builder().id("category1").name("NAME1").build());

        mockMvc.perform(delete("/category2"))
                .andExpect(status().isNotFound());
    }
}
