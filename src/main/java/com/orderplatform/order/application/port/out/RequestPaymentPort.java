package com.orderplatform.order.application.port.out;

import java.util.UUID;

/**
 * 결제 요청 Outbound Port — 의도적 동기 호출
 * Port 추상화 뒤에서도 장애 전파 문제를 체감하기 위한 설계.
 * Phase 3에서 Kafka 이벤트 기반 비동기 호출로 전환 예정.
 */
public interface RequestPaymentPort {

    void requestPayment(UUID orderId, Long memberId, long totalAmount);
}
