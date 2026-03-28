package com.orderplatform.order.service;

import com.orderplatform.common.exception.OrderNotFoundException;
import com.orderplatform.common.exception.ProductNotFoundException;
import com.orderplatform.order.dto.CreateOrderRequest;
import com.orderplatform.order.dto.OrderItemRequest;
import com.orderplatform.order.dto.OrderResponse;
import com.orderplatform.order.entity.Order;
import com.orderplatform.order.entity.OrderLine;
import com.orderplatform.order.repository.OrderRepository;
import com.orderplatform.product.entity.Product;
import com.orderplatform.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@SuppressWarnings("null")
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    /**
     * 주문 생성: 상품 조회 → 재고 차감 → 스냅샷 저장 → 주문 확정 (단일 트랜잭션)
     */
    @Transactional
    public OrderResponse createOrder(Long memberId, CreateOrderRequest request) {
        List<OrderLine> orderLines = new ArrayList<>();
        long totalAmount = 0;

        for (OrderItemRequest item : request.items()) {
            Product product = productRepository.findById(item.productId())
                    .orElseThrow(() -> new ProductNotFoundException(item.productId()));

            product.decreaseStock(item.quantity());

            OrderLine orderLine = new OrderLine(
                    product.getId(),
                    product.getName(),
                    product.getPrice(),
                    item.quantity()
            );
            orderLines.add(orderLine);
            totalAmount += product.getPrice() * item.quantity();
        }

        Order order = Order.create(memberId, orderLines, totalAmount);
        order.place();

        return OrderResponse.from(orderRepository.save(order));
    }

    /**
     * 주문 상세 조회 (본인 주문만)
     */
    public OrderResponse getOrder(Long memberId, UUID orderId) {
        Order order = findOrderByIdAndMemberId(orderId, memberId);
        return OrderResponse.from(order);
    }

    /**
     * 내 주문 목록 조회
     */
    public List<OrderResponse> getMyOrders(Long memberId) {
        return orderRepository.findByMemberIdWithOrderLines(memberId).stream()
                .map(OrderResponse::from)
                .toList();
    }

    /**
     * 주문 취소: 상태 검증 → 취소 → 재고 복원 (단일 트랜잭션)
     */
    @Transactional
    public OrderResponse cancelOrder(Long memberId, UUID orderId) {
        Order order = findOrderByIdAndMemberId(orderId, memberId);
        order.cancel();

        for (OrderLine line : order.getOrderLines()) {
            Product product = productRepository.findById(line.getProductId())
                    .orElseThrow(() -> new ProductNotFoundException(line.getProductId()));
            product.restoreStock(line.getQuantity());
        }

        return OrderResponse.from(order);
    }

    /**
     * 주문 조회 + 본인 확인 (타인 주문은 보안상 404 처리)
     */
    private Order findOrderByIdAndMemberId(UUID orderId, Long memberId) {
        Order order = orderRepository.findByIdWithOrderLines(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        if (!order.getMemberId().equals(memberId)) {
            throw new OrderNotFoundException(orderId);
        }
        return order;
    }
}
