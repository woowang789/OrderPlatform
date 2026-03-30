package com.orderplatform.payment.adapter.out.order;

import com.orderplatform.payment.application.port.out.LoadOrderForPaymentPort;
import com.orderplatform.payment.application.port.out.OrderInfoForPayment;
import com.orderplatform.payment.application.port.out.UpdateOrderStatusPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Order 서비스 연동 Stub — Phase 3 6단계에서 Kafka 이벤트 기반으로 전환 예정
 */
@Slf4j
@Component
public class NoOpOrderAdapter implements LoadOrderForPaymentPort, UpdateOrderStatusPort {

    @Override
    public OrderInfoForPayment loadOrder(UUID orderId) {
        log.info("[Stub] 주문 조회 요청 — orderId: {} (Phase 3 6단계에서 이벤트로 전환 예정)", orderId);
        return new OrderInfoForPayment(orderId, 0L, 0L, "PLACED");
    }

    @Override
    public void markOrderPaid(UUID orderId) {
        log.info("[Stub] 주문 결제완료 처리 — orderId: {} (Phase 3 6단계에서 이벤트로 전환 예정)", orderId);
    }

    @Override
    public void cancelOrder(Long memberId, UUID orderId) {
        log.info("[Stub] 주문 취소 요청 — memberId: {}, orderId: {} (Phase 3 6단계에서 이벤트로 전환 예정)", memberId, orderId);
    }
}
