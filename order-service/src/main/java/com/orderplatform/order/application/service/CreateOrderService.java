package com.orderplatform.order.application.service;

import com.orderplatform.common.domain.event.OrderPlacedEvent;
import com.orderplatform.common.domain.event.payload.OrderItemPayload;
import com.orderplatform.order.application.port.in.*;
import com.orderplatform.order.application.port.out.OrderEventPublishPort;
import com.orderplatform.order.application.port.out.SaveOrderPort;
import com.orderplatform.order.domain.model.Order;
import com.orderplatform.order.domain.model.OrderLine;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 주문 생성 서비스
 * 주문 생성 → 저장 → OrderPlacedEvent Kafka 발행
 */
@Service
@RequiredArgsConstructor
public class CreateOrderService implements CreateOrderUseCase {

    private final SaveOrderPort saveOrderPort;
    private final OrderEventPublishPort orderEventPublishPort;

    @Override
    @Transactional
    public OrderInfo createOrder(CreateOrderCommand command) {
        // 1. Command에서 직접 OrderLine 생성 (클라이언트 제공 스냅샷)
        List<OrderLine> orderLines = command.items().stream()
                .map(item -> new OrderLine(
                        item.productId(), item.productName(),
                        item.price(), item.quantity()))
                .toList();

        // 2. 주문 생성 (CREATED → PLACED)
        Order order = Order.create(command.memberId(), orderLines);

        // 3. 주문 저장
        Order savedOrder = saveOrderPort.save(order);

        // 4. OrderPlacedEvent Kafka 발행
        List<OrderItemPayload> itemPayloads = savedOrder.getOrderLines().stream()
                .map(line -> new OrderItemPayload(
                        line.productId(), line.productName(),
                        line.price(), line.quantity()))
                .toList();

        orderEventPublishPort.publishOrderPlaced(new OrderPlacedEvent(
                savedOrder.getId(), command.memberId(), itemPayloads,
                savedOrder.getTotalAmount(), command.paymentMethod()));

        return OrderInfo.from(savedOrder);
    }
}
