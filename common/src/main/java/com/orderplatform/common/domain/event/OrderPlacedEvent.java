package com.orderplatform.common.domain.event;

import com.orderplatform.common.domain.event.payload.OrderItemPayload;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 주문 생성 이벤트.
 * Order Service가 발행하며, Payment Service와 Notification Service가 소비한다.
 */
public record OrderPlacedEvent(
        UUID eventId,
        UUID orderId,
        Long memberId,
        List<OrderItemPayload> items,
        long totalAmount,
        String paymentMethod,
        LocalDateTime occurredAt
) implements DomainEvent {

    /** 편의 생성자 — eventId, occurredAt 자동 생성 */
    public OrderPlacedEvent(UUID orderId, Long memberId, List<OrderItemPayload> items,
                            long totalAmount, String paymentMethod) {
        this(UUID.randomUUID(), orderId, memberId, items, totalAmount, paymentMethod, LocalDateTime.now());
    }

    @Override
    public UUID aggregateId() { return orderId; }

    @Override
    public UUID orderId() { return orderId; }
}
