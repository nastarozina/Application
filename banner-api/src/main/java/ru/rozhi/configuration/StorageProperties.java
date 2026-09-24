package ru.rozhi.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@Data
@ConfigurationProperties("storage.s3")
public class StorageProperties {

    private String bucket;

    private Duration presignedUrlExpiry = Duration.ofMinutes(5);
}