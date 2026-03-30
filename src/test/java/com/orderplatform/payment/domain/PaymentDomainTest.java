package com.orderplatform.payment.domain;

import com.orderplatform.payment.domain.exception.InvalidPaymentStatusException;
import com.orderplatform.payment.domain.model.Payment;
import com.orderplatform.payment.domain.model.PaymentMethod;
import com.orderplatform.payment.domain.model.PaymentStatus;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Payment 도메인 모델 단위 테스트 — 순수 Java (Spring 없음)
 */
class PaymentDomainTest {

    private final UUID orderId = UUID.randomUUID();
    private final Long memberId = 1L;

    @Test
    void create_orderId_null이면_예외() {
        assertThatThrownBy(() -> Payment.create(null, memberId, 10000, PaymentMethod.CARD))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void create_금액_0이하면_예외() {
        assertThatThrownBy(() -> Payment.create(orderId, memberId, 0, PaymentMethod.CARD))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> Payment.create(orderId, memberId, -1000, PaymentMethod.CARD))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void create_정상_생성시_PENDING() {
        Payment payment = Payment.create(orderId, memberId, 10000, PaymentMethod.CARD);

        assertThat(payment.getId()).isNull();
        assertThat(payment.getOrderId()).isEqualTo(orderId);
        assertThat(payment.getMemberId()).isEqualTo(memberId);
        assertThat(payment.getAmount()).isEqualTo(10000);
        assertThat(payment.getMethod()).isEqualTo(PaymentMethod.CARD);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(payment.getPgTxnId()).isNull();
        assertThat(payment.getFailReason()).isNull();
    }

    @Test
    void complete_PENDING에서_COMPLETED() {
        Payment payment = Payment.create(orderId, memberId, 10000, PaymentMethod.CARD);

        payment.complete("PG_TXN_123");

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(payment.getPgTxnId()).isEqualTo("PG_TXN_123");
    }

    @Test
    void complete_비PENDING_상태면_예외() {
        Payment payment = Payment.create(orderId, memberId, 10000, PaymentMethod.CARD);
        payment.complete("PG_TXN_123");

        assertThatThrownBy(() -> payment.complete("PG_TXN_456"))
                .isInstanceOf(InvalidPaymentStatusException.class);
    }

    @Test
    void fail_PENDING에서_FAILED() {
        Payment payment = Payment.create(orderId, memberId, 10000, PaymentMethod.CARD);

        payment.fail("잔액 부족");

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(payment.getFailReason()).isEqualTo("잔액 부족");
    }

    @Test
    void fail_비PENDING_상태면_예외() {
        Payment payment = Payment.create(orderId, memberId, 10000, PaymentMethod.CARD);
        payment.fail("실패");

        assertThatThrownBy(() -> payment.fail("다시 실패"))
                .isInstanceOf(InvalidPaymentStatusException.class);
    }

    @Test
    void cancel_COMPLETED에서_CANCELLED() {
        Payment payment = Payment.create(orderId, memberId, 10000, PaymentMethod.CARD);
        payment.complete("PG_TXN_123");

        payment.cancel();

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CANCELLED);
    }

    @Test
    void cancel_비COMPLETED_상태면_예외() {
        Payment payment = Payment.create(orderId, memberId, 10000, PaymentMethod.CARD);

        // PENDING 상태에서 cancel 시도
        assertThatThrownBy(() -> payment.cancel())
                .isInstanceOf(InvalidPaymentStatusException.class);
    }

    @Test
    void cancel_FAILED_상태에서_예외() {
        Payment payment = Payment.create(orderId, memberId, 10000, PaymentMethod.CARD);
        payment.fail("실패");

        assertThatThrownBy(() -> payment.cancel())
                .isInstanceOf(InvalidPaymentStatusException.class);
    }
}
