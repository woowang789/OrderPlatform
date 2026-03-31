package com.orderplatform.order.adapter.in.web;

import com.orderplatform.common.annotation.CurrentMemberId;
import com.orderplatform.order.adapter.in.web.dto.CreateOrderRequest;
import com.orderplatform.order.adapter.in.web.dto.OrderResponse;
import com.orderplatform.order.application.port.in.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final CreateOrderUseCase createOrderUseCase;
    private final CancelOrderUseCase cancelOrderUseCase;
    private final GetOrderUseCase getOrderUseCase;

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @CurrentMemberId Long memberId,
            @Valid @RequestBody CreateOrderRequest request) {

        CreateOrderCommand command = new CreateOrderCommand(
                memberId,
                request.items().stream()
                        .map(item -> new OrderItemCommand(
                                item.productId(), item.productName(),
                                item.price(), item.quantity()))
                        .toList(),
                request.paymentMethod()
        );

        OrderInfo info = createOrderUseCase.createOrder(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(OrderResponse.from(info));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrder(
            @CurrentMemberId Long memberId,
            @PathVariable UUID id) {
        OrderInfo info = getOrderUseCase.getOrder(memberId, id);
        return ResponseEntity.ok(OrderResponse.from(info));
    }

    @GetMapping
    public ResponseEntity<List<OrderResponse>> getMyOrders(
            @CurrentMemberId Long memberId) {
        List<OrderResponse> responses = getOrderUseCase.getMyOrders(memberId).stream()
                .map(OrderResponse::from)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<OrderResponse> cancelOrder(
            @CurrentMemberId Long memberId,
            @PathVariable UUID id) {
        CancelOrderCommand command = new CancelOrderCommand(id, memberId);
        OrderInfo info = cancelOrderUseCase.cancelOrder(command);
        return ResponseEntity.ok(OrderResponse.from(info));
    }
}
