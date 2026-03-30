package com.orderplatform.order.domain.event;

import com.orderplatform.common.domain.event.DomainEvent;

import java.time.LocalDateTime;
import java.util.UUID;

public record OrderCancelledEvent(
        UUID orderId,
        Long memberId,
        LocalDateTime occurredAt
) implements DomainEvent {

    public OrderCancelledEvent(UUID orderId, Long memberId) {
        this(orderId, memberId, LocalDateTime.now());
    }
}
