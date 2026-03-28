package com.orderplatform.order.dto;

import com.orderplatform.order.entity.OrderLine;

public record OrderLineResponse(
        Long productId,
        String productName,
        long price,
        int quantity
) {
    public static OrderLineResponse from(OrderLine orderLine) {
        return new OrderLineResponse(
                orderLine.getProductId(),
                orderLine.getProductName(),
                orderLine.getPrice(),
                orderLine.getQuantity()
        );
    }
}
