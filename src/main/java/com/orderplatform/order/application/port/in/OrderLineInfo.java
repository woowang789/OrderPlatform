package com.orderplatform.order.application.port.in;

public record OrderLineInfo(
        Long productId,
        String productName,
        long price,
        int quantity
) {
}
