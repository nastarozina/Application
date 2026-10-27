package ru.rozhi.utils;

import com.github.tomakehurst.wiremock.client.WireMock;
import org.springframework.http.HttpStatus;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.havingExactly;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;

public class StubUtils {

    public static void stubCategoryGetChain(String id, String fileName, HttpStatus status) {
        stubFor(get(urlEqualTo("/categories/" + id))
                .willReturn(
                        aResponse()
                                .withStatus(status.value())
                                .withHeader("Content-Type", "application/json")
                                .withBodyFile("wiremock/" + fileName + ".json")
                )
        );
    }

    public static void stubCategoryGetNames(String fileName, HttpStatus status, String... params) {
        stubFor(WireMock.get(urlPathEqualTo("/names"))
                .withQueryParam("categoryId", havingExactly(params))
                .willReturn(
                        aResponse()
                                .withStatus(status.value())
                                .withHeader("Content-Type", "application/json")
                                .withBodyFile("wiremock/" + fileName + ".json")
                )
        );
    }
}
