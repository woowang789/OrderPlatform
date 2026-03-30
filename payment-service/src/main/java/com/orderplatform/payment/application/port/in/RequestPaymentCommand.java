package com.orderplatform.payment.application.port.in;

import java.util.Objects;
import java.util.UUID;

public record RequestPaymentCommand(
        Long memberId,
        UUID orderId,
        String method
) {
    public RequestPaymentCommand {
        Objects.requireNonNull(memberId, "회원 ID는 필수입니다.");
        Objects.requireNonNull(orderId, "주문 ID는 필수입니다.");
        Objects.requireNonNull(method, "결제 수단은 필수입니다.");
    }
}
