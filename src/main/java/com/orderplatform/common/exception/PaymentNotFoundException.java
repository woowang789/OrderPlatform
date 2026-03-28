package com.orderplatform.common.exception;

import org.springframework.http.HttpStatus;

import java.util.UUID;

public class PaymentNotFoundException extends BusinessException {

    public PaymentNotFoundException(UUID paymentId) {
        super("결제를 찾을 수 없습니다. ID: " + paymentId, HttpStatus.NOT_FOUND);
    }
}
