package com.orderplatform.payment.adapter.out.order;

import com.orderplatform.order.adapter.out.persistence.OrderJpaEntity;
import com.orderplatform.order.adapter.out.persistence.OrderJpaRepository;
import com.orderplatform.order.application.port.in.CancelOrderCommand;
import com.orderplatform.order.application.port.in.CancelOrderUseCase;
import com.orderplatform.order.domain.exception.OrderNotFoundException;
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

    private final OrderJpaRepository orderJpaRepository;
    private final CancelOrderUseCase cancelOrderUseCase;

    @Override
    public OrderInfoForPayment loadOrder(UUID orderId) {
        OrderJpaEntity order = orderJpaRepository.findByIdWithOrderLines(orderId)
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
        OrderJpaEntity order = orderJpaRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
        order.updateFrom(com.orderplatform.order.domain.model.OrderStatus.PAID);
    }

    @Override
    public void cancelOrder(Long memberId, UUID orderId) {
        cancelOrderUseCase.cancelOrder(new CancelOrderCommand(orderId, memberId));
    }
}
