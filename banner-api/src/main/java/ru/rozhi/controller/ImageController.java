package ru.rozhi.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import ru.rozhi.controller.dto.UploadUrlRequest;
import ru.rozhi.controller.dto.UploadUrlResponse;
import ru.rozhi.service.ImageService;

@RestController
public class ImageController {

    @Autowired
    private ImageService imageService;

    @PostMapping("/{bannerId}/images/upload-url")
    public UploadUrlResponse createUploadUrl(
            @PathVariable String bannerId,
            @RequestBody UploadUrlRequest request,
            @RequestHeader("X-User-Id") String userId
    ) {
        return imageService.createUploadUrl(bannerId, request, userId);
    }

    @PostMapping("/{bannerId}/images/{imageId}/complete")
    public void complete(
            @PathVariable String bannerId,
            @PathVariable String imageId
    ) {
        imageService.completeUpload(bannerId, imageId);
    }

    @DeleteMapping("/{bannerId}/images/{imageId}")
    public void deleteBannerImage(
            @PathVariable String bannerId,
            @PathVariable String imageId,
            @RequestHeader("X-User-Id") String userId
    ) {
        imageService.deleteImage(bannerId, imageId, userId);
    }
}