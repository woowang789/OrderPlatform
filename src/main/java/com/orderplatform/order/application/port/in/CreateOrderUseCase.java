package com.orderplatform.order.application.port.in;

public interface CreateOrderUseCase {

    OrderInfo createOrder(CreateOrderCommand command);
}
