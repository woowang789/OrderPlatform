package com.orderplatform.payment.application.port.in;

import java.util.UUID;

public interface CancelPaymentUseCase {

    PaymentInfo cancelPayment(CancelPaymentCommand command);

    /**
     * orderId 기반 결제 취소 — 이벤트 기반 Saga 보상 시 사용
     */
    void cancelPaymentByOrderId(UUID orderId);
}
