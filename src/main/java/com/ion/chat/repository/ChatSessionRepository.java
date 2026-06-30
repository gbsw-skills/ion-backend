package com.ion.chat.repository;

import com.ion.chat.domain.ChatSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ChatSessionRepository extends JpaRepository<ChatSession, UUID> {
    Page<ChatSession> findByUserIdOrderByLastActiveAtDesc(Long userId, Pageable pageable);
    Optional<ChatSession> findByIdAndUserId(UUID id, Long userId);

    @Query("""
            select distinct s
            from ChatSession s
            left join ChatMessage m on m.sessionId = s.id
            where s.user.id = :userId
              and (
                  lower(s.title) like lower(concat('%', :query, '%'))
                  or lower(m.content) like lower(concat('%', :query, '%'))
              )
            order by s.lastActiveAt desc
            """)
    Page<ChatSession> searchByUserIdAndQuery(
            @Param("userId") Long userId,
            @Param("query") String query,
            Pageable pageable
    );
}
