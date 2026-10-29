package ru.rozhi.repository.model;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

@Document(collection = "categories")
@Data
@Builder
public class Category {

    @Id
    private String id;

    @Indexed(unique = true)
    private String name;

    @Field(targetType = FieldType.OBJECT_ID)
    private String parentId;
}
