package com.orderplatform.product.adapter.in.web.dto;

import com.orderplatform.product.application.port.in.ProductInfo;

import java.time.LocalDateTime;

public record ProductResponse(
        Long id,
        String name,
        long price,
        int stock,
        String category,
        LocalDateTime createdAt
) {
    public static ProductResponse from(ProductInfo info) {
        return new ProductResponse(
                info.id(),
                info.name(),
                info.price(),
                info.stock(),
                info.category(),
                info.createdAt()
        );
    }
}
