package com.orderplatform.order.application.port.in;

import java.util.List;
import java.util.UUID;

public interface GetOrderUseCase {

    OrderInfo getOrder(Long memberId, UUID orderId);

    List<OrderInfo> getMyOrders(Long memberId);
}
