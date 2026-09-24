package ru.rozhi.repository.model;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;


@Document(collection = "banners")
@Data
@Builder
public class Banner{

    @Id
    private String id;

    private String name;

    private String description;

    @Builder.Default
    private List<Image> images = new ArrayList<>();
}
