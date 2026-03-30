package com.orderplatform.payment.domain.event;

import com.orderplatform.common.domain.event.DomainEvent;

import java.time.LocalDateTime;
import java.util.UUID;

public record PaymentCancelledEvent(
        UUID paymentId,
        UUID orderId,
        LocalDateTime occurredAt
) implements DomainEvent {

    public PaymentCancelledEvent(UUID paymentId, UUID orderId) {
        this(paymentId, orderId, LocalDateTime.now());
    }
}
