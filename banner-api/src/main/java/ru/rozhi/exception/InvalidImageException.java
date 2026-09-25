package ru.rozhi.exception;

public class InvalidImageException extends BadRequestException {

    public InvalidImageException(String message) {
        super("INVALID_IMAGE", message);
    }
}