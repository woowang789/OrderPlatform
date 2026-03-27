package com.orderplatform.product.dto;

import com.orderplatform.product.entity.Product;

import java.time.LocalDateTime;

public record ProductResponse(
        Long id,
        String name,
        int price,
        int stock,
        String category,
        LocalDateTime createdAt
) {
    public static ProductResponse from(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getPrice(),
                product.getStock(),
                product.getCategory(),
                product.getCreatedAt()
        );
    }
}
