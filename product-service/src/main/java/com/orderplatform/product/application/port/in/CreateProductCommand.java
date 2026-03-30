package com.orderplatform.product.application.port.in;

public record CreateProductCommand(
        String name,
        long price,
        int stock,
        String category
) {
}
