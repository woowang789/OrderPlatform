package com.orderplatform.payment.application.port.in;

import com.orderplatform.common.domain.event.payload.StockItemPayload;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record RequestPaymentCommand(
        Long memberId,
        UUID orderId,
        long totalAmount,
        String method,
        List<StockItemPayload> items
) {
    public RequestPaymentCommand {
        Objects.requireNonNull(memberId, "회원 ID는 필수입니다.");
        Objects.requireNonNull(orderId, "주문 ID는 필수입니다.");
        Objects.requireNonNull(method, "결제 수단은 필수입니다.");
        Objects.requireNonNull(items, "주문 항목은 필수입니다.");
    }
}
