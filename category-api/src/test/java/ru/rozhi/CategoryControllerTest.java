package ru.rozhi;

import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
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
import java.util.Map;

import static org.mockito.Mockito.verify;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class CategoryControllerTest {

    @Container
    @ServiceConnection
    static MongoDBContainer mongoDBContainer = new MongoDBContainer(
            DockerImageName.parse("mongo:8.3.11")
    );

    @Autowired
    private MockMvc mockMvc;

    @MockitoSpyBean
    private CategoryRepository categoryRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    void setUp() {
        categoryRepository.deleteAll();
        Cache cache = cacheManager.getCache("categoryChains");
        if (cache != null) {
            cache.clear();
        }
    }

    @Test
    @SneakyThrows
    void shouldReturnAllCategories() {
        categoryRepository.save(Category.builder().id("category1").name("NAME1").build());
        categoryRepository.save(Category.builder().id("category2").name("NAME2").build());
        List<CategoryResponse> expected = List.of(new CategoryResponse("category1", "NAME1", null),
                new CategoryResponse("category2","NAME2", null));

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
        CategoryResponse expected = new CategoryResponse("category2","NAME2", null);

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
    void shouldReturnCategoriesNamesByIds() {
        categoryRepository.save(Category.builder().id("1").name("NAME1").build());
        categoryRepository.save(Category.builder().id("2").name("NAME2").build());
        Map<String, CategoryResponse> expected = Map.of( "1", new CategoryResponse("1","NAME1", null)
                , "2", new CategoryResponse("2","NAME2", null));

        MvcResult result = mockMvc.perform(get("/names").param("categoryId", "1", "2"))
                .andExpect(status().isOk())
                .andReturn();

        Map<String, CategoryResponse> response =
                objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {});

        assertThat(response).isEqualTo(expected);
    }

    @Test
    @SneakyThrows
    void shouldCreateCategoryAndReturnItById() {
        CategoryRequest categoryRequest = new CategoryRequest("NAME2", null);

        MvcResult resultOfPostRequest = createCategory(categoryRequest)
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

        CategoryRequest updateCategoryRequest1 = new CategoryRequest("  ", null);
        CategoryResponse expected1 = new CategoryResponse("category1", "NAME1", null);

        CategoryRequest updateCategoryRequest2 = new CategoryRequest("NAME2", null);
        CategoryResponse expected2 = new CategoryResponse("category1", "NAME2", null);

        MvcResult result1 = updateCategory("category1", updateCategoryRequest1)
                .andExpect(status().isOk())
                .andReturn();

        CategoryResponse response1 = objectMapper.readValue(result1.getResponse().getContentAsString(), CategoryResponse.class);

        assertThat(response1).isEqualTo(expected1);

        MvcResult result2 = updateCategory("category1", updateCategoryRequest2)
                .andExpect(status().isOk())
                .andReturn();

        CategoryResponse response2 = objectMapper.readValue(result2.getResponse().getContentAsString(), CategoryResponse.class);

        assertThat(response2).isEqualTo(expected2);
    }

    @Test
    @SneakyThrows
    void shouldReturn404WhenCategoryToUpdateNotFound() {
        CategoryRequest updateCategoryRequest = new CategoryRequest("NAME1", null);

        updateCategory("category1", updateCategoryRequest)
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

    @Test
    @SneakyThrows
    void shouldReturnCategoryChain() {
        categoryRepository.save(Category.builder().id("root").name("ROOT").build());
        categoryRepository.save(Category.builder().id("child1").name("CHILD1").parentId("root").build());
        categoryRepository.save(Category.builder().id("child2").name("CHILD2").parentId("child1").build());

        List<CategoryResponse> expected = List.of(
                new CategoryResponse("root", "ROOT", null),
                new CategoryResponse("child1", "CHILD1", "root"),
                new CategoryResponse("child2", "CHILD2", "child1")
        );

        MvcResult result = mockMvc.perform(get("/categories/child2"))
                .andExpect(status().isOk())
                .andReturn();

        List<CategoryResponse> response =
                objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {});

        assertThat(response).isEqualTo(expected);
    }

    @Test
    @SneakyThrows
    void shouldCacheCategoryChain() {
        categoryRepository.save(Category.builder().id("root").name("ROOT").build());
        categoryRepository.save(Category.builder().id("child1").name("CHILD1").parentId("root").build());
        categoryRepository.save(Category.builder().id("child2").name("CHILD2").parentId("child1").build());

        Cache cache = cacheManager.getCache("categoryChains");
        assertThat(cache).isNotNull();
        assertThat(cache.get("child2")).isNull();

        mockMvc.perform(get("/categories/child2"))
                .andExpect(status().isOk());

        Cache.ValueWrapper cachedValue = cache.get("child2");
        assertThat(cachedValue).isNotNull();
        verify(categoryRepository, atLeastOnce()).findById(anyString());
        clearInvocations(categoryRepository);

        List<CategoryResponse> cachedResponse = (List<CategoryResponse>) cachedValue.get();
        assertThat(cachedResponse).hasSize(3);
        assertThat(cachedResponse.get(0).id()).isEqualTo("root");
        assertThat(cachedResponse.get(2).id()).isEqualTo("child2");

        MvcResult result2 = mockMvc.perform(get("/categories/child2"))
                .andExpect(status().isOk())
                .andReturn();

        List<CategoryResponse> response2 =
                objectMapper.readValue(result2.getResponse().getContentAsString(), new TypeReference<>() {});

        verifyNoInteractions(categoryRepository);
        assertThat(response2).isEqualTo(cachedResponse);
    }

    @Test
    @SneakyThrows
    void shouldEvictCacheOnUpdate() {
        categoryRepository.save(Category.builder().id("category1").name("NAME1").build());

        mockMvc.perform(get("/categories/category1"))
                .andExpect(status().isOk());

        Cache cache = cacheManager.getCache("categoryChains");
        assertThat(cache.get("category1")).isNotNull();

        CategoryRequest updateRequest = new CategoryRequest("NAME2", null);

        updateCategory("category1", updateRequest).andExpect(status().isOk());

        assertThat(cache.get("category1")).isNull();
    }

    @Test
    @SneakyThrows
    void shouldEvictCacheOnDelete() {
        categoryRepository.save(Category.builder().id("category1").name("NAME1").build());

        mockMvc.perform(get("/categories/category1"))
                .andExpect(status().isOk());

        Cache cache = cacheManager.getCache("categoryChains");
        assertThat(cache.get("category1")).isNotNull();

        mockMvc.perform(delete("/category1"))
                .andExpect(status().isOk());

        assertThat(cache.get("category1")).isNull();
    }

    @SneakyThrows
    private ResultActions createCategory(CategoryRequest request) {
        return mockMvc.perform(
                post("/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        );
    }

    @SneakyThrows
    private ResultActions updateCategory(String categoryId, CategoryRequest request) {
        return mockMvc.perform(
                put("/{categoryId}", categoryId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        );
    }
}
