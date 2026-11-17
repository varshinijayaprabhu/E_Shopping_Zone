package com.eshopping.order.repository;

import com.eshopping.order.entity.Order;
import com.eshopping.order.entity.SagaState;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    @EntityGraph(attributePaths = {"orderItems", "paymentRecords"})
    Optional<Order> findByIdempotencyKey(String idempotencyKey);

    @EntityGraph(attributePaths = {"orderItems", "paymentRecords"})
    Optional<Order> findByOrderNumber(String orderNumber);

    @EntityGraph(attributePaths = {"orderItems", "paymentRecords"})
    Page<Order> findByCustomerIdOrderByCreatedAtDesc(Long customerId, Pageable pageable);

    @EntityGraph(attributePaths = {"orderItems", "paymentRecords"})
    Optional<Order> findByIdAndCustomerId(Long id, Long customerId);

    List<Order> findBySagaStateIn(Collection<SagaState> sagaStates);

    boolean existsByIdempotencyKey(String idempotencyKey);
}

