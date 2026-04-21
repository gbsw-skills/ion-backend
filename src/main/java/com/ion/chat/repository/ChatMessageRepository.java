package com.ion.chat.repository;

import com.ion.chat.domain.ChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    Page<ChatMessage> findBySessionIdOrderByCreatedAtAsc(UUID sessionId, Pageable pageable);
    List<ChatMessage> findTop20BySessionIdOrderByCreatedAtDesc(UUID sessionId);
}
