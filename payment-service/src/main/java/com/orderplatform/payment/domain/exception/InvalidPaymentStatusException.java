package com.orderplatform.payment.domain.exception;

import com.orderplatform.common.exception.BusinessException;
import com.orderplatform.payment.domain.model.PaymentStatus;
import org.springframework.http.HttpStatus;

public class InvalidPaymentStatusException extends BusinessException {

    public InvalidPaymentStatusException(PaymentStatus currentStatus) {
        super("현재 상태에서 해당 작업을 수행할 수 없습니다. 현재 상태: " + currentStatus, HttpStatus.BAD_REQUEST);
    }
}
