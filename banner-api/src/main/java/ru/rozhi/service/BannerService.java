package ru.rozhi.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.rozhi.controller.dto.BannerResponse;
import ru.rozhi.repository.BannerRepository;
import ru.rozhi.repository.model.Banner;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class BannerService {

    @Autowired
    private BannerRepository repository;

    public BannerResponse getBannerById(String id) {
        Banner banner = repository.findById(id).orElse(null);
        if (banner == null) return null;

        return new BannerResponse(
                banner.getName(),
                banner.getDescription()
        );
    }

    public List<BannerResponse> getAllBanners() {
        List<Banner> banners = repository.findAll();
        return banners.stream().map(banner -> new BannerResponse(banner.getName(), banner.getDescription()))
                .collect(Collectors.toList());
    }
}
