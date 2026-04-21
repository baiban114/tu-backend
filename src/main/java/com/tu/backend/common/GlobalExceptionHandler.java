package com.tu.backend.common;

import jakarta.validation.ConstraintViolationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ApiResponse<Void> handleBusinessException(BusinessException ex) {
        return ApiResponse.failure(ex.getCode(), ex.getMessage());
    }

    @ExceptionHandler({
        MethodArgumentNotValidException.class,
        ConstraintViolationException.class,
        HttpMessageNotReadableException.class
    })
    public ApiResponse<Void> handleBadRequest(Exception ex) {
        return ApiResponse.failure(40000, "request parameter error");
    }

    @ExceptionHandler(Exception.class)
    public ApiResponse<Void> handleException(Exception ex) {
        return ApiResponse.failure(50000, "internal server error");
    }
}

