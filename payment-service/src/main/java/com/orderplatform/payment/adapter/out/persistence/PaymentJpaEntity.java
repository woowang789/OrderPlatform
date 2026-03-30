package com.orderplatform.payment.adapter.out.persistence;

import com.orderplatform.common.entity.BaseEntity;
import com.orderplatform.payment.domain.model.PaymentMethod;
import com.orderplatform.payment.domain.model.PaymentStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "payments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentJpaEntity extends BaseEntity {

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

    public PaymentJpaEntity(UUID orderId, Long memberId, long amount,
                            PaymentMethod method, PaymentStatus status,
                            String pgTxnId, String failReason) {
        this.orderId = orderId;
        this.memberId = memberId;
        this.amount = amount;
        this.method = method;
        this.status = status;
        this.pgTxnId = pgTxnId;
        this.failReason = failReason;
    }

    /**
     * 도메인 모델 변경사항을 JPA 엔티티에 반영 (Adapter save 시 사용)
     */
    void updateFrom(PaymentStatus status, String pgTxnId, String failReason) {
        this.status = status;
        this.pgTxnId = pgTxnId;
        this.failReason = failReason;
    }
}
