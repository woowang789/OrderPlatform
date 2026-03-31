package com.orderplatform.product.application.port.in;

/**
 * 주문 단위 원자적 재고 차감 UseCase
 */
public interface DeductStockForOrderUseCase {

    void deductStockForOrder(DeductStockForOrderCommand command);
}
