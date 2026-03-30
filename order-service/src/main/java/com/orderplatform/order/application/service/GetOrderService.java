package com.orderplatform.order.application.service;

import com.orderplatform.order.application.port.in.GetOrderUseCase;
import com.orderplatform.order.application.port.in.OrderInfo;
import com.orderplatform.order.application.port.out.LoadOrderPort;
import com.orderplatform.order.domain.exception.OrderNotFoundException;
import com.orderplatform.order.domain.model.Order;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * 주문 조회 서비스
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetOrderService implements GetOrderUseCase {

    private final LoadOrderPort loadOrderPort;

    @Override
    public OrderInfo getOrder(Long memberId, UUID orderId) {
        Order order = loadOrderPort.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        // 본인 주문만 조회 가능 (타인 주문은 보안상 404 처리)
        if (!order.getMemberId().equals(memberId)) {
            throw new OrderNotFoundException(orderId);
        }

        return OrderInfo.from(order);
    }

    @Override
    public List<OrderInfo> getMyOrders(Long memberId) {
        return loadOrderPort.findAllByMemberId(memberId).stream()
                .map(OrderInfo::from)
                .toList();
    }
}
