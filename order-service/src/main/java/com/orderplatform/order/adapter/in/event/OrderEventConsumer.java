package com.orderplatform.order.adapter.in.event;

import com.orderplatform.common.domain.event.*;
import com.orderplatform.order.application.port.in.HandleOrderEventUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Kafka 이벤트 소비 어댑터 — 주문 상태 전이
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventConsumer {

    private final HandleOrderEventUseCase handleOrderEventUseCase;

    @KafkaListener(topics = Topics.PAYMENT_COMPLETED, groupId = "order-group")
    public void handlePaymentCompleted(PaymentCompletedEvent event) {
        log.info("결제 완료 이벤트 수신 — orderId: {}", event.orderId());
        handleOrderEventUseCase.handlePaymentCompleted(event.orderId());
    }

    @KafkaListener(topics = Topics.PAYMENT_FAILED, groupId = "order-group")
    public void handlePaymentFailed(PaymentFailedEvent event) {
        log.info("결제 실패 이벤트 수신 — orderId: {}", event.orderId());
        handleOrderEventUseCase.handlePaymentFailed(event.orderId());
    }

    @KafkaListener(topics = Topics.STOCK_DEDUCTED, groupId = "order-group")
    public void handleStockDeducted(StockDeductedEvent event) {
        log.info("재고 차감 완료 이벤트 수신 — orderId: {}", event.orderId());
        handleOrderEventUseCase.handleStockDeducted(event.orderId());
    }

    @KafkaListener(topics = Topics.STOCK_DEDUCTION_FAILED, groupId = "order-group")
    public void handleStockDeductionFailed(StockDeductionFailedEvent event) {
        log.info("재고 차감 실패 이벤트 수신 — orderId: {}", event.orderId());
        handleOrderEventUseCase.handleStockDeductionFailed(event.orderId());
    }

    @KafkaListener(topics = Topics.PAYMENT_CANCELLED, groupId = "order-group")
    public void handlePaymentCancelled(PaymentCancelledEvent event) {
        log.info("결제 취소 완료 이벤트 수신 — orderId: {}", event.orderId());
        handleOrderEventUseCase.handlePaymentCancelled(event.orderId());
    }
}
