package com.orderplatform.common.domain.event;

import com.orderplatform.common.domain.event.payload.StockItemPayload;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 재고 차감 완료 이벤트.
 * Product Service가 주문 단위로 원자적 차감 후 발행하며, Order Service가 소비한다.
 * 주문당 단일 이벤트 발행 (멀티 아이템도 하나의 이벤트).
 */
public record StockDeductedEvent(
        UUID eventId,
        UUID orderId,
        List<StockItemPayload> items,
        LocalDateTime occurredAt
) implements DomainEvent {

    /** 편의 생성자 — eventId, occurredAt 자동 생성 */
    public StockDeductedEvent(UUID orderId, List<StockItemPayload> items) {
        this(UUID.randomUUID(), orderId, items, LocalDateTime.now());
    }

    @Override
    public UUID aggregateId() { return orderId; }

    @Override
    public UUID orderId() { return orderId; }
}
