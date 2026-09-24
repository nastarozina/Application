package ru.rozhi.repository.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Image {

    private String id;

    private String objectKey;

    private String contentType;

    private Long size;

    private Integer position;

    private ImageStatus status;
}
