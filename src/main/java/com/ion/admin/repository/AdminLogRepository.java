package com.ion.admin.repository;

import com.ion.admin.domain.AdminLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminLogRepository extends JpaRepository<AdminLog, Long> {
    Page<AdminLog> findByAdminIdOrderByCreatedAtDesc(Long adminId, Pageable pageable);
    Page<AdminLog> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
