package com.eshopping.order.repository;

import com.eshopping.order.entity.OrderItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    @Query(value = "SELECT oi FROM OrderItem oi JOIN FETCH oi.order o WHERE oi.merchantId = :merchantId ORDER BY o.createdAt DESC",
           countQuery = "SELECT count(oi) FROM OrderItem oi WHERE oi.merchantId = :merchantId")
    Page<OrderItem> findByMerchantIdWithOrder(@Param("merchantId") Long merchantId, Pageable pageable);

    List<OrderItem> findByOrderId(Long orderId);
}

