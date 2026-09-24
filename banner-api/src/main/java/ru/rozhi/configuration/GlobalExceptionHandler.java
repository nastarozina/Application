package ru.rozhi.configuration;

import org.jspecify.annotations.NonNull;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.rozhi.controller.dto.ApiErrorResponse;
import ru.rozhi.controller.exception.ApiException;
import ru.rozhi.controller.exception.TechnicalException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<@NonNull ApiErrorResponse> handleApiException(ApiException ex) {

        ApiErrorResponse response = new ApiErrorResponse(ex.getCode(), ex.getMessage());

        return ResponseEntity
                .status(ex.getStatus())
                .body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<@NonNull ApiErrorResponse> handleUnexpectedException(Exception ex) {

        return handleApiException(new TechnicalException(ex.getMessage()));
    }
}
