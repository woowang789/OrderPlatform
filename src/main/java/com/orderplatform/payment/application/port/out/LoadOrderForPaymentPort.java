package com.orderplatform.payment.application.port.out;

import java.util.UUID;

public interface LoadOrderForPaymentPort {

    OrderInfoForPayment loadOrder(UUID orderId);
}
