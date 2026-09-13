package ru.rozhi.utils;

import ru.rozhi.controller.dto.BannerResponse;
import ru.rozhi.repository.model.Banner;

public class MapperUtils {
    public static BannerResponse getBannerResponse(Banner banner) {
        return new BannerResponse(
                banner.getId(),
                banner.getName(),
                banner.getDescription()
        );
    }
}