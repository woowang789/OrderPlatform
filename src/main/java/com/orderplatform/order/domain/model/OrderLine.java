package com.orderplatform.order.domain.model;

/**
 * 주문 항목 Value Object — 주문 시점의 상품명/가격 스냅샷
 */
public record OrderLine(
        Long productId,
        String productName,
        long price,
        int quantity
) {
}
