package com.orderplatform.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;

@Getter
public class BusinessException extends RuntimeException {

    @NonNull
    private final HttpStatus status;

    public BusinessException(String message, @NonNull HttpStatus status) {
        super(message);
        this.status = status;
    }
}
