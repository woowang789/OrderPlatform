package com.orderplatform.payment.application.port.out;

import java.util.UUID;

public interface UpdateOrderStatusPort {

    void markOrderPaid(UUID orderId);

    void cancelOrder(Long memberId, UUID orderId);
}
