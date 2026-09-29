package ru.rozhi;

import com.github.tomakehurst.wiremock.client.WireMock;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import ru.rozhi.controller.dto.BannerRequest;
import tools.jackson.core.type.TypeReference;
import ru.rozhi.controller.dto.BannerResponse;
import ru.rozhi.repository.model.Banner;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.havingExactly;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.rozhi.utils.StubUtils.stubCategoryGetById;

public class BannerControllerTest extends BaseControllerTest {

    @Test
    @SneakyThrows
    void shouldReturnAllBanners() {
        bannerRepository.save(Banner.builder().id("banner1").name("NAME1").description("DESCRIPTION1").categoryId("1").build());
        bannerRepository.save(Banner.builder().id("banner2").name("NAME2").description("DESCRIPTION2").categoryId("2").build());
        List<BannerResponse> expected = List.of(
                new BannerResponse("banner1", "NAME1", "DESCRIPTION1", "Electronics", List.of()),
                new BannerResponse("banner2","NAME2", "DESCRIPTION2", "Cars", List.of())
        );

        stubFor(WireMock.get(urlPathEqualTo("/names"))
                .withQueryParam("categoriesId", havingExactly("1", "2"))
                .willReturn(
                        aResponse()
                                .withStatus(200)
                                .withHeader("Content-Type", "application/json")
                                .withBodyFile("wiremock/response-3.json")
                )
        );

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
        bannerRepository.save(Banner.builder().id("banner1").name("NAME1").description("DESCRIPTION1").categoryId("1")
                .build());

        bannerRepository.save(Banner.builder().id("banner2").name("NAME2").description("DESCRIPTION2").categoryId("2")
                .build());
        BannerResponse expected = new BannerResponse("banner2","NAME2", "DESCRIPTION2",
                "Electronics", List.of());

        stubCategoryGetById("2", "response-1", HttpStatus.OK);

        MvcResult result = mockMvc.perform(get("/banner2"))
                .andExpect(status().isOk())
                .andReturn();

        BannerResponse response = getResponse(result, BannerResponse.class);

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
        BannerRequest bannerRequest = new BannerRequest("NAME2", "DESCRIPTION2", "2");
        stubCategoryGetById("2", "response-1", HttpStatus.OK);

        MvcResult resultOfPostRequest = createBanner(bannerRequest)
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andReturn();

        BannerResponse responseOfPostRequest = getResponse(resultOfPostRequest, BannerResponse.class);

        assertThat(responseOfPostRequest.id()).isNotNull();
        assertThat(responseOfPostRequest.name()).isEqualTo(bannerRequest.name());
        assertThat(responseOfPostRequest.description()).isEqualTo(bannerRequest.description());
        assertThat(responseOfPostRequest.categoryName()).isEqualTo("Electronics");

        Banner banner = bannerRepository.findById(responseOfPostRequest.id()).orElse(null);
        assertThat(banner).isNotNull();

        String bannerUrl = resultOfPostRequest.getResponse().getHeader("Location");
        MvcResult resultOfGetRequest = mockMvc.perform(get(bannerUrl))
                .andExpect(status().isOk())
                .andReturn();

        BannerResponse responseOfGetRequest = getResponse(resultOfGetRequest, BannerResponse.class);

        assertThat(responseOfGetRequest.id()).isNotNull();
        assertThat(responseOfGetRequest.name()).isEqualTo(bannerRequest.name());
        assertThat(responseOfGetRequest.description()).isEqualTo(bannerRequest.description());
        assertThat(responseOfPostRequest.categoryName()).isEqualTo("Electronics");
    }

