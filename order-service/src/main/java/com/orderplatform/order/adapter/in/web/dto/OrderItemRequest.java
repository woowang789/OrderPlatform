package com.orderplatform.order.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record OrderItemRequest(
        @NotNull(message = "상품 ID는 필수입니다.")
        Long productId,

        @NotBlank(message = "상품명은 필수입니다.")
        String productName,

        @Positive(message = "가격은 양수여야 합니다.")
        long price,

        @Positive(message = "수량은 양수여야 합니다.")
        int quantity
) {
}
