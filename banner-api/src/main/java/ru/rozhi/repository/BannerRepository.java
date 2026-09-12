package ru.rozhi.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import ru.rozhi.repository.model.Banner;

@Repository
public interface BannerRepository extends MongoRepository<Banner, String> {}
