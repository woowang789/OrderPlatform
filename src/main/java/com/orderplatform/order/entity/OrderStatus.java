package com.orderplatform.order.entity;

/**
 * 주문 상태 열거형 (Phase 1: 기본 4개 상태)
 */
public enum OrderStatus {

    CREATED,    // 주문 생성됨
    PLACED,     // 주문 확정
    PAID,       // 결제 완료
    CANCELLED   // 주문 취소
}
