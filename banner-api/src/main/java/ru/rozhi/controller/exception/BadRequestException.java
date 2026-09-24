package ru.rozhi.controller.exception;

import org.springframework.http.HttpStatus;

public abstract class BadRequestException extends ApiException {

    protected BadRequestException(String code, String message) {
        super(HttpStatus.BAD_REQUEST, code, message);
    }
}
