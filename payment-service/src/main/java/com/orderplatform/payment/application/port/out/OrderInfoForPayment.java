package com.orderplatform.payment.application.port.out;

import java.util.UUID;

/**
 * Payment 컨텍스트에서 필요한 주문 정보 (크로스 컨텍스트 DTO)
 */
public record OrderInfoForPayment(
        UUID orderId,
        Long memberId,
        long totalAmount,
        String status
) {
}
