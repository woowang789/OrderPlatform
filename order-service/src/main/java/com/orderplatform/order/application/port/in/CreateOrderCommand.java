package com.orderplatform.order.application.port.in;

import java.util.List;
import java.util.Objects;

public record CreateOrderCommand(
        Long memberId,
        List<OrderItemCommand> items
) {
    public CreateOrderCommand {
        Objects.requireNonNull(memberId, "회원 ID는 필수입니다.");
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("주문 항목은 최소 1개 이상이어야 합니다.");
        }
    }
}
