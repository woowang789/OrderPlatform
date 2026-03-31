package com.orderplatform.payment.application.port.out;

import com.orderplatform.common.domain.event.PaymentCancelledEvent;
import com.orderplatform.common.domain.event.PaymentCompletedEvent;
import com.orderplatform.common.domain.event.PaymentFailedEvent;

/**
 * 결제 이벤트 발행 Outbound Port
 */
public interface PaymentEventPublishPort {

    void publishPaymentCompleted(PaymentCompletedEvent event);

    void publishPaymentFailed(PaymentFailedEvent event);

    void publishPaymentCancelled(PaymentCancelledEvent event);
}
