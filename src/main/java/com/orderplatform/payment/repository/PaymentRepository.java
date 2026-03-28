package com.orderplatform.payment.repository;

import com.orderplatform.payment.entity.Payment;
import com.orderplatform.payment.entity.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    Optional<Payment> findByOrderIdAndStatusNot(UUID orderId, PaymentStatus status);

    Optional<Payment> findByIdAndMemberId(UUID id, Long memberId);
}
