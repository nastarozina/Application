package ru.rozhi.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
            @RequestBody UploadUrlRequest request
    ) {
        return imageService.createUploadUrl(bannerId, request);
    }

    @PostMapping("/{bannerId}/images/{imageId}/complete")
    public void complete(
            @PathVariable String bannerId,
            @PathVariable String imageId
    ) {
        imageService.completeUpload(bannerId, imageId);
    }
}
