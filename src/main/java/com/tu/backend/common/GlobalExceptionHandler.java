package com.tu.backend.common;

import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ApiResponse<Void> handleBusinessException(BusinessException ex) {
        log.warn("business exception: code={}, message={}", ex.getCode(), ex.getMessage(), ex);
        return ApiResponse.failure(ex.getCode(), ex.getMessage());
    }

    @ExceptionHandler({
        MethodArgumentNotValidException.class,
        ConstraintViolationException.class,
        HttpMessageNotReadableException.class
    })
    public ApiResponse<Void> handleBadRequest(Exception ex) {
        log.warn("bad request exception: {}", ex.getMessage(), ex);
        return ApiResponse.failure(40000, "request parameter error");
    }

    @ExceptionHandler(Exception.class)
    public ApiResponse<Void> handleException(Exception ex) {
        log.error("unhandled exception", ex);
        return ApiResponse.failure(50000, "internal server error");
    }
}
