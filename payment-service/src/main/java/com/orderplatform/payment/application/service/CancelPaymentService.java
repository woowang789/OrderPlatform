package com.orderplatform.payment.application.service;

import com.orderplatform.common.domain.event.PaymentCancelledEvent;
import com.orderplatform.payment.application.port.in.CancelPaymentCommand;
import com.orderplatform.payment.application.port.in.CancelPaymentUseCase;
import com.orderplatform.payment.application.port.in.PaymentInfo;
import com.orderplatform.payment.application.port.out.LoadPaymentPort;
import com.orderplatform.payment.application.port.out.PaymentEventPublishPort;
import com.orderplatform.payment.application.port.out.SavePaymentPort;
import com.orderplatform.payment.domain.exception.PaymentNotFoundException;
import com.orderplatform.payment.domain.model.Payment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 결제 취소 서비스
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CancelPaymentService implements CancelPaymentUseCase {

    private final LoadPaymentPort loadPaymentPort;
    private final SavePaymentPort savePaymentPort;
    private final PaymentEventPublishPort paymentEventPublishPort;

    @Override
    @Transactional
    public PaymentInfo cancelPayment(CancelPaymentCommand command) {
        Payment payment = loadPaymentPort.findByIdAndMemberId(command.paymentId(), command.memberId())
                .orElseThrow(() -> new PaymentNotFoundException(command.paymentId()));

        payment.cancel();
        Payment saved = savePaymentPort.save(payment);

        paymentEventPublishPort.publishPaymentCancelled(
                new PaymentCancelledEvent(saved.getId(), saved.getOrderId()));

        return PaymentInfo.from(saved);
    }

    @Override
    @Transactional
    public void cancelPaymentByOrderId(UUID orderId) {
        Payment payment = loadPaymentPort.findByOrderIdExcludingCancelled(orderId)
                .orElseThrow(() -> new IllegalStateException(
                        "결제를 찾을 수 없습니다. orderId: " + orderId));

        payment.cancel();
        Payment saved = savePaymentPort.save(payment);

        paymentEventPublishPort.publishPaymentCancelled(
                new PaymentCancelledEvent(saved.getId(), saved.getOrderId()));
    }
}
