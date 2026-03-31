package com.orderplatform.common.domain.event.payload;

/**
 * 재고 차감 실패 아이템 페이로드.
 * StockDeductionFailedEvent에서 실패 상세 정보를 전달할 때 사용.
 */
public record StockFailureItemPayload(
        Long productId,
        int requestedQuantity,
        int availableStock
) {
}
