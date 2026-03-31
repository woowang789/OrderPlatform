package com.orderplatform.common.domain.event;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 주문 확정 이벤트.
 * Order Service가 stock.deducted 수신 후 발행하며, Notification Service가 소비한다.
 */
public record OrderConfirmedEvent(
        UUID eventId,
        UUID orderId,
        LocalDateTime occurredAt
) implements DomainEvent {

    /** 편의 생성자 — eventId, occurredAt 자동 생성 */
    public OrderConfirmedEvent(UUID orderId) {
        this(UUID.randomUUID(), orderId, LocalDateTime.now());
    }

    @Override
    public UUID aggregateId() { return orderId; }

    @Override
    public UUID orderId() { return orderId; }
}
