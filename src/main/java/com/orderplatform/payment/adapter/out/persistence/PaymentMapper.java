package com.orderplatform.payment.adapter.out.persistence;

import com.orderplatform.payment.domain.model.Payment;

/**
 * Domain Payment ↔ PaymentJpaEntity 양방향 변환
 */
public final class PaymentMapper {

    private PaymentMapper() {
    }

    public static PaymentJpaEntity toJpaEntity(Payment payment) {
        return new PaymentJpaEntity(
                payment.getOrderId(),
                payment.getMemberId(),
                payment.getAmount(),
                payment.getMethod(),
                payment.getStatus(),
                payment.getPgTxnId(),
                payment.getFailReason()
        );
    }

    public static Payment toDomain(PaymentJpaEntity entity) {
        return Payment.reconstitute(
                entity.getId(),
                entity.getOrderId(),
                entity.getMemberId(),
                entity.getAmount(),
                entity.getMethod(),
                entity.getStatus(),
                entity.getPgTxnId(),
                entity.getFailReason(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
