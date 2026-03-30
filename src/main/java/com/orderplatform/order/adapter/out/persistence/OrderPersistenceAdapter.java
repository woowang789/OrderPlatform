package com.orderplatform.order.adapter.out.persistence;

import com.orderplatform.order.application.port.out.LoadOrderPort;
import com.orderplatform.order.application.port.out.SaveOrderPort;
import com.orderplatform.order.domain.model.Order;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@SuppressWarnings("null")
@Component
@RequiredArgsConstructor
public class OrderPersistenceAdapter implements LoadOrderPort, SaveOrderPort {

    private final OrderJpaRepository orderJpaRepository;

    @Override
    public Optional<Order> findById(UUID orderId) {
        return orderJpaRepository.findByIdWithOrderLines(orderId)
                .map(OrderMapper::toDomain);
    }

    @Override
    public Optional<Order> findByIdWithLock(UUID orderId) {
        return orderJpaRepository.findByIdForUpdate(orderId)
                .map(OrderMapper::toDomain);
    }

    @Override
    public List<Order> findAllByMemberId(Long memberId) {
        return orderJpaRepository.findByMemberIdWithOrderLines(memberId).stream()
                .map(OrderMapper::toDomain)
                .toList();
    }

    @Override
    public Order save(Order order) {
        if (order.getId() == null) {
            // 신규 생성
            OrderJpaEntity entity = OrderMapper.toJpaEntity(order);
            OrderJpaEntity saved = orderJpaRepository.save(entity);
            return OrderMapper.toDomain(saved);
        } else {
            // 업데이트 — 영속 상태의 엔티티를 조회하여 필드 갱신 (dirty checking)
            OrderJpaEntity entity = orderJpaRepository.findByIdWithOrderLines(order.getId())
                    .orElseThrow();
            entity.updateFrom(order.getStatus());
            return OrderMapper.toDomain(entity);
        }
    }
}
