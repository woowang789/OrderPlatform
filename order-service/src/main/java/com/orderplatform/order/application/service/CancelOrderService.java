package com.orderplatform.order.application.service;

import com.orderplatform.common.domain.event.OrderCancelledEvent;
import com.orderplatform.order.application.port.in.CancelOrderCommand;
import com.orderplatform.order.application.port.in.CancelOrderUseCase;
import com.orderplatform.order.application.port.in.OrderInfo;
import com.orderplatform.order.application.port.out.LoadOrderPort;
import com.orderplatform.order.application.port.out.OrderEventPublishPort;
import com.orderplatform.order.application.port.out.SaveOrderPort;
import com.orderplatform.order.domain.exception.OrderNotFoundException;
import com.orderplatform.order.domain.model.Order;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 주문 취소 서비스
 * 주문 취소 → 저장 → OrderCancelledEvent Kafka 발행
 */
@Service
@RequiredArgsConstructor
public class CancelOrderService implements CancelOrderUseCase {

    private final LoadOrderPort loadOrderPort;
    private final SaveOrderPort saveOrderPort;
    private final OrderEventPublishPort orderEventPublishPort;

    @Override
    @Transactional
    public OrderInfo cancelOrder(CancelOrderCommand command) {
        // 1. 주문 조회 + 본인 확인
        Order order = loadOrderPort.findById(command.orderId())
                .orElseThrow(() -> new OrderNotFoundException(command.orderId()));

        if (!order.getMemberId().equals(command.memberId())) {
            throw new OrderNotFoundException(command.orderId());
        }

        // 2. 주문 취소
        order.cancel();

        // 3. 저장
        Order savedOrder = saveOrderPort.save(order);

        // 4. OrderCancelledEvent Kafka 발행
        orderEventPublishPort.publishOrderCancelled(
                new OrderCancelledEvent(savedOrder.getId(), "사용자 직접 취소"));

        return OrderInfo.from(savedOrder);
    }
}
