package com.orderplatform.common.exception;

import com.orderplatform.order.entity.OrderStatus;
import org.springframework.http.HttpStatus;

public class InvalidOrderStatusException extends BusinessException {

    public InvalidOrderStatusException(OrderStatus currentStatus) {
        super("현재 상태에서 해당 작업을 수행할 수 없습니다. 현재 상태: " + currentStatus, HttpStatus.BAD_REQUEST);
    }
}
