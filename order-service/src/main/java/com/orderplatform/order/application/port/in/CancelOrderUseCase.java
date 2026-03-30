package com.orderplatform.order.application.port.in;

public interface CancelOrderUseCase {

    OrderInfo cancelOrder(CancelOrderCommand command);
}
