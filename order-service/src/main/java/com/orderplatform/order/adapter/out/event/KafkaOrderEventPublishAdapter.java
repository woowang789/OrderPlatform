package com.orderplatform.order.adapter.out.event;

import com.orderplatform.common.domain.event.*;
import com.orderplatform.order.application.port.out.OrderEventPublishPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Kafka 기반 주문 이벤트 발행 어댑터
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaOrderEventPublishAdapter implements OrderEventPublishPort {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    public void publishOrderPlaced(OrderPlacedEvent event) {
        log.info("주문 생성 이벤트 발행 — orderId: {}", event.orderId());
        kafkaTemplate.send(Topics.ORDER_PLACED, event.orderId().toString(), event);
    }

    @Override
    public void publishOrderConfirmed(OrderConfirmedEvent event) {
        log.info("주문 확정 이벤트 발행 — orderId: {}", event.orderId());
        kafkaTemplate.send(Topics.ORDER_CONFIRMED, event.orderId().toString(), event);
    }

    @Override
    public void publishOrderCancelled(OrderCancelledEvent event) {
        log.info("주문 취소 이벤트 발행 — orderId: {}", event.orderId());
        kafkaTemplate.send(Topics.ORDER_CANCELLED, event.orderId().toString(), event);
    }

    @Override
    public void publishPaymentCancelRequested(PaymentCancelRequestedEvent event) {
        log.info("결제 취소 요청 이벤트 발행 — orderId: {}", event.orderId());
        kafkaTemplate.send(Topics.PAYMENT_CANCEL_REQUESTED, event.orderId().toString(), event);
    }
}
