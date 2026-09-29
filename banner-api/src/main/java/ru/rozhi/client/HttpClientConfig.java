package ru.rozhi.client;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restclient.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.service.registry.ImportHttpServices;
import ru.rozhi.ApiErrorResponse;
import ru.rozhi.exception.ExternalException;
import tools.jackson.databind.ObjectMapper;

@ImportHttpServices(
        CategoryClient.class
)
@Configuration
public class HttpClientConfig {

    @Autowired
    private ObjectMapper objectMapper;

    @Bean
    RestClientCustomizer restClientExceptionCustomizer() {
        return restClientBuilder ->
                restClientBuilder.defaultStatusHandler(
                        HttpStatusCode::isError,
                        (_, response) -> {
                            ApiErrorResponse errorResponse = objectMapper.readValue(
                                    response.getBody(),
                                    ApiErrorResponse.class
                            );
                            throw new ExternalException(
                                    errorResponse.code(),
                                    errorResponse.message()
                            );
                        }
                );
    }
}