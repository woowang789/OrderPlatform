package com.orderplatform.order.adapter.out.persistence;

import com.orderplatform.order.domain.model.Order;
import com.orderplatform.order.domain.model.OrderLine;

import java.util.List;

/**
 * Domain Order ↔ OrderJpaEntity 양방향 변환
 */
public final class OrderMapper {

    private OrderMapper() {
    }

    public static Order toDomain(OrderJpaEntity entity) {
        List<OrderLine> orderLines = entity.getOrderLines().stream()
                .map(line -> new OrderLine(
                        line.getProductId(),
                        line.getProductName(),
                        line.getPrice(),
                        line.getQuantity()
                ))
                .toList();

        return Order.reconstitute(
                entity.getId(),
                entity.getMemberId(),
                entity.getStatus(),
                entity.getTotalAmount(),
                entity.getVersion(),
                orderLines,
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public static OrderJpaEntity toJpaEntity(Order order) {
        OrderJpaEntity entity = new OrderJpaEntity(
                order.getMemberId(),
                order.getStatus(),
                order.getTotalAmount()
        );

        for (OrderLine line : order.getOrderLines()) {
            OrderLineJpaEntity lineEntity = new OrderLineJpaEntity(
                    line.productId(),
                    line.productName(),
                    line.price(),
                    line.quantity()
            );
            entity.addOrderLine(lineEntity);
        }

        return entity;
    }
}
