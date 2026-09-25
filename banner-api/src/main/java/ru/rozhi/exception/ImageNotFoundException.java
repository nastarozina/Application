package ru.rozhi.exception;

public class ImageNotFoundException extends ResourceNotFoundException {

    public ImageNotFoundException(String bannerId, String imageId) {
        super("IMAGE_NOT_FOUND", "Image " + imageId + " not found for banner " + bannerId);
    }
}
