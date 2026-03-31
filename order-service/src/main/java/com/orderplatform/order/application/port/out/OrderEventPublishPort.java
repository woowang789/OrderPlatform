package com.orderplatform.order.application.port.out;

import com.orderplatform.common.domain.event.OrderCancelledEvent;
import com.orderplatform.common.domain.event.OrderConfirmedEvent;
import com.orderplatform.common.domain.event.OrderPlacedEvent;
import com.orderplatform.common.domain.event.PaymentCancelRequestedEvent;

/**
 * 주문 이벤트 발행 Outbound Port
 */
public interface OrderEventPublishPort {

    void publishOrderPlaced(OrderPlacedEvent event);

    void publishOrderConfirmed(OrderConfirmedEvent event);

    void publishOrderCancelled(OrderCancelledEvent event);

    void publishPaymentCancelRequested(PaymentCancelRequestedEvent event);
}
