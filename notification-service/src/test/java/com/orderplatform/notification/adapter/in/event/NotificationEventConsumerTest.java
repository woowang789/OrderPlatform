package com.orderplatform.notification.adapter.in.event;

import com.orderplatform.common.domain.event.OrderCancelledEvent;
import com.orderplatform.common.domain.event.OrderConfirmedEvent;
import com.orderplatform.common.domain.event.PaymentCompletedEvent;
import com.orderplatform.common.domain.event.PaymentFailedEvent;
import com.orderplatform.notification.application.port.in.SendNotificationUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationEventConsumerTest {

    @Mock
    private SendNotificationUseCase sendNotificationUseCase;

    @InjectMocks
    private NotificationEventConsumer notificationEventConsumer;

    @Test
    @DisplayName("주문 확정 이벤트를 수신하면 UseCase에 위임한다")
    void handleOrderConfirmed() {
        // given
        OrderConfirmedEvent event = new OrderConfirmedEvent(UUID.randomUUID());

        // when
        notificationEventConsumer.handleOrderConfirmed(event);

        // then
        verify(sendNotificationUseCase).notifyOrderConfirmed(event);
    }

    @Test
    @DisplayName("주문 취소 이벤트를 수신하면 UseCase에 위임한다")
    void handleOrderCancelled() {
        // given
        OrderCancelledEvent event = new OrderCancelledEvent(UUID.randomUUID(), "재고 부족");

        // when
        notificationEventConsumer.handleOrderCancelled(event);

        // then
        verify(sendNotificationUseCase).notifyOrderCancelled(event);
    }

    @Test
    @DisplayName("결제 완료 이벤트를 수신하면 UseCase에 위임한다")
    void handlePaymentCompleted() {
        // given
        PaymentCompletedEvent event = new PaymentCompletedEvent(
                UUID.randomUUID(), UUID.randomUUID(), 1L, 50000L, List.of());

        // when
        notificationEventConsumer.handlePaymentCompleted(event);

        // then
        verify(sendNotificationUseCase).notifyPaymentCompleted(event);
    }

    @Test
    @DisplayName("결제 실패 이벤트를 수신하면 UseCase에 위임한다")
    void handlePaymentFailed() {
        // given
        PaymentFailedEvent event = new PaymentFailedEvent(
                UUID.randomUUID(), UUID.randomUUID(), "잔액 부족");

        // when
        notificationEventConsumer.handlePaymentFailed(event);

        // then
        verify(sendNotificationUseCase).notifyPaymentFailed(event);
    }
}
