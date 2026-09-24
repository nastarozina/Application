package ru.rozhi.controller.exception;

import org.springframework.http.HttpStatus;

public class TechnicalException extends ApiException {

    public TechnicalException(String message) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", message);
    }
}
