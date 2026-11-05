package ru.rozhi.configuration;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.auditing.DateTimeProvider;

import java.time.Instant;
import java.time.temporal.TemporalAccessor;
import java.util.Optional;

@TestConfiguration
public class TestMongoAuditingConfig {

    public static final Instant NOW = Instant.parse("2026-11-02T10:00:00Z");
    private static Instant FIXED_INSTANT = NOW;

    public static void setFixedInstant(Instant instant) {
        FIXED_INSTANT = instant;
    }

    public static void clearFixedInstant() {
        FIXED_INSTANT = NOW;
    }

    @Bean
    @Primary
    public DateTimeProvider dateTimeProvider() {
        return () -> Optional.ofNullable(FIXED_INSTANT).map(TemporalAccessor.class::cast);
    }
}