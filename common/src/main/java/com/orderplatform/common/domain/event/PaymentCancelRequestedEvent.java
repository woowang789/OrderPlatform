package com.orderplatform.common.domain.event;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 결제 취소 요청 이벤트.
 * Order Service가 stock.deduction.failed 수신 후 발행하며, Payment Service가 소비한다.
 * Saga 보상 트랜잭션의 시작점.
 */
public record PaymentCancelRequestedEvent(
        UUID eventId,
        UUID orderId,
        UUID paymentId,
        String reason,
        LocalDateTime occurredAt
) implements DomainEvent {

    /** 편의 생성자 — eventId, occurredAt 자동 생성 */
    public PaymentCancelRequestedEvent(UUID orderId, UUID paymentId, String reason) {
        this(UUID.randomUUID(), orderId, paymentId, reason, LocalDateTime.now());
    }

    @Override
    public UUID aggregateId() { return orderId; }

    @Override
    public UUID orderId() { return orderId; }
}
