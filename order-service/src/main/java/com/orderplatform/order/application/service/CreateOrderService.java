package com.orderplatform.order.application.service;

import com.orderplatform.order.application.port.in.*;
import com.orderplatform.order.application.port.out.DecreaseStockPort;
import com.orderplatform.order.application.port.out.SaveOrderPort;
import com.orderplatform.order.application.port.out.StockInfo;
import com.orderplatform.order.domain.model.Order;
import com.orderplatform.order.domain.model.OrderLine;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 주문 생성 서비스
 * 재고 차감 → 주문 저장 (단일 @Transactional)
 * 결제 요청은 별도 API(POST /api/payments)로 분리.
 * RequestPaymentPort는 Phase 3 이벤트 전환 시 활용 예정.
 */
@Service
@RequiredArgsConstructor
public class CreateOrderService implements CreateOrderUseCase {

    private final DecreaseStockPort decreaseStockPort;
    private final SaveOrderPort saveOrderPort;

    @Override
    @Transactional
    public OrderInfo createOrder(CreateOrderCommand command) {
        // 1. 재고 차감 + 스냅샷 생성
        List<OrderLine> orderLines = new ArrayList<>();
        for (OrderItemCommand item : command.items()) {
            StockInfo stockInfo = decreaseStockPort.decreaseStock(item.productId(), item.quantity());
            orderLines.add(new OrderLine(
                    item.productId(),
                    stockInfo.productName(),
                    stockInfo.price(),
                    item.quantity()
            ));
        }

        // 2. 주문 생성 (CREATED → PLACED)
        Order order = Order.create(command.memberId(), orderLines);

        // 3. 주문 저장
        Order savedOrder = saveOrderPort.save(order);

        return OrderInfo.from(savedOrder);
    }
}
