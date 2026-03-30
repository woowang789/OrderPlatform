package com.orderplatform.payment.application.port.in;

import java.util.Objects;
import java.util.UUID;

public record CancelPaymentCommand(
        Long memberId,
        UUID paymentId
) {
    public CancelPaymentCommand {
        Objects.requireNonNull(memberId, "회원 ID는 필수입니다.");
        Objects.requireNonNull(paymentId, "결제 ID는 필수입니다.");
    }
}
