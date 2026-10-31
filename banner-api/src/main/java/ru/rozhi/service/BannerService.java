package ru.rozhi.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import ru.rozhi.client.CategoryClient;
import ru.rozhi.controller.dto.BannerRequest;
import ru.rozhi.controller.dto.BannerResponse;
import ru.rozhi.controller.dto.CategoryResponse;
import ru.rozhi.exception.BannerForbiddenException;
import ru.rozhi.exception.BannerNotFoundException;
import ru.rozhi.repository.BannerRepository;
import ru.rozhi.repository.model.Banner;
import ru.rozhi.utils.MapperUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class BannerService {

    @Autowired
    private BannerRepository repository;

    @Autowired
    private CategoryClient categoryClient;

    @Autowired
    private MongoTemplate mongoTemplate;

    public BannerResponse getBannerById(String id) {
        Banner banner = repository.findById(id)
                .orElseThrow(() -> new BannerNotFoundException(id));
        List<CategoryResponse> categories = categoryClient.getCategoryChain(banner.getCategoryId());
        return MapperUtils.getBannerResponse(banner, categories);
    }

    public List<BannerResponse> getAllBanners(String categoryId, String ownerId) {
        List<Criteria> criteriaList = new ArrayList<>();

        if (categoryId != null) {
            List<String> childCategoryIds = categoryClient.getChildCategoryIds(categoryId);

            List<String> allCategoryIds = new ArrayList<>();
            allCategoryIds.add(categoryId);
            allCategoryIds.addAll(childCategoryIds);

            criteriaList.add(
                    Criteria.where("categoryId").in(allCategoryIds)
            );
        }

        addAndIfNotNull(criteriaList, "ownerId", ownerId);

        Query query = new Query();
        if (!criteriaList.isEmpty()) {
            query.addCriteria(new Criteria().andOperator(criteriaList.toArray(new Criteria[0])));
        }

        List<Banner> banners = mongoTemplate.find(query, Banner.class);

        if (banners.isEmpty()) {
            return List.of();
        }

        List<String> ids = banners.stream()
                .map(Banner::getCategoryId)
                .distinct()
                .toList();

        Map<String, CategoryResponse> categoryNames = categoryClient.getCategoryNames(ids);

        return banners.stream()
                .map(banner -> MapperUtils.getBannerResponse(
                        banner,
                        List.of(categoryNames.get(banner.getCategoryId()))
                ))
                .toList();
    }

    private void addAndIfNotNull(List<Criteria> criteriaList, String fieldName, Object value) {
        if (value != null) {
            criteriaList.add(Criteria.where(fieldName).is(value));
        }
    }

    public BannerResponse createBanner(BannerRequest banner, String userId) {
        List<CategoryResponse> categories = categoryClient.getCategoryChain(banner.categoryId());
        Banner createdBanner = repository.save(Banner.builder()
                .name(banner.name())
                .description(banner.description())
                .categoryId(banner.categoryId())
                .ownerId(userId)
                .build());
        return MapperUtils.getBannerResponse(createdBanner, categories);
    }

    public BannerResponse updateBanner(String id, BannerRequest bannerToUpdate, String userId) {
        Banner banner = repository.findById(id)
                .orElseThrow(() -> new BannerNotFoundException(id));

        if (!banner.getOwnerId().equals(userId)) {
            throw new BannerForbiddenException(id);
        }

        if (bannerToUpdate.name() != null && !bannerToUpdate.name().isBlank()) {
            banner.setName(bannerToUpdate.name());
        }

        if (bannerToUpdate.description() != null) {
            banner.setDescription(bannerToUpdate.description());
        }

        if (bannerToUpdate.categoryId() != null && !bannerToUpdate.categoryId().isBlank()) {
            banner.setCategoryId(bannerToUpdate.categoryId());
        }

        List<CategoryResponse> categories = categoryClient.getCategoryChain(banner.getCategoryId());

        banner = repository.save(banner);

        return MapperUtils.getBannerResponse(banner, categories);
    }

    public BannerResponse updateBanner(String id, BannerRequest bannerToUpdate) {
        Banner banner = repository.findById(id)
                .orElseThrow(() -> new BannerNotFoundException(id));

        if (bannerToUpdate.name() != null && !bannerToUpdate.name().isBlank()) {
            banner.setName(bannerToUpdate.name());
        }

        if (bannerToUpdate.description() != null) {
            banner.setDescription(bannerToUpdate.description());
        }

        if (bannerToUpdate.categoryId() != null && !bannerToUpdate.categoryId().isBlank()) {
            banner.setCategoryId(bannerToUpdate.categoryId());
        }

        List<CategoryResponse> categories = categoryClient.getCategoryChain(banner.getCategoryId());

        banner = repository.save(banner);

        return MapperUtils.getBannerResponse(banner, categories);
    }

    public void deleteBanner(String id, String userId) {
        Banner banner = repository.findById(id)
                .orElseThrow(() -> new BannerNotFoundException(id));

        if (!banner.getOwnerId().equals(userId)) {
            throw new BannerForbiddenException(id);
        }

        repository.deleteById(id);
    }

    public void deleteBanner(String id) {
        boolean isBannerExist = repository.existsById(id);
        if (!isBannerExist) {
            throw new BannerNotFoundException(id);
        }

        repository.deleteById(id);
    }
}