    @Test
    @SneakyThrows
    void shouldNotCreateBannerBecauseCategoryNotFound() {
        BannerRequest bannerRequest = new BannerRequest("NAME2", "DESCRIPTION2", "1");

        stubCategoryGetById("1", "response-2", HttpStatus.NOT_FOUND);

        MvcResult result = createBanner(bannerRequest)
                .andExpect(status().isInternalServerError())
                .andExpect(header().doesNotExist("Location"))
                .andReturn();

        ApiErrorResponse response = getResponse(result, ApiErrorResponse.class);

        assertThat(response.code()).isEqualTo("CATEGORY_NOT_FOUND");
        assertThat(response.message()).isEqualTo("Category not found: 1");

        Banner banner = bannerRepository.findByName("NAME2").orElse(null);
        assertThat(banner).isNull();
    }

    @Test
    @SneakyThrows
    void shouldUpdateBannerAndReturnIt() {
        String bannerId = "banner1";
        bannerRepository.save(Banner.builder().id(bannerId).name("NAME1").description("DESCRIPTION1")
                .build());

        BannerRequest updateBannerRequest1 = new BannerRequest("  ", "DESCRIPTION2", "2");
        BannerResponse expected1 = new BannerResponse(bannerId, "NAME1", "DESCRIPTION2",
                "Electronics", List.of());

        BannerRequest updateBannerRequest2 = new BannerRequest("NAME2", null, "2");
        BannerResponse expected2 = new BannerResponse(bannerId, "NAME2", "DESCRIPTION2",
                "Electronics", List.of());

        BannerRequest updateBannerRequest3 = new BannerRequest("NAME3", "DESCRIPTION2", "1");
        Banner expected3 = Banner.builder().id(bannerId).name("NAME2").description("DESCRIPTION2").categoryId("2")
                .build();

        stubCategoryGetById("1", "response-2", HttpStatus.NOT_FOUND);
        stubCategoryGetById("2", "response-1", HttpStatus.OK);

        MvcResult result1 = updateBanner(bannerId, updateBannerRequest1)
                .andExpect(status().isOk())
                .andReturn();

        BannerResponse response1 = getResponse(result1, BannerResponse.class);

        assertThat(response1).isEqualTo(expected1);

        MvcResult result2 = updateBanner(bannerId, updateBannerRequest2)
                .andExpect(status().isOk())
                .andReturn();

        BannerResponse response2 = getResponse(result2, BannerResponse.class);

        assertThat(response2).isEqualTo(expected2);

        updateBanner(bannerId, updateBannerRequest3)
                .andExpect(status().isInternalServerError());

        Banner result3 = bannerRepository.findById(bannerId).orElse(null);
        assertThat(expected3).isEqualTo(result3);
    }

    @Test
    @SneakyThrows
    void shouldReturn404WhenBannerToUpdateNotFound() {
        BannerRequest updateBannerRequest = new BannerRequest("NAME1", "DESCRIPTION1", "");

        updateBanner("banner1", updateBannerRequest)
                .andExpect(status().isNotFound());
    }

    @Test
    @SneakyThrows
    void shouldDeleteBannerById() {
        bannerRepository.save(Banner.builder().id("banner1").name("NAME1").description("DESCRIPTION1").build());

        deleteBanner("banner1")
                .andExpect(status().isOk());

        Banner banner = bannerRepository.findById("banner1").orElse(null);
        assertThat(banner).isNull();

        mockMvc.perform(get("/banner1")).andExpect(status().isNotFound());
    }

    @Test
    @SneakyThrows
    void shouldReturn404WhenBannerToDeleteNotFound() {
        bannerRepository.save(Banner.builder().id("banner1").name("NAME1").description("DESCRIPTION1").build());

        deleteBanner("banner2")
                .andExpect(status().isNotFound());
    }

    @SneakyThrows
    private ResultActions createBanner(BannerRequest request) {
        return mockMvc.perform(
                post("/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        );
    }

    @SneakyThrows
    private ResultActions updateBanner(String bannerId, BannerRequest request) {
        return mockMvc.perform(
                put("/{bannerId}", bannerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        );
    }

    @SneakyThrows
    private ResultActions deleteBanner(String bannerId) {
        return mockMvc.perform(delete("/{bannerId}", bannerId));
    }
}
