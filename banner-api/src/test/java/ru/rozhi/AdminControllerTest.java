package ru.rozhi;

import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;
import ru.rozhi.client.CategoryClient;
import ru.rozhi.controller.dto.BannerRequest;
import ru.rozhi.controller.dto.BannerResponse;
import ru.rozhi.controller.dto.CategoryResponse;
import ru.rozhi.repository.model.Banner;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class AdminControllerTest extends BaseControllerTest {

    @MockitoBean
    private CategoryClient categoryClient;

    @Test
    @SneakyThrows
    void shouldReturn403WhenBannerToUpdateForbidden() {
        bannerRepository.save(Banner.builder().id("banner1").name("NAME1").description("DESCRIPTION1")
                .categoryId("1").ownerId("owner1").build());

        BannerRequest updateBannerRequest = new BannerRequest("NAME1", "DESCRIPTION2", "1");
        BannerResponse expected = new BannerResponse("banner1", "NAME1", "DESCRIPTION2",
                List.of(new CategoryResponse("1", "Electronics", null)), List.of(), "owner1");

        when(categoryClient.getCategoryChain("1")).thenReturn(
                List.of(new CategoryResponse("1", "Electronics", null))
        );

        MvcResult result = mockMvc.perform(
                put("/admin/{bannerId}", "banner1").header("X-User-Id", "owner2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateBannerRequest))
        ).andExpect(status().isOk()).andReturn();

        BannerResponse response = getResponse(result, BannerResponse.class);

        assertThat(expected).isEqualTo(response);
    }

    @Test
    @SneakyThrows
    void shouldReturn403WhenBannerToDeleteForbidden() {
        bannerRepository.save(Banner.builder().id("banner1").name("NAME1").description("DESCRIPTION1").ownerId("owner1")
                .categoryId("2").build());

        mockMvc.perform(delete("/admin/{bannerId}", "banner1").header("X-User-Id", "owner2"))
                .andExpect(status().isOk());

        Banner banner = bannerRepository.findById("banner1").orElse(null);
        assertThat(banner).isNull();

        mockMvc.perform(get("/banner1")).andExpect(status().isNotFound());
    }
}
