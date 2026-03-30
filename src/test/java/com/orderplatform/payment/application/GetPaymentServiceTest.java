package com.orderplatform.payment.application;

import com.orderplatform.payment.application.port.in.PaymentInfo;
import com.orderplatform.payment.application.port.out.LoadPaymentPort;
import com.orderplatform.payment.application.service.GetPaymentService;
import com.orderplatform.payment.domain.exception.PaymentNotFoundException;
import com.orderplatform.payment.domain.model.Payment;
import com.orderplatform.payment.domain.model.PaymentMethod;
import com.orderplatform.payment.domain.model.PaymentStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class GetPaymentServiceTest {

    @Mock LoadPaymentPort loadPaymentPort;
    @InjectMocks GetPaymentService getPaymentService;

    private final UUID paymentId = UUID.randomUUID();
    private final Long memberId = 1L;

    @Test
    void 정상_조회() {
        Payment payment = Payment.reconstitute(
                paymentId, UUID.randomUUID(), memberId, 10000,
                PaymentMethod.CARD, PaymentStatus.COMPLETED, "PG_TXN", null,
                LocalDateTime.now(), LocalDateTime.now()
        );
        given(loadPaymentPort.findByIdAndMemberId(paymentId, memberId))
                .willReturn(Optional.of(payment));

        PaymentInfo result = getPaymentService.getPayment(memberId, paymentId);

        assertThat(result.id()).isEqualTo(paymentId);
        assertThat(result.status()).isEqualTo("COMPLETED");
    }

    @Test
    void 미존재시_예외() {
        given(loadPaymentPort.findByIdAndMemberId(paymentId, memberId))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> getPaymentService.getPayment(memberId, paymentId))
                .isInstanceOf(PaymentNotFoundException.class);
    }
}
