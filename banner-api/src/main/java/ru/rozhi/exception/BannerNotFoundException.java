package ru.rozhi.exception;

public class BannerNotFoundException extends ResourceNotFoundException {

    public BannerNotFoundException(String bannerId) {
        super("BANNER_NOT_FOUND", "Banner not found: " + bannerId);
    }
}
