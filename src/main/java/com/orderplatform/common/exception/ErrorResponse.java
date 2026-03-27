package com.orderplatform.common.exception;

import java.util.List;

public record ErrorResponse(
        int status,
        String message,
        List<FieldError> errors
) {
    public record FieldError(
            String field,
            String message
    ) {
    }

    public static ErrorResponse of(int status, String message) {
        return new ErrorResponse(status, message, List.of());
    }

    public static ErrorResponse of(int status, String message, List<FieldError> errors) {
        return new ErrorResponse(status, message, errors);
    }
}
