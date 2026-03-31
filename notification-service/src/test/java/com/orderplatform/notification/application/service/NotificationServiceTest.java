package com.orderplatform.notification.application.service;

import com.orderplatform.common.domain.event.OrderCancelledEvent;
import com.orderplatform.common.domain.event.OrderConfirmedEvent;
import com.orderplatform.common.domain.event.PaymentCompletedEvent;
import com.orderplatform.common.domain.event.PaymentFailedEvent;
import com.orderplatform.notification.application.port.out.NotificationSendPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationSendPort notificationSendPort;

    @InjectMocks
    private NotificationService notificationService;

    @Test
    @DisplayName("주문 확정 이벤트 수신 시 알림을 발송한다")
    void notifyOrderConfirmed() {
        // given
        OrderConfirmedEvent event = new OrderConfirmedEvent(UUID.randomUUID());

        // when
        notificationService.notifyOrderConfirmed(event);

        // then
        verify(notificationSendPort).send(isNull(), eq("주문 확정"), contains(event.orderId().toString()));
    }

    @Test
    @DisplayName("주문 취소 이벤트 수신 시 알림을 발송한다")
    void notifyOrderCancelled() {
        // given
        OrderCancelledEvent event = new OrderCancelledEvent(UUID.randomUUID(), "재고 부족");

        // when
        notificationService.notifyOrderCancelled(event);

        // then
        verify(notificationSendPort).send(isNull(), eq("주문 취소"), contains("재고 부족"));
    }

    @Test
    @DisplayName("결제 완료 이벤트 수신 시 알림을 발송한다")
    void notifyPaymentCompleted() {
        // given
        PaymentCompletedEvent event = new PaymentCompletedEvent(
                UUID.randomUUID(), UUID.randomUUID(), 1L, 50000L, List.of());

        // when
        notificationService.notifyPaymentCompleted(event);

        // then
        verify(notificationSendPort).send(isNull(), eq("결제 완료"), contains("50,000"));
    }

    @Test
    @DisplayName("결제 실패 이벤트 수신 시 알림을 발송한다")
    void notifyPaymentFailed() {
        // given
        PaymentFailedEvent event = new PaymentFailedEvent(
                UUID.randomUUID(), UUID.randomUUID(), "잔액 부족");

        // when
        notificationService.notifyPaymentFailed(event);

        // then
        verify(notificationSendPort).send(isNull(), eq("결제 실패"), contains("잔액 부족"));
    }
}
