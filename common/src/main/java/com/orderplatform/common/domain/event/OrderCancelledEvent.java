package com.orderplatform.common.domain.event;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 주문 취소 이벤트.
 * Order Service가 발행하며, Notification Service가 소비한다.
 */
public record OrderCancelledEvent(
        UUID eventId,
        UUID orderId,
        String reason,
        LocalDateTime occurredAt
) implements DomainEvent {

    /** 편의 생성자 — eventId, occurredAt 자동 생성 */
    public OrderCancelledEvent(UUID orderId, String reason) {
        this(UUID.randomUUID(), orderId, reason, LocalDateTime.now());
    }

    @Override
    public UUID aggregateId() { return orderId; }

    @Override
    public UUID orderId() { return orderId; }
}
