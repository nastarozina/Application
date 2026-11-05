package ru.rozhi.exception;

import org.jspecify.annotations.NonNull;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.rozhi.ApiErrorResponse;

@RestControllerAdvice
public class RequestExceptionHandler {

    @ExceptionHandler({ServletRequestBindingException.class})
    public ResponseEntity<@NonNull ApiErrorResponse> handleRequestException(ServletRequestBindingException ex) {
        ApiException exception = new InvalidRequestException(ex.getMessage());
        return handleApiException(exception);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class})
    public ResponseEntity<@NonNull ApiErrorResponse> handleRequestBodyException(HttpMessageNotReadableException ex) {
        ApiException exception = new InvalidRequestException(ex.getMessage());
        return handleApiException(exception);
    }

    private ResponseEntity<@NonNull ApiErrorResponse> handleApiException(ApiException ex) {
        ApiErrorResponse response = new ApiErrorResponse(ex.getCode(), ex.getMessage());
        return ResponseEntity.status(ex.getStatus()).body(response);
    }
}
