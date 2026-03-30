package com.orderplatform.payment.domain.event;

import com.orderplatform.common.domain.event.DomainEvent;

import java.time.LocalDateTime;
import java.util.UUID;

public record PaymentFailedEvent(
        UUID paymentId,
        UUID orderId,
        String reason,
        LocalDateTime occurredAt
) implements DomainEvent {

    public PaymentFailedEvent(UUID paymentId, UUID orderId, String reason) {
        this(paymentId, orderId, reason, LocalDateTime.now());
    }
}
