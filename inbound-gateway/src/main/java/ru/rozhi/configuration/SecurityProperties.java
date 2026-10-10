package ru.rozhi.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "security")
public class SecurityProperties {

    private List<SecurityRoute> routes = new ArrayList<>();
    private String defaultAccess = "authenticated";

    @Data
    public static class SecurityRoute {
        private List<String> paths = new ArrayList<>();
        private List<HttpMethod> methods = new ArrayList<>();
        private String access;
    }
}