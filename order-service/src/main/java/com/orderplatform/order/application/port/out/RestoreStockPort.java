package com.orderplatform.order.application.port.out;

/**
 * 재고 복원 Outbound Port — 주문 취소 시 사용
 */
public interface RestoreStockPort {

    void restoreStock(Long productId, int quantity);
}
