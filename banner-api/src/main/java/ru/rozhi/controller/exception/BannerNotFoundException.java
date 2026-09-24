package ru.rozhi.controller.exception;

public class BannerNotFoundException extends ResourceNotFoundException {

    public BannerNotFoundException(String bannerId) {
        super("BANNER_NOT_FOUND", "Banner not found: " + bannerId);
    }
}
