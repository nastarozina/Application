package ru.rozhi.utils;

import org.springframework.http.HttpStatus;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;

public class StubUtils {

    public static void stubCategoryGetById(String id, String fileName, HttpStatus status) {
        stubFor(get(urlEqualTo("/" + id))
                .willReturn(
                        aResponse()
                                .withStatus(status.value())
                                .withHeader("Content-Type", "application/json")
                                .withBodyFile("wiremock/" + fileName + ".json")
                )
        );
    }
}
