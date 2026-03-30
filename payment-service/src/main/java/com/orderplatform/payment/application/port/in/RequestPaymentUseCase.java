package com.orderplatform.payment.application.port.in;

public interface RequestPaymentUseCase {

    PaymentInfo requestPayment(RequestPaymentCommand command);
}
