package com.ion.chat.service;

import com.ion.chat.domain.ChatMessage;
import com.ion.chat.domain.ChatSession;
import com.ion.chat.dto.MessageResponse;
import com.ion.chat.dto.SendMessageRequest;
import com.ion.chat.dto.SessionResponse;
import com.ion.chat.repository.ChatMessageRepository;
import com.ion.chat.repository.ChatSessionRepository;
import com.ion.common.exception.ErrorCode;
import com.ion.common.exception.IonException;
import com.ion.common.response.PageResponse;
import com.ion.user.domain.User;
import com.ion.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatSessionRepository sessionRepository;
    private final ChatMessageRepository messageRepository;
    private final UserRepository userRepository;
    private final SseEmitterService sseEmitterService;
    private final LlmProcessingService llmProcessingService;

    @Transactional
    public SessionResponse createSession(Long userId) {
        User user = userRepository.getReferenceById(userId);
        ChatSession session = sessionRepository.save(
                ChatSession.builder()
                        .user(user)
                        .title("새 대화")
                        .lastActiveAt(Instant.now())
                        .build()
        );
        return toSessionResponse(session);
    }

    @Transactional(readOnly = true)
    public PageResponse<SessionResponse> getSessions(Long userId, int page, int size) {
        return PageResponse.from(
                sessionRepository.findByUserIdOrderByLastActiveAtDesc(userId, PageRequest.of(page, size))
                        .map(this::toSessionResponse)
        );
    }

    @Transactional
    public void deleteSession(UUID sessionId, Long userId) {
        ChatSession session = sessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new IonException(ErrorCode.CHAT_001));
        sessionRepository.delete(session);
    }

    @Transactional
    public MessageResponse sendMessage(UUID sessionId, Long userId, SendMessageRequest request) {
        ChatSession session = sessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new IonException(ErrorCode.CHAT_001));

        ChatMessage userMessage = messageRepository.save(
                ChatMessage.builder()
                        .sessionId(sessionId)
                        .role(ChatMessage.Role.user)
                        .content(request.content())
                        .build()
        );

        session.updateLastActive();
        sessionRepository.save(session);

        return toMessageResponse(userMessage);
    }

    // 트랜잭션 커밋 후 컨트롤러에서 호출
    public void triggerLlmProcessing(UUID sessionId) {
        llmProcessingService.processAsync(sessionId);
    }

    public SseEmitter createSseEmitter(UUID sessionId, Long userId) {
        sessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new IonException(ErrorCode.CHAT_001));
        return sseEmitterService.create(sessionId);
    }

    @Transactional(readOnly = true)
    public PageResponse<MessageResponse> getMessages(UUID sessionId, Long userId, int page, int size) {
        sessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new IonException(ErrorCode.CHAT_001));
        return PageResponse.from(
                messageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId, PageRequest.of(page, size))
                        .map(this::toMessageResponse)
        );
    }

    private SessionResponse toSessionResponse(ChatSession s) {
        return new SessionResponse(s.getId(), s.getTitle(), s.getCreatedAt(), s.getLastActiveAt());
    }

    private MessageResponse toMessageResponse(ChatMessage m) {
        return new MessageResponse(m.getId(), m.getSessionId(), m.getRole().name(), m.getContent(), m.getCreatedAt());
    }
}
