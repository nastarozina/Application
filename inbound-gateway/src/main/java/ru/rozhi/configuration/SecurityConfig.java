package ru.rozhi.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final SecurityProperties securityProperties;
    private final AuthErrorHandler authErrorHandler;

    public SecurityConfig(SecurityProperties securityProperties,
                          AuthErrorHandler authErrorHandler) {
        this.securityProperties = securityProperties;
        this.authErrorHandler = authErrorHandler;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> {})
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(new CustomJwtAuthenticationConverter()))
                )
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(authErrorHandler)
                        .accessDeniedHandler(authErrorHandler)
                );

        http.authorizeHttpRequests(auth -> {
            for (SecurityProperties.SecurityRoute route : securityProperties.getRoutes()) {
                applyRoute(auth, route);
            }
            applyDefaultRule(auth, securityProperties.getDefaultAccess());
        });

        return http.build();
    }

    private void applyRoute(
            AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry auth,
            SecurityProperties.SecurityRoute route
    ) {
        String[] pathPatterns = route.getPaths().toArray(new String[0]);

        if (route.getMethods() == null || route.getMethods().isEmpty()) {
            applyAccess(auth.requestMatchers(pathPatterns), route.getAccess());
        } else {
            for (HttpMethod method : route.getMethods()) {
                applyAccess(auth.requestMatchers(method, pathPatterns), route.getAccess());
            }
        }
    }

    private void applyDefaultRule(
            AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry auth,
            String defaultAccess
    ) {
        applyAccess(auth.anyRequest(), defaultAccess);
    }


    private void applyAccess(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizedUrl spec, String access) {
        String a = access.toLowerCase().trim();

        if (a.equals("permit")) {
            spec.permitAll();
        } else if (a.equals("authenticated")) {
            spec.authenticated();
        } else if (a.equals("deny")) {
             spec.denyAll();
        } else if (a.startsWith("role:")) {
            String[] roles = a.substring(5).split(",");
            for (int i = 0; i < roles.length; i++) roles[i] = roles[i].trim().toUpperCase();
            spec.hasAnyRole(roles);
        } else if (a.startsWith("authority:")) {
            String[] auths = a.substring(10).split(",");
            for (int i = 0; i < auths.length; i++) auths[i] = auths[i].trim();
            spec.hasAnyAuthority(auths);
        }
    }
}