package com.orderplatform.payment.application.service;

import com.orderplatform.common.domain.event.PaymentCompletedEvent;
import com.orderplatform.common.domain.event.PaymentFailedEvent;
import com.orderplatform.payment.application.port.in.PaymentInfo;
import com.orderplatform.payment.application.port.in.RequestPaymentCommand;
import com.orderplatform.payment.application.port.in.RequestPaymentUseCase;
import com.orderplatform.payment.application.port.out.*;
import com.orderplatform.payment.domain.exception.DuplicatePaymentException;
import com.orderplatform.payment.domain.model.Payment;
import com.orderplatform.payment.domain.model.PaymentMethod;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 결제 요청 서비스
 * 이벤트 페이로드에서 주문 정보를 직접 받아 결제를 처리한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RequestPaymentService implements RequestPaymentUseCase {

    private final LoadPaymentPort loadPaymentPort;
    private final SavePaymentPort savePaymentPort;
    private final PaymentGatewayPort paymentGatewayPort;
    private final PaymentEventPublishPort paymentEventPublishPort;

    @Override
    @Transactional
    public PaymentInfo requestPayment(RequestPaymentCommand command) {
        // 1. 중복 결제 방지
        loadPaymentPort.findByOrderIdExcludingCancelled(command.orderId())
                .ifPresent(p -> {
                    throw new DuplicatePaymentException(command.orderId());
                });

        // 2. 결제 생성
        PaymentMethod method = PaymentMethod.valueOf(command.method());
        Payment payment = Payment.create(
                command.orderId(), command.memberId(), command.totalAmount(), method
        );

        // 3. PG 결제 처리
        PgPaymentResult pgResult = paymentGatewayPort.processPayment(
                command.totalAmount(), command.method()
        );

        if (pgResult.success()) {
            payment.complete(pgResult.pgTxnId());
        } else {
            payment.fail(pgResult.failReason());
        }

        // 4. 저장
        Payment saved = savePaymentPort.save(payment);

        // 5. 이벤트 발행
        if (saved.getStatus() == com.orderplatform.payment.domain.model.PaymentStatus.COMPLETED) {
            paymentEventPublishPort.publishPaymentCompleted(new PaymentCompletedEvent(
                    saved.getId(), saved.getOrderId(), saved.getMemberId(),
                    saved.getAmount(), command.items()));
        } else {
            paymentEventPublishPort.publishPaymentFailed(new PaymentFailedEvent(
                    saved.getId(), saved.getOrderId(), saved.getFailReason()));
        }

        return PaymentInfo.from(saved);
    }
}
