package com.orderplatform.order.domain.model;

/**
 * 주문 상태 열거형
 */
public enum OrderStatus {

    CREATED,    // 주문 생성됨
    PLACED,     // 주문 확정
    PAID,       // 결제 완료
    CONFIRMED,  // 재고 확정 (Phase 3: stock.deducted 수신 시)
    CANCELLED   // 주문 취소
}
