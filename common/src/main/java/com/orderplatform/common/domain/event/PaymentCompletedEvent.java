package com.orderplatform.common.domain.event;

import com.orderplatform.common.domain.event.payload.StockItemPayload;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 결제 완료 이벤트.
 * Payment Service가 발행하며, Product Service(재고 차감)와 Order Service(상태 전이)가 소비한다.
 * items 필드는 Product Service가 재고 차감에 사용한다.
 */
public record PaymentCompletedEvent(
        UUID eventId,
        UUID paymentId,
        UUID orderId,
        Long memberId,
        long amount,
        List<StockItemPayload> items,
        LocalDateTime occurredAt
) implements DomainEvent {

    /** 편의 생성자 — eventId, occurredAt 자동 생성 */
    public PaymentCompletedEvent(UUID paymentId, UUID orderId, Long memberId,
                                  long amount, List<StockItemPayload> items) {
        this(UUID.randomUUID(), paymentId, orderId, memberId, amount, items, LocalDateTime.now());
    }

    @Override
    public UUID aggregateId() { return paymentId; }

    @Override
    public UUID orderId() { return orderId; }
}
