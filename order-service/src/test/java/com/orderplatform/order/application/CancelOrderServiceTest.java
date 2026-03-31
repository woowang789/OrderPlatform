package com.orderplatform.order.application;

import com.orderplatform.common.domain.event.OrderCancelledEvent;
import com.orderplatform.order.application.port.in.CancelOrderCommand;
import com.orderplatform.order.application.port.in.OrderInfo;
import com.orderplatform.order.application.port.out.LoadOrderPort;
import com.orderplatform.order.application.port.out.OrderEventPublishPort;
import com.orderplatform.order.application.port.out.SaveOrderPort;
import com.orderplatform.order.application.service.CancelOrderService;
import com.orderplatform.order.domain.exception.OrderNotFoundException;
import com.orderplatform.order.domain.model.Order;
import com.orderplatform.order.domain.model.OrderLine;
import com.orderplatform.order.domain.model.OrderStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CancelOrderServiceTest {

    @Mock LoadOrderPort loadOrderPort;
    @Mock SaveOrderPort saveOrderPort;
    @Mock OrderEventPublishPort orderEventPublishPort;
    @InjectMocks CancelOrderService cancelOrderService;

    private final UUID orderId = UUID.randomUUID();
    private final Long memberId = 1L;
    private final LocalDateTime now = LocalDateTime.now();

    private Order placedOrder() {
        return Order.reconstitute(orderId, memberId, OrderStatus.PLACED, 25000, 0L,
                List.of(
                        new OrderLine(1L, "상품A", 10000, 2),
                        new OrderLine(2L, "상품B", 5000, 1)
                ), now, now);
    }

    @Test
    void 정상_주문_취소() {
        CancelOrderCommand command = new CancelOrderCommand(orderId, memberId);
        Order order = placedOrder();
        given(loadOrderPort.findById(orderId)).willReturn(Optional.of(order));
        given(saveOrderPort.save(any(Order.class))).willAnswer(invocation -> invocation.getArgument(0));

        OrderInfo result = cancelOrderService.cancelOrder(command);

        assertThat(result.status()).isEqualTo("CANCELLED");
        verify(orderEventPublishPort).publishOrderCancelled(any(OrderCancelledEvent.class));
    }

    @Test
    void 미존재_주문이면_예외() {
        CancelOrderCommand command = new CancelOrderCommand(orderId, memberId);
        given(loadOrderPort.findById(orderId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> cancelOrderService.cancelOrder(command))
                .isInstanceOf(OrderNotFoundException.class);
    }

    @Test
    void 타인_주문이면_예외() {
        CancelOrderCommand command = new CancelOrderCommand(orderId, 999L);
        given(loadOrderPort.findById(orderId)).willReturn(Optional.of(placedOrder()));

        assertThatThrownBy(() -> cancelOrderService.cancelOrder(command))
                .isInstanceOf(OrderNotFoundException.class);
    }
}
