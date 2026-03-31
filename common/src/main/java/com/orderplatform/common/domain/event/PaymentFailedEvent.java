package com.orderplatform.common.domain.event;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 결제 실패 이벤트.
 * Payment Service가 발행하며, Order Service와 Notification Service가 소비한다.
 */
public record PaymentFailedEvent(
        UUID eventId,
        UUID paymentId,
        UUID orderId,
        String reason,
        LocalDateTime occurredAt
) implements DomainEvent {

    /** 편의 생성자 — eventId, occurredAt 자동 생성 */
    public PaymentFailedEvent(UUID paymentId, UUID orderId, String reason) {
        this(UUID.randomUUID(), paymentId, orderId, reason, LocalDateTime.now());
    }

    @Override
    public UUID aggregateId() { return paymentId; }

    @Override
    public UUID orderId() { return orderId; }
}
