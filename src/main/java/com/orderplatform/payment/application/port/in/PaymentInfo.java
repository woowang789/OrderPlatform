package com.orderplatform.payment.application.port.in;

import com.orderplatform.payment.domain.model.Payment;

import java.time.LocalDateTime;
import java.util.UUID;

public record PaymentInfo(
        UUID id,
        UUID orderId,
        Long memberId,
        long amount,
        String status,
        String method,
        String pgTxnId,
        String failReason,
        LocalDateTime createdAt
) {
    public static PaymentInfo from(Payment payment) {
        return new PaymentInfo(
                payment.getId(),
                payment.getOrderId(),
                payment.getMemberId(),
                payment.getAmount(),
                payment.getStatus().name(),
                payment.getMethod().name(),
                payment.getPgTxnId(),
                payment.getFailReason(),
                payment.getCreatedAt()
        );
    }
}
