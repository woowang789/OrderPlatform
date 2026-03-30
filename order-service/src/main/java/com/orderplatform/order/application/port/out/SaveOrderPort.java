package com.orderplatform.order.application.port.out;

import com.orderplatform.order.domain.model.Order;

public interface SaveOrderPort {

    Order save(Order order);
}
