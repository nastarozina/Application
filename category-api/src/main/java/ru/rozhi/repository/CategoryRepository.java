package ru.rozhi.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import ru.rozhi.repository.model.Category;

@Repository
public interface CategoryRepository extends MongoRepository<Category, String> {}
