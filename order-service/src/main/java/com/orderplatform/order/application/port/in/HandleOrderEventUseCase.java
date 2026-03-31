package com.orderplatform.order.application.port.in;

import java.util.UUID;

/**
 * 이벤트 기반 주문 상태 전이 UseCase
 */
public interface HandleOrderEventUseCase {

    void handlePaymentCompleted(UUID orderId);

    void handlePaymentFailed(UUID orderId);

    void handleStockDeducted(UUID orderId);

    void handleStockDeductionFailed(UUID orderId);

    void handlePaymentCancelled(UUID orderId);
}
