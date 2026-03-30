package com.orderplatform.payment.application.port.out;

import com.orderplatform.payment.domain.model.Payment;

import java.util.Optional;
import java.util.UUID;

public interface LoadPaymentPort {

    Optional<Payment> findByIdAndMemberId(UUID id, Long memberId);

    Optional<Payment> findByOrderIdExcludingCancelled(UUID orderId);
}
