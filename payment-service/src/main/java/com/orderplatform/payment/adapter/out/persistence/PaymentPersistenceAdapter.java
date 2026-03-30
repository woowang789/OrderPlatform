package com.orderplatform.payment.adapter.out.persistence;

import com.orderplatform.payment.application.port.out.LoadPaymentPort;
import com.orderplatform.payment.application.port.out.SavePaymentPort;
import com.orderplatform.payment.domain.model.Payment;
import com.orderplatform.payment.domain.model.PaymentStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@SuppressWarnings("null")
@Component
@RequiredArgsConstructor
public class PaymentPersistenceAdapter implements LoadPaymentPort, SavePaymentPort {

    private final PaymentJpaRepository paymentJpaRepository;

    @Override
    public Optional<Payment> findByIdAndMemberId(UUID id, Long memberId) {
        return paymentJpaRepository.findByIdAndMemberId(id, memberId)
                .map(PaymentMapper::toDomain);
    }

    @Override
    public Optional<Payment> findByOrderIdExcludingCancelled(UUID orderId) {
        return paymentJpaRepository.findByOrderIdAndStatusNot(orderId, PaymentStatus.CANCELLED)
                .map(PaymentMapper::toDomain);
    }

    @Override
    public Payment save(Payment payment) {
        if (payment.getId() == null) {
            // 신규 생성
            PaymentJpaEntity entity = PaymentMapper.toJpaEntity(payment);
            PaymentJpaEntity saved = paymentJpaRepository.save(entity);
            return PaymentMapper.toDomain(saved);
        } else {
            // 업데이트 — 영속 상태의 엔티티를 조회하여 필드 갱신 (dirty checking)
            PaymentJpaEntity entity = paymentJpaRepository.findById(payment.getId())
                    .orElseThrow();
            entity.updateFrom(
                    payment.getStatus(),
                    payment.getPgTxnId(),
                    payment.getFailReason()
            );
            return PaymentMapper.toDomain(entity);
        }
    }
}
