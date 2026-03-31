package com.orderplatform.payment.adapter.out.event;

import com.orderplatform.common.domain.event.*;
import com.orderplatform.payment.application.port.out.PaymentEventPublishPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Kafka 기반 결제 이벤트 발행 어댑터
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaPaymentEventPublishAdapter implements PaymentEventPublishPort {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    public void publishPaymentCompleted(PaymentCompletedEvent event) {
        log.info("결제 완료 이벤트 발행 — orderId: {}, paymentId: {}", event.orderId(), event.paymentId());
        kafkaTemplate.send(Topics.PAYMENT_COMPLETED, event.orderId().toString(), event);
    }

    @Override
    public void publishPaymentFailed(PaymentFailedEvent event) {
        log.info("결제 실패 이벤트 발행 — orderId: {}, paymentId: {}", event.orderId(), event.paymentId());
        kafkaTemplate.send(Topics.PAYMENT_FAILED, event.orderId().toString(), event);
    }

    @Override
    public void publishPaymentCancelled(PaymentCancelledEvent event) {
        log.info("결제 취소 이벤트 발행 — orderId: {}, paymentId: {}", event.orderId(), event.paymentId());
        kafkaTemplate.send(Topics.PAYMENT_CANCELLED, event.orderId().toString(), event);
    }
}
