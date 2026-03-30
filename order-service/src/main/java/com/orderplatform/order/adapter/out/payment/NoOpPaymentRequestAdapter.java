package com.orderplatform.order.adapter.out.payment;

import com.orderplatform.order.application.port.out.RequestPaymentPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * 결제 요청 Stub — Phase 3 6단계에서 Kafka 이벤트 기반으로 전환 예정
 */
@Slf4j
@Component
public class NoOpPaymentRequestAdapter implements RequestPaymentPort {

    @Override
    public void requestPayment(UUID orderId, Long memberId, long totalAmount) {
        log.info("[Stub] 결제 요청 — orderId: {}, memberId: {}, amount: {} (Phase 3 6단계에서 이벤트로 전환 예정)",
                orderId, memberId, totalAmount);
    }
}
