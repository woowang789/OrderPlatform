package com.orderplatform.order.application.port.in;

import java.util.Objects;

public record OrderItemCommand(
        Long productId,
        String productName,
        long price,
        int quantity
) {
    public OrderItemCommand {
        Objects.requireNonNull(productId, "상품 ID는 필수입니다.");
        Objects.requireNonNull(productName, "상품명은 필수입니다.");
        if (price <= 0) {
            throw new IllegalArgumentException("가격은 0보다 커야 합니다.");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("수량은 1개 이상이어야 합니다.");
        }
    }
}
