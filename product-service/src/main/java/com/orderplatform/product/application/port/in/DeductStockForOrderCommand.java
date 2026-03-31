package com.orderplatform.product.application.port.in;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * 주문 단위 재고 차감 커맨드
 */
public record DeductStockForOrderCommand(
        UUID orderId,
        List<DeductItem> items
) {
    public DeductStockForOrderCommand {
        Objects.requireNonNull(orderId, "주문 ID는 필수입니다.");
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("차감 항목은 최소 1개 이상이어야 합니다.");
        }
    }

    public record DeductItem(Long productId, int quantity) {
        public DeductItem {
            Objects.requireNonNull(productId, "상품 ID는 필수입니다.");
            if (quantity <= 0) {
                throw new IllegalArgumentException("차감 수량은 양수여야 합니다.");
            }
        }
    }
}
