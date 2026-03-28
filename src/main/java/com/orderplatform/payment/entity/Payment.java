package com.orderplatform.payment.entity;

import com.orderplatform.common.entity.BaseEntity;
import com.orderplatform.common.exception.InvalidPaymentStatusException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * 결제 Aggregate Root 엔티티
 */
@Entity
@Table(name = "payments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID orderId;

    @Column(nullable = false)
    private Long memberId;

    @Column(nullable = false)
    private long amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PaymentMethod method;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PaymentStatus status;

    @Column(length = 200)
    private String pgTxnId;

    @Column(length = 500)
    private String failReason;

    @Version
    private Long version;

    private Payment(UUID orderId, Long memberId, long amount, PaymentMethod method) {
        this.orderId = orderId;
        this.memberId = memberId;
        this.amount = amount;
        this.method = method;
        this.status = PaymentStatus.PENDING;
    }

    /**
     * 결제 생성 정적 팩토리
     */
    public static Payment create(UUID orderId, Long memberId, long amount, PaymentMethod method) {
        if (orderId == null) {
            throw new IllegalArgumentException("주문 ID는 필수입니다.");
        }
        if (amount <= 0) {
            throw new IllegalArgumentException("결제 금액은 0보다 커야 합니다.");
        }
        return new Payment(orderId, memberId, amount, method);
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
}
