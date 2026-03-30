package com.orderplatform.payment.application.port.in;

public interface CancelPaymentUseCase {

    PaymentInfo cancelPayment(CancelPaymentCommand command);
}
