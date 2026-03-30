package com.orderplatform.order.domain.event;

import com.orderplatform.common.domain.event.DomainEvent;

import java.time.LocalDateTime;
import java.util.UUID;

public record OrderPlacedEvent(
        UUID orderId,
        Long memberId,
        long totalAmount,
        LocalDateTime occurredAt
) implements DomainEvent {

    public OrderPlacedEvent(UUID orderId, Long memberId, long totalAmount) {
        this(orderId, memberId, totalAmount, LocalDateTime.now());
    }
}
