package com.orderplatform.order.application.port.in;

import java.util.Objects;

public record OrderItemCommand(
        Long productId,
        int quantity
) {
    public OrderItemCommand {
        Objects.requireNonNull(productId, "상품 ID는 필수입니다.");
        if (quantity <= 0) {
            throw new IllegalArgumentException("수량은 1개 이상이어야 합니다.");
        }
    }
}
