package ru.rozhi.utils;

import ru.rozhi.controller.dto.BannerResponse;
import ru.rozhi.controller.dto.CategoryResponse;
import ru.rozhi.controller.dto.ImageDto;
import ru.rozhi.repository.model.Banner;
import ru.rozhi.repository.model.Image;

import java.util.List;

public class MapperUtils {
    public static BannerResponse getBannerResponse(Banner banner, List<CategoryResponse> categories) {
        return new BannerResponse(
                banner.getId(),
                banner.getName(),
                banner.getDescription(),
                categories,
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