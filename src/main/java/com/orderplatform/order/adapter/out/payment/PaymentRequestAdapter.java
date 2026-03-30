package com.orderplatform.order.adapter.out.payment;

import com.orderplatform.order.application.port.out.RequestPaymentPort;
import com.orderplatform.payment.application.port.in.RequestPaymentCommand;
import com.orderplatform.payment.application.port.in.RequestPaymentUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * 결제 요청 어댑터 — 의도적 동기 호출
 * Port 추상화 뒤에서도 장애 전파 문제를 체감하기 위한 설계.
 * Phase 3에서 Kafka 이벤트 기반 비동기 호출로 전환 예정.
 */
@Component
@RequiredArgsConstructor
public class PaymentRequestAdapter implements RequestPaymentPort {

    private final RequestPaymentUseCase requestPaymentUseCase;

    @Override
    public void requestPayment(UUID orderId, Long memberId, long totalAmount) {
        RequestPaymentCommand command = new RequestPaymentCommand(
                memberId,
                orderId,
                "CARD"  // 기본 결제 수단 — Phase 2에서는 단순화
        );
        requestPaymentUseCase.requestPayment(command);
    }
}
