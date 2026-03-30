package com.orderplatform.payment.application.port.out;

public interface PaymentGatewayPort {

    PgPaymentResult processPayment(long amount, String method);
}
