package com.orderplatform.order.application.port.out;

/**
 * 재고 차감 Outbound Port — 구현체는 Product 컨텍스트의 Adapter가 담당
 */
public interface DecreaseStockPort {

    StockInfo decreaseStock(Long productId, int quantity);
}
