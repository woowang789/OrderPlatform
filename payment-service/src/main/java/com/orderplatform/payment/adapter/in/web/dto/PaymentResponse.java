package com.orderplatform.payment.adapter.in.web.dto;

import com.orderplatform.payment.application.port.in.PaymentInfo;

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
    public static PaymentResponse from(PaymentInfo info) {
        return new PaymentResponse(
                info.id(),
                info.orderId(),
                info.memberId(),
                info.amount(),
                info.status(),
                info.method(),
                info.pgTxnId(),
                info.failReason(),
                info.createdAt()
        );
    }
}
