package com.orderplatform.order.application.port.in;

import java.util.Objects;
import java.util.UUID;

public record CancelOrderCommand(
        UUID orderId,
        Long memberId
) {
    public CancelOrderCommand {
        Objects.requireNonNull(orderId, "주문 ID는 필수입니다.");
        Objects.requireNonNull(memberId, "회원 ID는 필수입니다.");
    }
}
