package com.orderplatform.order.domain.exception;

import com.orderplatform.common.exception.BusinessException;
import com.orderplatform.order.domain.model.OrderStatus;
import org.springframework.http.HttpStatus;

public class InvalidOrderStatusException extends BusinessException {

    public InvalidOrderStatusException(OrderStatus currentStatus) {
        super("현재 상태에서 해당 작업을 수행할 수 없습니다. 현재 상태: " + currentStatus, HttpStatus.BAD_REQUEST);
    }
}
