package com.orderplatform.common.exception;

import org.springframework.http.HttpStatus;

import java.util.UUID;

public class DuplicatePaymentException extends BusinessException {

    public DuplicatePaymentException(UUID orderId) {
        super("해당 주문에 이미 결제가 존재합니다. 주문 ID: " + orderId, HttpStatus.CONFLICT);
    }
}
