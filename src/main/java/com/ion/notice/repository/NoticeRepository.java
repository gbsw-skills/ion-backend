package com.ion.notice.repository;

import com.ion.notice.domain.Notice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NoticeRepository extends JpaRepository<Notice, Long> {
    Page<Notice> findAllByOrderByPublishedAtDesc(Pageable pageable);
    Page<Notice> findByTitleContainingIgnoreCaseOrderByPublishedAtDesc(String keyword, Pageable pageable);
}
