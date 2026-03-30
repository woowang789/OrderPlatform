package com.orderplatform.order.domain.exception;

import com.orderplatform.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

import java.util.UUID;

public class OrderNotFoundException extends BusinessException {

    public OrderNotFoundException(UUID orderId) {
        super("주문을 찾을 수 없습니다. ID: " + orderId, HttpStatus.NOT_FOUND);
    }
}
