package com.orderplatform.payment.dto;

import com.orderplatform.payment.entity.Payment;

import java.time.LocalDateTime;
import java.util.UUID;

public record PaymentResponse(
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
    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(
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
