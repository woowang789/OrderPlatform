package com.orderplatform.order.application.port.out;

/**
 * 재고 차감 시 반환되는 상품 정보 — OrderLine 스냅샷 생성용
 */
public record StockInfo(
        String productName,
        long price
) {
}
