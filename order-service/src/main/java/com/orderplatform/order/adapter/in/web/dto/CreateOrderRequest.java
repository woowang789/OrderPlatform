package com.orderplatform.order.adapter.in.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record CreateOrderRequest(
        @NotEmpty(message = "주문 항목은 최소 1개 이상이어야 합니다.")
        List<@Valid OrderItemRequest> items,

        @NotBlank(message = "결제 방법은 필수입니다.")
        String paymentMethod
) {
}
