package com.orderplatform.common.domain.event;

/**
 * Kafka 토픽 상수 정의.
 * 모든 토픽은 orderId를 파티션 키로 사용하여 동일 주문 이벤트의 순서를 보장한다.
 */
public final class Topics {

    private Topics() {
        // 인스턴스 생성 방지
    }

    // 주문 이벤트
    public static final String ORDER_PLACED = "order.placed";
    public static final String ORDER_CONFIRMED = "order.confirmed";
    public static final String ORDER_CANCELLED = "order.cancelled";

    // 결제 이벤트
    public static final String PAYMENT_COMPLETED = "payment.completed";
    public static final String PAYMENT_FAILED = "payment.failed";
    public static final String PAYMENT_CANCEL_REQUESTED = "payment.cancel.requested";
    public static final String PAYMENT_CANCELLED = "payment.cancelled";

    // 재고 이벤트
    public static final String STOCK_DEDUCTED = "stock.deducted";
    public static final String STOCK_DEDUCTION_FAILED = "stock.deduction.failed";
}
