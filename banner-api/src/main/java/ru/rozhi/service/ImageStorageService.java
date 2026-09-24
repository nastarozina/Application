package ru.rozhi.service;

import ru.rozhi.service.dto.UploadedImageInfo;

public interface ImageStorageService {

    String createUploadUrl(String objectKey);

    UploadedImageInfo getImageInfo(String objectKey);

    void deleteImage(String objectKey);
}
