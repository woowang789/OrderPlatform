package com.orderplatform.common.domain.event.payload;

/**
 * 주문 아이템 페이로드.
 * OrderPlacedEvent에서 주문 항목 정보를 전달할 때 사용.
 */
public record OrderItemPayload(
        Long productId,
        String productName,
        long price,
        int quantity
) {
}
