package com.eshopping.admin.repository;

import com.eshopping.admin.entity.DisputeStatus;
import com.eshopping.admin.entity.DisputeTicket;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface DisputeTicketRepository extends JpaRepository<DisputeTicket, Long> {

    List<DisputeTicket> findByCustomerIdOrderByCreatedAtDesc(Long customerId);

    List<DisputeTicket> findByMerchantIdOrderByCreatedAtDesc(Long merchantId);

    Page<DisputeTicket> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<DisputeTicket> findByStatusOrderByCreatedAtDesc(DisputeStatus status, Pageable pageable);

    List<DisputeTicket> findByStatusAndMerchantDeadlineBefore(DisputeStatus status, LocalDateTime deadline);

    Optional<DisputeTicket> findByOrderId(Long orderId);

    long countByStatus(DisputeStatus status);
}
