package com.orderplatform.order.application.port.in;

import com.orderplatform.order.domain.model.Order;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record OrderInfo(
        UUID id,
        Long memberId,
        String status,
        long totalAmount,
        List<OrderLineInfo> orderLines,
        LocalDateTime createdAt
) {
    public static OrderInfo from(Order order) {
        List<OrderLineInfo> lines = order.getOrderLines().stream()
                .map(line -> new OrderLineInfo(
                        line.productId(),
                        line.productName(),
                        line.price(),
                        line.quantity()
                ))
                .toList();

        return new OrderInfo(
                order.getId(),
                order.getMemberId(),
                order.getStatus().name(),
                order.getTotalAmount(),
                lines,
                order.getCreatedAt()
        );
    }
}
