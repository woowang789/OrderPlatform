package com.orderplatform.notification.application.port.in;

import com.orderplatform.common.domain.event.OrderCancelledEvent;
import com.orderplatform.common.domain.event.OrderConfirmedEvent;
import com.orderplatform.common.domain.event.PaymentCompletedEvent;
import com.orderplatform.common.domain.event.PaymentFailedEvent;

/**
 * 알림 발송 UseCase.
 * 각 이벤트 타입별로 적절한 알림 메시지를 구성하여 발송한다.
 */
public interface SendNotificationUseCase {

    void notifyOrderConfirmed(OrderConfirmedEvent event);

    void notifyOrderCancelled(OrderCancelledEvent event);

    void notifyPaymentCompleted(PaymentCompletedEvent event);

    void notifyPaymentFailed(PaymentFailedEvent event);
}
