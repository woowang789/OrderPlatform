package com.orderplatform.common.domain.event.payload;

/**
 * 재고 아이템 페이로드.
 * PaymentCompletedEvent, StockDeductedEvent에서 재고 차감 대상 정보를 전달할 때 사용.
 */
public record StockItemPayload(
        Long productId,
        int quantity
) {
}
