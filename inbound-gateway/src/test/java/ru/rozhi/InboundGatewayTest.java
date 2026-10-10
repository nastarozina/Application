package ru.rozhi;

import com.github.tomakehurst.wiremock.client.WireMock;
import dasniko.testcontainers.keycloak.KeycloakContainer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import org.wiremock.spring.EnableWireMock;

import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@EnableWireMock
@Testcontainers
public class InboundGatewayTest {

    @Container
    static final KeycloakContainer keycloak = new KeycloakContainer(
            DockerImageName.parse("keycloak/keycloak:26.7")
    ).withRealmImportFile("test-realm.json");

    @Autowired
    private WebTestClient webTestClient;

    private String validUserToken;
    private String validAdminToken;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        // Указываем Spring Security брать ключи из запущенного контейнера Keycloak
        String authServerUrl = keycloak.getAuthServerUrl();
        registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri",
                () -> authServerUrl + "/realms/test");
        registry.add("spring.security.oauth2.resourceserver.jwt.jwk-set-uri",
                () -> authServerUrl + "/realms/test/protocol/openid-connect/certs");
    }

    @BeforeEach
    void setUp() {
        validUserToken = obtainAccessToken("testuser", "password");
        validAdminToken = obtainAccessToken("adminuser", "password");
    }

    @Test
    void whenNoToken_thenReturns401Unauthorized() throws Exception {
        webTestClient.get()
                .uri("/api/user/profile")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void whenInvalidToken_thenReturns401Unauthorized() throws Exception {
        webTestClient.get()
                .uri("/api/user/profile")
                .header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token-string")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void whenUserToken_thenReturns200AndForwardsUserIdHeader() throws Exception {
        String userId = extractSubject(validUserToken);
        stubFor(WireMock.get(urlEqualTo("/profile"))
                .withHeader("X-User-Id", equalTo(userId)) // Subject из токена
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"message\": \"Hello from user-service\"}")));

        // Выполняем запрос через Gateway
        webTestClient.get()
                .uri("/api/user/profile")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + validUserToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.message").isEqualTo("Hello from user-service");

        // Проверяем, что WireMock действительно получил запрос (маршрутизация сработала)
        verify(getRequestedFor(urlEqualTo("/profile"))
                .withHeader("X-User-Id", equalTo(userId)));
    }

    @Test
    void whenAdminTokenAccessingAdminEndpoint_thenReturns200() throws Exception {
        stubFor(WireMock.get(urlEqualTo("/data"))
                .willReturn(aResponse().withStatus(200)));

        webTestClient.get()
                .uri("/api/admin/data")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + validAdminToken)
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void whenUserTokenAccessingAdminEndpoint_thenReturns403Forbidden() throws Exception {
        // У testuser нет роли ADMIN, только USER
        webTestClient.get()
                .uri("/api/admin/data")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + validUserToken)
                .exchange()
                .expectStatus().isForbidden();
    }

    private String obtainAccessToken(String username, String password) {
        String tokenEndpoint = keycloak.getAuthServerUrl()
                + "/realms/test/protocol/openid-connect/token";

        RestClient restClient = RestClient.builder().build();

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "password");
        formData.add("client_id", "gateway-client");
        formData.add("username", username);
        formData.add("password", password);

        Map<String, Object> response = restClient.post()
                .uri(tokenEndpoint)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(formData)
                .retrieve()
                .body(Map.class);

        return (String) response.get("access_token");
    }

    private String extractSubject(String token) {
        JwtDecoder decoder = JwtDecoders.fromIssuerLocation(
                keycloak.getAuthServerUrl() + "/realms/test"
        );

        Jwt jwt = decoder.decode(token);
        return jwt.getSubject();
    }
}
