package com.orderplatform.common.exception;

import org.springframework.http.HttpStatus;

public class ProductNotFoundException extends BusinessException {

    public ProductNotFoundException(Long productId) {
        super("상품을 찾을 수 없습니다. ID: " + productId, HttpStatus.NOT_FOUND);
    }
}
