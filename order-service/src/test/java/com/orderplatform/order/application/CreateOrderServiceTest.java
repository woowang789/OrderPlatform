package com.orderplatform.order.application;

import com.orderplatform.common.domain.event.OrderPlacedEvent;
import com.orderplatform.order.application.port.in.CreateOrderCommand;
import com.orderplatform.order.application.port.in.OrderInfo;
import com.orderplatform.order.application.port.in.OrderItemCommand;
import com.orderplatform.order.application.port.out.OrderEventPublishPort;
import com.orderplatform.order.application.port.out.SaveOrderPort;
import com.orderplatform.order.application.service.CreateOrderService;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CreateOrderServiceTest {

    @Mock SaveOrderPort saveOrderPort;
    @Mock OrderEventPublishPort orderEventPublishPort;
    @InjectMocks CreateOrderService createOrderService;

    @Test
    void 정상_주문_생성() {
        List<OrderItemCommand> items = List.of(
                new OrderItemCommand(1L, "상품A", 10000, 2),
                new OrderItemCommand(2L, "상품B", 5000, 1)
        );
        CreateOrderCommand command = new CreateOrderCommand(1L, items, "CARD");

        UUID savedOrderId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        given(saveOrderPort.save(any(Order.class))).willReturn(
                Order.reconstitute(savedOrderId, 1L, OrderStatus.PLACED, 25000, 0L,
                        List.of(
                                new OrderLine(1L, "상품A", 10000, 2),
                                new OrderLine(2L, "상품B", 5000, 1)
                        ), now, now)
        );

        OrderInfo result = createOrderService.createOrder(command);

        assertThat(result.id()).isEqualTo(savedOrderId);
        assertThat(result.status()).isEqualTo("PLACED");
        assertThat(result.totalAmount()).isEqualTo(25000);
        assertThat(result.orderLines()).hasSize(2);

        verify(saveOrderPort).save(any(Order.class));
        verify(orderEventPublishPort).publishOrderPlaced(any(OrderPlacedEvent.class));
    }

    @Test
    void 클라이언트_스냅샷으로_OrderLine_생성_검증() {
        List<OrderItemCommand> items = List.of(
                new OrderItemCommand(1L, "테스트상품", 15000, 3)
        );
        CreateOrderCommand command = new CreateOrderCommand(1L, items, "CARD");

        given(saveOrderPort.save(any(Order.class))).willAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            return Order.reconstitute(UUID.randomUUID(), 1L, order.getStatus(),
                    order.getTotalAmount(), 0L, order.getOrderLines(),
                    LocalDateTime.now(), LocalDateTime.now());
        });

        OrderInfo result = createOrderService.createOrder(command);

        assertThat(result.orderLines()).hasSize(1);
        assertThat(result.orderLines().get(0).productName()).isEqualTo("테스트상품");
        assertThat(result.orderLines().get(0).price()).isEqualTo(15000);
        assertThat(result.orderLines().get(0).quantity()).isEqualTo(3);
    }
}
