package com.orderplatform.payment.application.port.out;

import com.orderplatform.payment.domain.model.Payment;

public interface SavePaymentPort {

    Payment save(Payment payment);
}
