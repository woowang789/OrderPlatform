package com.orderplatform.order.repository;

import com.orderplatform.order.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {

    @Query("SELECT DISTINCT o FROM Order o JOIN FETCH o.orderLines WHERE o.id = :id")
    Optional<Order> findByIdWithOrderLines(@Param("id") UUID id);

    @Query("SELECT DISTINCT o FROM Order o JOIN FETCH o.orderLines WHERE o.memberId = :memberId ORDER BY o.createdAt DESC")
    List<Order> findByMemberIdWithOrderLines(@Param("memberId") Long memberId);
}
