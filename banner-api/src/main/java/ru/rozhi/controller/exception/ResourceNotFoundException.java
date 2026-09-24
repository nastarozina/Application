package ru.rozhi.controller.exception;

import org.springframework.http.HttpStatus;

public abstract class ResourceNotFoundException extends ApiException {

    protected ResourceNotFoundException(String code, String message) {
        super(HttpStatus.NOT_FOUND, code, message);
    }
}
