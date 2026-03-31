package com.orderplatform.order.application.service;

import com.orderplatform.common.domain.event.OrderCancelledEvent;
import com.orderplatform.common.domain.event.OrderConfirmedEvent;
import com.orderplatform.common.domain.event.PaymentCancelRequestedEvent;
import com.orderplatform.order.application.port.in.HandleOrderEventUseCase;
import com.orderplatform.order.application.port.out.LoadOrderPort;
import com.orderplatform.order.application.port.out.OrderEventPublishPort;
import com.orderplatform.order.application.port.out.SaveOrderPort;
import com.orderplatform.order.domain.exception.OrderNotFoundException;
import com.orderplatform.order.domain.model.Order;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 이벤트 기반 주문 상태 전이 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HandleOrderEventService implements HandleOrderEventUseCase {

    private final LoadOrderPort loadOrderPort;
    private final SaveOrderPort saveOrderPort;
    private final OrderEventPublishPort orderEventPublishPort;

    @Override
    @Transactional
    public void handlePaymentCompleted(UUID orderId) {
        log.info("결제 완료 처리 — orderId: {}", orderId);
        Order order = loadOrderWithLock(orderId);
        order.markPaid();
        saveOrderPort.save(order);
    }

    @Override
    @Transactional
    public void handlePaymentFailed(UUID orderId) {
        log.info("결제 실패 처리 — orderId: {}", orderId);
        Order order = loadOrderWithLock(orderId);
        order.cancel();
        saveOrderPort.save(order);

        orderEventPublishPort.publishOrderCancelled(
                new OrderCancelledEvent(orderId, "결제 실패"));
    }

    @Override
    @Transactional
    public void handleStockDeducted(UUID orderId) {
        log.info("재고 차감 완료 처리 — orderId: {}", orderId);
        Order order = loadOrderWithLock(orderId);
        order.markConfirmed();
        saveOrderPort.save(order);

        orderEventPublishPort.publishOrderConfirmed(
                new OrderConfirmedEvent(orderId));
    }

    @Override
    @Transactional
    public void handleStockDeductionFailed(UUID orderId) {
        log.info("재고 차감 실패 처리 — orderId: {}, 결제 취소 요청 발행", orderId);

        // 결제 취소 요청 이벤트 발행 (Saga 보상 시작)
        // paymentId는 Payment Service가 orderId로 조회하므로 null 허용하지 않아
        // 여기서는 orderId만 전달하고, PaymentCancelRequestedEvent의 paymentId는 없으므로
        // 편의 생성자를 사용할 수 없음 → 직접 생성
        orderEventPublishPort.publishPaymentCancelRequested(
                new PaymentCancelRequestedEvent(orderId, null, "재고 부족"));
    }

    @Override
    @Transactional
    public void handlePaymentCancelled(UUID orderId) {
        log.info("결제 취소 완료 처리 — orderId: {}", orderId);
        Order order = loadOrderWithLock(orderId);
        order.cancel();
        saveOrderPort.save(order);

        orderEventPublishPort.publishOrderCancelled(
                new OrderCancelledEvent(orderId, "재고 부족으로 결제 취소"));
    }

    private Order loadOrderWithLock(UUID orderId) {
        return loadOrderPort.findByIdWithLock(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
    }
}
