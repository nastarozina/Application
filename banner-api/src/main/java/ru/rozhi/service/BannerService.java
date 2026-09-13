package ru.rozhi.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.rozhi.controller.dto.BannerRequest;
import ru.rozhi.controller.dto.BannerResponse;
import ru.rozhi.repository.BannerRepository;
import ru.rozhi.repository.model.Banner;
import ru.rozhi.utils.MapperUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class BannerService {

    @Autowired
    private BannerRepository repository;

    public BannerResponse getBannerById(String id) {
        Banner banner = repository.findById(id).orElse(null);
        if (banner == null) return null;

        return MapperUtils.getBannerResponse(banner);
    }

    public List<BannerResponse> getAllBanners() {
        List<Banner> banners = repository.findAll();
        return banners.stream().map(MapperUtils::getBannerResponse).collect(Collectors.toList());
    }

    public BannerResponse createBanner(BannerRequest banner) {
        Banner createdBanner =  repository.save(Banner.builder()
                .name(banner.name())
                .description(banner.description())
                .build());

        return MapperUtils.getBannerResponse(createdBanner);
    }
}
