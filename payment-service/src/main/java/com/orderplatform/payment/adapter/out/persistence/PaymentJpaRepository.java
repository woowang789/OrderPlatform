package com.orderplatform.payment.adapter.out.persistence;

import com.orderplatform.payment.domain.model.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PaymentJpaRepository extends JpaRepository<PaymentJpaEntity, UUID> {

    Optional<PaymentJpaEntity> findByOrderIdAndStatusNot(UUID orderId, PaymentStatus status);

    Optional<PaymentJpaEntity> findByIdAndMemberId(UUID id, Long memberId);
}
