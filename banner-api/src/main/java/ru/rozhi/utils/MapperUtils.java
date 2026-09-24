package ru.rozhi.utils;

import ru.rozhi.controller.dto.BannerResponse;
import ru.rozhi.controller.dto.ImageDto;
import ru.rozhi.repository.model.Banner;
import ru.rozhi.repository.model.Image;

public class MapperUtils {
    public static BannerResponse getBannerResponse(Banner banner) {
        return new BannerResponse(
                banner.getId(),
                banner.getName(),
                banner.getDescription(),
                banner.getImages().stream().map(MapperUtils::getImageDto).toList()
        );
    }

    public static ImageDto getImageDto(Image image) {
        return new ImageDto(
                image.getId(),
                image.getObjectKey(),
                image.getContentType(),
                image.getSize(),
                image.getPosition()
        );
    }
}