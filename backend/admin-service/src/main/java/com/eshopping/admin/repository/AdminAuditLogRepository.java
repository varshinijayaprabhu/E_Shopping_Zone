package com.eshopping.admin.repository;

import com.eshopping.admin.entity.AdminAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdminAuditLogRepository extends JpaRepository<AdminAuditLog, Long> {

    Page<AdminAuditLog> findAllByOrderByCreatedAtDesc(Pageable pageable);

    List<AdminAuditLog> findByAdminIdOrderByCreatedAtDesc(Long adminId);

    List<AdminAuditLog> findByTargetEntityTypeAndTargetEntityId(String targetEntityType, Long targetEntityId);
}
