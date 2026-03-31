package com.orderplatform.common.domain.event;

import com.orderplatform.common.domain.event.payload.StockFailureItemPayload;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 재고 차감 실패 이벤트.
 * Product Service가 재고 부족 시 발행하며, Order Service가 소비한다.
 * 하나라도 재고 부족 시 전체 차감 롤백 후 이 이벤트를 발행한다.
 */
public record StockDeductionFailedEvent(
        UUID eventId,
        UUID orderId,
        List<StockFailureItemPayload> items,
        String reason,
        LocalDateTime occurredAt
) implements DomainEvent {

    /** 편의 생성자 — eventId, occurredAt 자동 생성 */
    public StockDeductionFailedEvent(UUID orderId, List<StockFailureItemPayload> items, String reason) {
        this(UUID.randomUUID(), orderId, items, reason, LocalDateTime.now());
    }

    @Override
    public UUID aggregateId() { return orderId; }

    @Override
    public UUID orderId() { return orderId; }
}
