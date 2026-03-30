package com.orderplatform.payment.adapter.out.order;

import com.orderplatform.common.exception.OrderNotFoundException;
import com.orderplatform.order.entity.Order;
import com.orderplatform.order.repository.OrderRepository;
import com.orderplatform.order.service.OrderService;
import com.orderplatform.payment.application.port.out.LoadOrderForPaymentPort;
import com.orderplatform.payment.application.port.out.OrderInfoForPayment;
import com.orderplatform.payment.application.port.out.UpdateOrderStatusPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Order 컨텍스트 연동 Adapter (모놀리스 한정)
 * Phase 3 MSA 전환 시 Kafka 이벤트 기반으로 교체 예정
 */
@Component
@RequiredArgsConstructor
public class OrderAdapterForPayment implements LoadOrderForPaymentPort, UpdateOrderStatusPort {

    private final OrderRepository orderRepository;
    private final OrderService orderService;

    @Override
    public OrderInfoForPayment loadOrder(UUID orderId) {
        Order order = orderRepository.findByIdWithOrderLines(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        return new OrderInfoForPayment(
                order.getId(),
                order.getMemberId(),
                order.getTotalAmount(),
                order.getStatus().name()
        );
    }

    @Override
    public void markOrderPaid(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
        order.markPaid();
    }

    @Override
    public void cancelOrder(Long memberId, UUID orderId) {
        orderService.cancelOrder(memberId, orderId);
    }
}
