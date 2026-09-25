package ru.rozhi.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.rozhi.controller.dto.UploadUrlRequest;
import ru.rozhi.controller.dto.UploadUrlResponse;
import ru.rozhi.exception.BannerNotFoundException;
import ru.rozhi.exception.ImageNotFoundException;
import ru.rozhi.exception.InvalidImageException;
import ru.rozhi.repository.BannerRepository;
import ru.rozhi.repository.model.Banner;
import ru.rozhi.repository.model.Image;
import ru.rozhi.repository.model.ImageStatus;
import ru.rozhi.service.dto.UploadedImageInfo;

import java.util.UUID;

@Service
public class ImageService {

    @Autowired
    private BannerRepository bannerRepository;

    @Autowired
    private ImageStorageService imageStorageService;

    public UploadUrlResponse createUploadUrl(
            String bannerId,
            UploadUrlRequest request
    ) {

        Banner banner = bannerRepository.findById(bannerId)
                .orElseThrow(() -> new BannerNotFoundException(bannerId));

        String imageId =
                UUID.randomUUID().toString();

        String objectKey = bannerId + "/" + imageId;

        int position = banner.getImages().size();

        Image image = Image.builder()
                .id(imageId)
                .objectKey(objectKey)
                .position(position)
                .contentType(request.contentType())
                .status(ImageStatus.PENDING)
                .build();

        banner.getImages().add(image);

        bannerRepository.save(banner);

        String uploadUrl = imageStorageService.createUploadUrl(objectKey);

        return new UploadUrlResponse(imageId, objectKey, uploadUrl);
    }

    public void completeUpload(
            String bannerId,
            String imageId
    ) {

        Banner banner = bannerRepository.findById(bannerId)
                .orElseThrow(() -> new BannerNotFoundException(bannerId));

        Image image = banner.getImages()
                .stream()
                .filter(i -> i.getId().equals(imageId))
                .findFirst()
                .orElseThrow(() -> new ImageNotFoundException(bannerId, imageId));

        if (image.getStatus() == ImageStatus.UPLOADED) {
            return;
        }

        UploadedImageInfo stat = imageStorageService.getImageInfo(image.getObjectKey());

        if (stat.size() <= 0) {
            throw new InvalidImageException("Uploaded image size is " + stat.size());
        }

        String contentType = stat.contentType();

        if (!isAllowedImage(contentType)) {

            imageStorageService.deleteImage(image.getObjectKey());

            throw new InvalidImageException("Uploaded image content type is not allowed: " + contentType);
        }

        image.setStatus(ImageStatus.UPLOADED);
        image.setSize(stat.size());
        image.setContentType(contentType);

        bannerRepository.save(banner);
    }

    private Boolean isAllowedImage(String contentType) {
        return true;
    }
}
