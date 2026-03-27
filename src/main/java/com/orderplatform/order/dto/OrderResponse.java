package com.orderplatform.order.dto;

import com.orderplatform.order.entity.Order;

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
    public static OrderResponse from(Order order) {
        List<OrderLineResponse> lines = order.getOrderLines().stream()
                .map(OrderLineResponse::from)
                .toList();

        return new OrderResponse(
                order.getId(),
                order.getMemberId(),
                order.getStatus().name(),
                order.getTotalAmount(),
                lines,
                order.getCreatedAt()
        );
    }
}
