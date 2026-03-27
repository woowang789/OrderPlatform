package com.orderplatform.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public record CreateProductRequest(
        @NotBlank(message = "상품명은 필수입니다.")
        String name,

        @Positive(message = "가격은 양수여야 합니다.")
        int price,

        @PositiveOrZero(message = "재고는 0 이상이어야 합니다.")
        int stock,

        String category
) {
}
