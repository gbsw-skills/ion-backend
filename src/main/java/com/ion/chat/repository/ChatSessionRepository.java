package com.ion.chat.repository;

import com.ion.chat.domain.ChatSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ChatSessionRepository extends JpaRepository<ChatSession, UUID> {
    Page<ChatSession> findByUserIdOrderByLastActiveAtDesc(Long userId, Pageable pageable);
    Optional<ChatSession> findByIdAndUserId(UUID id, Long userId);
}
