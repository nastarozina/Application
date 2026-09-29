package ru.rozhi.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.rozhi.client.CategoryClient;
import ru.rozhi.controller.dto.BannerRequest;
import ru.rozhi.controller.dto.BannerResponse;
import ru.rozhi.controller.dto.CategoryResponse;
import ru.rozhi.exception.BannerNotFoundException;
import ru.rozhi.repository.BannerRepository;
import ru.rozhi.repository.model.Banner;
import ru.rozhi.utils.MapperUtils;

import java.util.List;
import java.util.Map;

@Service
public class BannerService {

    @Autowired
    private BannerRepository repository;

    @Autowired
    private CategoryClient categoryClient;

    public BannerResponse getBannerById(String id) {
        Banner banner = repository.findById(id)
                .orElseThrow(() -> new BannerNotFoundException(id));
        CategoryResponse category = categoryClient.getCategory(banner.getCategoryId());
        return MapperUtils.getBannerResponse(banner, category.name());
    }

    public List<BannerResponse> getAllBanners() {
        List<Banner> banners = repository.findAll();

        Map<String, String> categoriesNames = categoryClient
                .getCategoriesNames(
                        banners.stream()
                                .map(Banner::getCategoryId)
                                .toList()
                );

        return banners.stream()
                .map(banner -> MapperUtils.getBannerResponse(
                        banner,
                        categoriesNames.get(banner.getCategoryId())
                ))
                .toList();
    }

    public BannerResponse createBanner(BannerRequest banner) {
        CategoryResponse category = categoryClient.getCategory(banner.categoryId());
        Banner createdBanner = repository.save(Banner.builder()
                .name(banner.name())
                .description(banner.description())
                .categoryId(banner.categoryId())
                .build());
        return MapperUtils.getBannerResponse(createdBanner, category.name());
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

        CategoryResponse category = categoryClient.getCategory(banner.getCategoryId());

        banner = repository.save(banner);

        return MapperUtils.getBannerResponse(banner, category.name());
    }

    public void deleteBanner(String id) {
        boolean isBannerExist = repository.existsById(id);
        if (!isBannerExist) {
            throw new BannerNotFoundException(id);
        }

        repository.deleteById(id);
    }
}
