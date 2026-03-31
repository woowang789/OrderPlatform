package com.orderplatform.notification.adapter.in.event;

import com.orderplatform.common.domain.event.OrderCancelledEvent;
import com.orderplatform.common.domain.event.OrderConfirmedEvent;
import com.orderplatform.common.domain.event.PaymentCompletedEvent;
import com.orderplatform.common.domain.event.PaymentFailedEvent;
import com.orderplatform.common.domain.event.Topics;
import com.orderplatform.notification.application.port.in.SendNotificationUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Kafka 이벤트 소비 어댑터 — 알림 발송.
 * 주문/결제 관련 이벤트를 구독하여 알림 UseCase에 위임한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventConsumer {

    private final SendNotificationUseCase sendNotificationUseCase;

    @KafkaListener(topics = Topics.ORDER_CONFIRMED, groupId = "notification-group")
    public void handleOrderConfirmed(OrderConfirmedEvent event) {
        log.info("주문 확정 이벤트 수신 — orderId: {}", event.orderId());
        sendNotificationUseCase.notifyOrderConfirmed(event);
    }

    @KafkaListener(topics = Topics.ORDER_CANCELLED, groupId = "notification-group")
    public void handleOrderCancelled(OrderCancelledEvent event) {
        log.info("주문 취소 이벤트 수신 — orderId: {}", event.orderId());
        sendNotificationUseCase.notifyOrderCancelled(event);
    }

    @KafkaListener(topics = Topics.PAYMENT_COMPLETED, groupId = "notification-group")
    public void handlePaymentCompleted(PaymentCompletedEvent event) {
        log.info("결제 완료 이벤트 수신 — orderId: {}", event.orderId());
        sendNotificationUseCase.notifyPaymentCompleted(event);
    }

    @KafkaListener(topics = Topics.PAYMENT_FAILED, groupId = "notification-group")
    public void handlePaymentFailed(PaymentFailedEvent event) {
        log.info("결제 실패 이벤트 수신 — orderId: {}", event.orderId());
        sendNotificationUseCase.notifyPaymentFailed(event);
    }
}
