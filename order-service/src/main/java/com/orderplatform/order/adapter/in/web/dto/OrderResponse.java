package com.orderplatform.order.adapter.in.web.dto;

import com.orderplatform.order.application.port.in.OrderInfo;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        Long memberId,
        String status,
        long totalAmount,
        List<OrderLineResponse> orderLines,
        LocalDateTime createdAt
) {
    public static OrderResponse from(OrderInfo info) {
        List<OrderLineResponse> lines = info.orderLines().stream()
                .map(OrderLineResponse::from)
                .toList();

        return new OrderResponse(
                info.id(),
                info.memberId(),
                info.status(),
                info.totalAmount(),
                lines,
                info.createdAt()
        );
    }
}
