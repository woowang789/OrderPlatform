package com.orderplatform.order.adapter.out.persistence;

import com.orderplatform.common.entity.BaseEntity;
import com.orderplatform.order.domain.model.OrderStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "orders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderJpaEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private Long memberId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OrderStatus status;

    @Column(nullable = false)
    private long totalAmount;

    @Version
    private Long version;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderLineJpaEntity> orderLines = new ArrayList<>();

    public OrderJpaEntity(Long memberId, OrderStatus status, long totalAmount) {
        this.memberId = memberId;
        this.status = status;
        this.totalAmount = totalAmount;
    }

    /**
     * 도메인 모델 변경사항을 JPA 엔티티에 반영 (Adapter save 시 사용)
     */
    public void updateFrom(OrderStatus status) {
        this.status = status;
    }

    public void addOrderLine(OrderLineJpaEntity orderLine) {
        this.orderLines.add(orderLine);
        orderLine.setOrder(this);
    }

    void clearOrderLines() {
        this.orderLines.clear();
    }
}
