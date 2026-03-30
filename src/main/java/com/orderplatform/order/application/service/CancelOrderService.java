package com.orderplatform.order.application.service;

import com.orderplatform.order.application.port.in.CancelOrderCommand;
import com.orderplatform.order.application.port.in.CancelOrderUseCase;
import com.orderplatform.order.application.port.in.OrderInfo;
import com.orderplatform.order.application.port.out.LoadOrderPort;
import com.orderplatform.order.application.port.out.RestoreStockPort;
import com.orderplatform.order.application.port.out.SaveOrderPort;
import com.orderplatform.order.domain.exception.OrderNotFoundException;
import com.orderplatform.order.domain.model.Order;
import com.orderplatform.order.domain.model.OrderLine;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 주문 취소 서비스
 * 주문 취소 → 재고 복원 (단일 @Transactional)
 */
@Service
@RequiredArgsConstructor
public class CancelOrderService implements CancelOrderUseCase {

    private final LoadOrderPort loadOrderPort;
    private final SaveOrderPort saveOrderPort;
    private final RestoreStockPort restoreStockPort;

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

        // 3. 재고 복원
        for (OrderLine line : order.getOrderLines()) {
            restoreStockPort.restoreStock(line.productId(), line.quantity());
        }

        // 4. 저장 및 반환
        Order savedOrder = saveOrderPort.save(order);
        return OrderInfo.from(savedOrder);
    }
}
