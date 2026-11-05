package ru.rozhi.exception;

public class InvalidRequestException extends BadRequestException {

    protected InvalidRequestException(String message) {
        super("INVALID_REQUEST", message);
    }
}
