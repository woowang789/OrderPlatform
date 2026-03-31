package com.orderplatform.notification.application.service;

import com.orderplatform.common.domain.event.OrderCancelledEvent;
import com.orderplatform.common.domain.event.OrderConfirmedEvent;
import com.orderplatform.common.domain.event.PaymentCompletedEvent;
import com.orderplatform.common.domain.event.PaymentFailedEvent;
import com.orderplatform.notification.application.port.in.SendNotificationUseCase;
import com.orderplatform.notification.application.port.out.NotificationSendPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 알림 발송 UseCase 구현체.
 * 이벤트에서 주문/결제 정보를 추출하여 알림 메시지를 구성하고 발송한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService implements SendNotificationUseCase {

    private final NotificationSendPort notificationSendPort;

    @Override
    public void notifyOrderConfirmed(OrderConfirmedEvent event) {
        notificationSendPort.send(
                null,
                "주문 확정",
                String.format("주문 %s가 확정되었습니다.", event.orderId())
        );
    }

    @Override
    public void notifyOrderCancelled(OrderCancelledEvent event) {
        notificationSendPort.send(
                null,
                "주문 취소",
                String.format("주문 %s가 취소되었습니다. 사유: %s", event.orderId(), event.reason())
        );
    }

    @Override
    public void notifyPaymentCompleted(PaymentCompletedEvent event) {
        notificationSendPort.send(
                null,
                "결제 완료",
                String.format("주문 %s의 결제가 완료되었습니다. 결제 금액: %,d원", event.orderId(), event.amount())
        );
    }

    @Override
    public void notifyPaymentFailed(PaymentFailedEvent event) {
        notificationSendPort.send(
                null,
                "결제 실패",
                String.format("주문 %s의 결제가 실패하였습니다. 사유: %s", event.orderId(), event.reason())
        );
    }
}
