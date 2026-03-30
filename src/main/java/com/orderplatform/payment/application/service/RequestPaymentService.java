package com.orderplatform.payment.application.service;

import com.orderplatform.common.exception.BusinessException;
import com.orderplatform.order.domain.exception.OrderNotFoundException;
import com.orderplatform.payment.application.port.in.PaymentInfo;
import com.orderplatform.payment.application.port.in.RequestPaymentCommand;
import com.orderplatform.payment.application.port.in.RequestPaymentUseCase;
import com.orderplatform.payment.application.port.out.*;
import com.orderplatform.payment.domain.exception.DuplicatePaymentException;
import com.orderplatform.payment.domain.model.Payment;
import com.orderplatform.payment.domain.model.PaymentMethod;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 결제 요청 서비스
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RequestPaymentService implements RequestPaymentUseCase {

    private final LoadPaymentPort loadPaymentPort;
    private final SavePaymentPort savePaymentPort;
    private final PaymentGatewayPort paymentGatewayPort;
    private final LoadOrderForPaymentPort loadOrderForPaymentPort;
    private final UpdateOrderStatusPort updateOrderStatusPort;

    @Override
    @Transactional
    public PaymentInfo requestPayment(RequestPaymentCommand command) {
        // 1. 주문 조회 및 소유자 검증
        OrderInfoForPayment order = loadOrderForPaymentPort.loadOrder(command.orderId());

        if (!order.memberId().equals(command.memberId())) {
            throw new OrderNotFoundException(command.orderId());
        }

        // 2. 중복 결제 방지
        loadPaymentPort.findByOrderIdExcludingCancelled(command.orderId())
                .ifPresent(p -> {
                    throw new DuplicatePaymentException(command.orderId());
                });

        // 3. 주문 상태 확인 (PLACED만 결제 가능)
        if (!"PLACED".equals(order.status())) {
            throw new BusinessException(
                    "현재 상태에서 해당 작업을 수행할 수 없습니다. 현재 상태: " + order.status(),
                    HttpStatus.BAD_REQUEST
            );
        }

        // 4. 결제 생성
        PaymentMethod method = PaymentMethod.valueOf(command.method());
        Payment payment = Payment.create(
                command.orderId(), command.memberId(), order.totalAmount(), method
        );

        // 5. PG 결제 처리
        PgPaymentResult pgResult = paymentGatewayPort.processPayment(
                order.totalAmount(), command.method()
        );

        if (pgResult.success()) {
            payment.complete(pgResult.pgTxnId());
            updateOrderStatusPort.markOrderPaid(command.orderId());
        } else {
            payment.fail(pgResult.failReason());
        }

        // 6. 저장 및 반환
        Payment saved = savePaymentPort.save(payment);
        return PaymentInfo.from(saved);
    }
}
