package com.orderplatform.order.adapter.in.web.dto;

import com.orderplatform.order.application.port.in.OrderLineInfo;

public record OrderLineResponse(
        Long productId,
        String productName,
        long price,
        int quantity
) {
    public static OrderLineResponse from(OrderLineInfo info) {
        return new OrderLineResponse(
                info.productId(),
                info.productName(),
                info.price(),
                info.quantity()
        );
    }
}
