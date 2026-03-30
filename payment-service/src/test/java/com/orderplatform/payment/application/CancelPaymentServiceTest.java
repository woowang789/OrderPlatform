package com.orderplatform.payment.application;

import com.orderplatform.payment.application.port.in.CancelPaymentCommand;
import com.orderplatform.payment.application.port.in.PaymentInfo;
import com.orderplatform.payment.application.port.out.LoadPaymentPort;
import com.orderplatform.payment.application.port.out.SavePaymentPort;
import com.orderplatform.payment.application.port.out.UpdateOrderStatusPort;
import com.orderplatform.payment.application.service.CancelPaymentService;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CancelPaymentServiceTest {

    @Mock LoadPaymentPort loadPaymentPort;
    @Mock SavePaymentPort savePaymentPort;
    @Mock UpdateOrderStatusPort updateOrderStatusPort;
    @InjectMocks CancelPaymentService cancelPaymentService;

    private final UUID paymentId = UUID.randomUUID();
    private final UUID orderId = UUID.randomUUID();
    private final Long memberId = 1L;

    @Test
    void 정상_결제_취소() {
        CancelPaymentCommand command = new CancelPaymentCommand(memberId, paymentId);
        Payment completedPayment = Payment.reconstitute(
                paymentId, orderId, memberId, 10000,
                PaymentMethod.CARD, PaymentStatus.COMPLETED, "PG_TXN_123", null,
                LocalDateTime.now(), LocalDateTime.now()
        );
        given(loadPaymentPort.findByIdAndMemberId(paymentId, memberId))
                .willReturn(Optional.of(completedPayment));
        given(savePaymentPort.save(any(Payment.class))).willAnswer(invocation -> invocation.getArgument(0));

        PaymentInfo result = cancelPaymentService.cancelPayment(command);

        assertThat(result.status()).isEqualTo("CANCELLED");
        verify(updateOrderStatusPort).cancelOrder(memberId, orderId);
    }

    @Test
    void 미존재시_예외() {
        CancelPaymentCommand command = new CancelPaymentCommand(memberId, paymentId);
        given(loadPaymentPort.findByIdAndMemberId(paymentId, memberId))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> cancelPaymentService.cancelPayment(command))
                .isInstanceOf(PaymentNotFoundException.class);
    }
}
