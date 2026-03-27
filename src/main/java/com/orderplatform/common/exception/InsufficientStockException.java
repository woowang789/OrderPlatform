package com.orderplatform.common.exception;

import org.springframework.http.HttpStatus;

public class InsufficientStockException extends BusinessException {

    public InsufficientStockException(int currentStock, int requestedQuantity) {
        super("재고가 부족합니다. 현재 재고: " + currentStock + ", 요청 수량: " + requestedQuantity,
                HttpStatus.BAD_REQUEST);
    }
}
