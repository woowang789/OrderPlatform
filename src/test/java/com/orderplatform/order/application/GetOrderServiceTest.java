package com.orderplatform.order.application;

import com.orderplatform.order.application.port.in.OrderInfo;
import com.orderplatform.order.application.port.out.LoadOrderPort;
import com.orderplatform.order.application.service.GetOrderService;
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
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class GetOrderServiceTest {

    @Mock LoadOrderPort loadOrderPort;
    @InjectMocks GetOrderService getOrderService;

    private final UUID orderId = UUID.randomUUID();
    private final Long memberId = 1L;
    private final LocalDateTime now = LocalDateTime.now();

    private Order sampleOrder() {
        return Order.reconstitute(orderId, memberId, OrderStatus.PLACED, 10000, 0L,
                List.of(new OrderLine(1L, "상품A", 10000, 1)), now, now);
    }

    @Test
    void 단건_조회() {
        given(loadOrderPort.findById(orderId)).willReturn(Optional.of(sampleOrder()));

        OrderInfo result = getOrderService.getOrder(memberId, orderId);

        assertThat(result.id()).isEqualTo(orderId);
        assertThat(result.status()).isEqualTo("PLACED");
    }

    @Test
    void 타인_주문_조회시_예외() {
        given(loadOrderPort.findById(orderId)).willReturn(Optional.of(sampleOrder()));

        assertThatThrownBy(() -> getOrderService.getOrder(999L, orderId))
                .isInstanceOf(OrderNotFoundException.class);
    }

    @Test
    void 미존재_주문_조회시_예외() {
        given(loadOrderPort.findById(orderId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> getOrderService.getOrder(memberId, orderId))
                .isInstanceOf(OrderNotFoundException.class);
    }

    @Test
    void 내_주문_목록_조회() {
        given(loadOrderPort.findAllByMemberId(memberId))
                .willReturn(List.of(sampleOrder()));

        List<OrderInfo> result = getOrderService.getMyOrders(memberId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).memberId()).isEqualTo(memberId);
    }
}
