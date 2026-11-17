package com.eshopping.order.repository;

import com.eshopping.order.entity.PaymentRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentRecordRepository extends JpaRepository<PaymentRecord, Long> {

    Optional<PaymentRecord> findByOrderId(Long orderId);

    Optional<PaymentRecord> findByPaymentReference(String paymentReference);
}

