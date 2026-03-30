package com.orderplatform.payment.domain.model;

import com.orderplatform.payment.domain.exception.InvalidPaymentStatusException;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Payment 도메인 모델 — 순수 Java 객체 (JPA/Spring 의존 없음)
 */
public class Payment {

    private final UUID id;
    private final UUID orderId;
    private final Long memberId;
    private final long amount;
    private final PaymentMethod method;
    private PaymentStatus status;
    private String pgTxnId;
    private String failReason;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    private Payment(UUID id, UUID orderId, Long memberId, long amount,
                    PaymentMethod method, PaymentStatus status,
                    String pgTxnId, String failReason,
                    LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.orderId = orderId;
        this.memberId = memberId;
        this.amount = amount;
        this.method = method;
        this.status = status;
        this.pgTxnId = pgTxnId;
        this.failReason = failReason;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /**
     * 신규 결제 생성 — id와 timestamp는 영속화 시 설정
     */
    public static Payment create(UUID orderId, Long memberId, long amount, PaymentMethod method) {
        if (orderId == null) {
            throw new IllegalArgumentException("주문 ID는 필수입니다.");
        }
        if (amount <= 0) {
            throw new IllegalArgumentException("결제 금액은 0보다 커야 합니다.");
        }
        return new Payment(null, orderId, memberId, amount, method,
                PaymentStatus.PENDING, null, null, null, null);
    }

    /**
     * DB에서 복원 — 모든 필드를 포함한 완전한 상태 복원
     */
    public static Payment reconstitute(UUID id, UUID orderId, Long memberId, long amount,
                                       PaymentMethod method, PaymentStatus status,
                                       String pgTxnId, String failReason,
                                       LocalDateTime createdAt, LocalDateTime updatedAt) {
        return new Payment(id, orderId, memberId, amount, method, status,
                pgTxnId, failReason, createdAt, updatedAt);
    }

    /**
     * 결제 완료 (PENDING → COMPLETED)
     */
    public void complete(String pgTxnId) {
        if (this.status != PaymentStatus.PENDING) {
            throw new InvalidPaymentStatusException(this.status);
        }
        this.status = PaymentStatus.COMPLETED;
        this.pgTxnId = pgTxnId;
    }

    /**
     * 결제 실패 (PENDING → FAILED)
     */
    public void fail(String reason) {
        if (this.status != PaymentStatus.PENDING) {
            throw new InvalidPaymentStatusException(this.status);
        }
        this.status = PaymentStatus.FAILED;
        this.failReason = reason;
    }

    /**
     * 결제 취소 (COMPLETED → CANCELLED)
     */
    public void cancel() {
        if (this.status != PaymentStatus.COMPLETED) {
            throw new InvalidPaymentStatusException(this.status);
        }
        this.status = PaymentStatus.CANCELLED;
    }

    public UUID getId() {
        return id;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public Long getMemberId() {
        return memberId;
    }

    public long getAmount() {
        return amount;
    }

    public PaymentMethod getMethod() {
        return method;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public String getPgTxnId() {
        return pgTxnId;
    }

    public String getFailReason() {
        return failReason;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
