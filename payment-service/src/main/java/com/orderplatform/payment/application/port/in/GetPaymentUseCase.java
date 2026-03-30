package com.orderplatform.payment.application.port.in;

import java.util.UUID;

public interface GetPaymentUseCase {

    PaymentInfo getPayment(Long memberId, UUID paymentId);
}
