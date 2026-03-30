package com.orderplatform.order.application.port.out;

import com.orderplatform.order.domain.model.Order;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LoadOrderPort {

    Optional<Order> findById(UUID orderId);

    Optional<Order> findByIdWithLock(UUID orderId);

    List<Order> findAllByMemberId(Long memberId);
}
