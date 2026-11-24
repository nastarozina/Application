package ru.rozhi.exception;

public class InvalidBannerException extends BadRequestException {

    public InvalidBannerException(String message) {
        super("INVALID_BANNER", message);
    }
}