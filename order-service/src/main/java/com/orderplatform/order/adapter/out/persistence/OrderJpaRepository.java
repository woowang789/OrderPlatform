package com.orderplatform.order.adapter.out.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderJpaRepository extends JpaRepository<OrderJpaEntity, UUID> {

    @Query("SELECT DISTINCT o FROM OrderJpaEntity o JOIN FETCH o.orderLines WHERE o.id = :id")
    Optional<OrderJpaEntity> findByIdWithOrderLines(@Param("id") UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM OrderJpaEntity o WHERE o.id = :id")
    Optional<OrderJpaEntity> findByIdForUpdate(@Param("id") UUID id);

    @Query("SELECT DISTINCT o FROM OrderJpaEntity o JOIN FETCH o.orderLines WHERE o.memberId = :memberId ORDER BY o.createdAt DESC")
    List<OrderJpaEntity> findByMemberIdWithOrderLines(@Param("memberId") Long memberId);
}
