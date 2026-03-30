package com.orderplatform.payment.application;

import com.orderplatform.common.exception.BusinessException;
import com.orderplatform.payment.application.port.in.PaymentInfo;
import com.orderplatform.payment.application.port.in.RequestPaymentCommand;
import com.orderplatform.payment.application.port.out.*;
import com.orderplatform.payment.application.service.RequestPaymentService;
import com.orderplatform.payment.domain.exception.DuplicatePaymentException;
import com.orderplatform.payment.domain.model.Payment;
import com.orderplatform.payment.domain.model.PaymentMethod;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RequestPaymentServiceTest {

    @Mock LoadPaymentPort loadPaymentPort;
    @Mock SavePaymentPort savePaymentPort;
    @Mock PaymentGatewayPort paymentGatewayPort;
    @Mock LoadOrderForPaymentPort loadOrderForPaymentPort;
    @Mock UpdateOrderStatusPort updateOrderStatusPort;
    @InjectMocks RequestPaymentService requestPaymentService;

    private final UUID orderId = UUID.randomUUID();
    private final Long memberId = 1L;

    @Test
    void 정상_결제_PG_성공() {
        RequestPaymentCommand command = new RequestPaymentCommand(memberId, orderId, "CARD");
        given(loadOrderForPaymentPort.loadOrder(orderId))
                .willReturn(new OrderInfoForPayment(orderId, memberId, 10000, "PLACED"));
        given(loadPaymentPort.findByOrderIdExcludingCancelled(orderId))
                .willReturn(Optional.empty());
        given(paymentGatewayPort.processPayment(10000, "CARD"))
                .willReturn(new PgPaymentResult("PG_TXN_123", true, null));
        given(savePaymentPort.save(any(Payment.class))).willAnswer(invocation -> {
            Payment p = invocation.getArgument(0);
            return Payment.reconstitute(
                    UUID.randomUUID(), orderId, memberId, 10000,
                    PaymentMethod.CARD, p.getStatus(), p.getPgTxnId(), p.getFailReason(),
                    LocalDateTime.now(), LocalDateTime.now()
            );
        });

        PaymentInfo result = requestPaymentService.requestPayment(command);

        assertThat(result.status()).isEqualTo("COMPLETED");
        verify(updateOrderStatusPort).markOrderPaid(orderId);
    }

    @Test
    void PG_실패시_FAILED() {
        RequestPaymentCommand command = new RequestPaymentCommand(memberId, orderId, "CARD");
        given(loadOrderForPaymentPort.loadOrder(orderId))
                .willReturn(new OrderInfoForPayment(orderId, memberId, 10000, "PLACED"));
        given(loadPaymentPort.findByOrderIdExcludingCancelled(orderId))
                .willReturn(Optional.empty());
        given(paymentGatewayPort.processPayment(10000, "CARD"))
                .willReturn(new PgPaymentResult(null, false, "잔액 부족"));
        given(savePaymentPort.save(any(Payment.class))).willAnswer(invocation -> {
            Payment p = invocation.getArgument(0);
            return Payment.reconstitute(
                    UUID.randomUUID(), orderId, memberId, 10000,
                    PaymentMethod.CARD, p.getStatus(), p.getPgTxnId(), p.getFailReason(),
                    LocalDateTime.now(), LocalDateTime.now()
            );
        });

        PaymentInfo result = requestPaymentService.requestPayment(command);

        assertThat(result.status()).isEqualTo("FAILED");
        verify(updateOrderStatusPort, never()).markOrderPaid(any());
    }

    @Test
    void 중복_결제시_예외() {
        RequestPaymentCommand command = new RequestPaymentCommand(memberId, orderId, "CARD");
        given(loadOrderForPaymentPort.loadOrder(orderId))
                .willReturn(new OrderInfoForPayment(orderId, memberId, 10000, "PLACED"));
        given(loadPaymentPort.findByOrderIdExcludingCancelled(orderId))
                .willReturn(Optional.of(Payment.create(orderId, memberId, 10000, PaymentMethod.CARD)));

        assertThatThrownBy(() -> requestPaymentService.requestPayment(command))
                .isInstanceOf(DuplicatePaymentException.class);
    }

    @Test
    void 비PLACED_주문이면_예외() {
        RequestPaymentCommand command = new RequestPaymentCommand(memberId, orderId, "CARD");
        given(loadOrderForPaymentPort.loadOrder(orderId))
                .willReturn(new OrderInfoForPayment(orderId, memberId, 10000, "PAID"));
        given(loadPaymentPort.findByOrderIdExcludingCancelled(orderId))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> requestPaymentService.requestPayment(command))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void 타인_주문이면_예외() {
        RequestPaymentCommand command = new RequestPaymentCommand(memberId, orderId, "CARD");
        given(loadOrderForPaymentPort.loadOrder(orderId))
                .willReturn(new OrderInfoForPayment(orderId, 999L, 10000, "PLACED"));

        assertThatThrownBy(() -> requestPaymentService.requestPayment(command))
                .isInstanceOf(BusinessException.class);
    }
}
