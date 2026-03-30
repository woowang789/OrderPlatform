package com.orderplatform.payment.domain.event;

import com.orderplatform.common.domain.event.DomainEvent;

import java.time.LocalDateTime;
import java.util.UUID;

public record PaymentCompletedEvent(
        UUID paymentId,
        UUID orderId,
        long amount,
        String pgTxnId,
        LocalDateTime occurredAt
) implements DomainEvent {

    public PaymentCompletedEvent(UUID paymentId, UUID orderId, long amount, String pgTxnId) {
        this(paymentId, orderId, amount, pgTxnId, LocalDateTime.now());
    }
}
