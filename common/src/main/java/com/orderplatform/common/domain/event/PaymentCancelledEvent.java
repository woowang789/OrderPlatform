package com.orderplatform.common.domain.event;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 결제 취소 완료 이벤트.
 * Payment Service가 payment.cancel.requested 수신 후 발행하며,
 * Order Service(주문 취소)와 Notification Service가 소비한다.
 */
public record PaymentCancelledEvent(
        UUID eventId,
        UUID paymentId,
        UUID orderId,
        LocalDateTime occurredAt
) implements DomainEvent {

    /** 편의 생성자 — eventId, occurredAt 자동 생성 */
    public PaymentCancelledEvent(UUID paymentId, UUID orderId) {
        this(UUID.randomUUID(), paymentId, orderId, LocalDateTime.now());
    }

    @Override
    public UUID aggregateId() { return paymentId; }

    @Override
    public UUID orderId() { return orderId; }
}
