package com.orderplatform.product.application.port.in;

import java.time.LocalDateTime;

public record ProductInfo(
        Long id,
        String name,
        long price,
        int stock,
        String category,
        LocalDateTime createdAt
) {
}
