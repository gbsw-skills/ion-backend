package com.ion.chat.service;

import com.ion.chat.domain.ChatSession;
import com.ion.chat.repository.ChatSessionRepository;
import com.ion.common.exception.ErrorCode;
import com.ion.common.exception.IonException;
import com.ion.llm.client.OpenAiCompatibleClient;
import com.ion.llm.domain.LlmEndpointConfig;
import com.ion.llm.dto.ChatCompletionRequest;
import com.ion.llm.service.LlmEndpointConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatTitleService {

    private static final String DEFAULT_SESSION_TITLE = "새 대화";
    private static final int MAX_TITLE_LENGTH = 30;

    private final ChatSessionRepository sessionRepository;
    private final LlmEndpointConfigService llmEndpointConfigService;
    private final OpenAiCompatibleClient llmClient;

    @Async("llmTaskExecutor")
    public void generateTitleAsync(UUID sessionId, String userQuestion) {
        try {
            LlmEndpointConfig endpoint = llmEndpointConfigService.getDefaultActiveEndpoint();
            ChatCompletionRequest request = ChatCompletionRequest.builder()
                    .model(endpoint.getModel())
                    .messages(List.of(
                            new ChatCompletionRequest.Message("system", """
                                    당신은 채팅방 제목 생성기입니다.
                                    사용자의 질문을 한국어 제목 하나로 요약하세요.
                                    답변에는 제목만 포함하세요.
                                    따옴표, 마침표, 설명 문장은 쓰지 마세요.
                                    """),
                            new ChatCompletionRequest.Message("user", userQuestion)
                    ))
                    .stream(false)
                    .temperature(0.2)
                    .maxTokens(Math.min(endpoint.getMaxTokens(), 64))
                    .build();

            String title = llmClient.chat(endpoint, request).block();
            updateTitleIfDefault(sessionId, sanitizeTitle(title, userQuestion));
        } catch (IonException e) {
            if (e.getErrorCode() == ErrorCode.LLM_003) {
                log.debug("Skipping chat title generation because no default LLM endpoint is active");
                return;
            }
            log.warn("Chat title generation failed for session {}: {}", sessionId, e.getErrorCode().name());
        } catch (Exception e) {
            log.warn("Chat title generation failed for session {}: {}", sessionId, e.getMessage());
        }
    }

    @Transactional
    public void updateTitleIfDefault(UUID sessionId, String title) {
        ChatSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IonException(ErrorCode.CHAT_001));
        if (DEFAULT_SESSION_TITLE.equals(session.getTitle())) {
            session.updateTitle(title);
            sessionRepository.save(session);
        }
    }

    private String sanitizeTitle(String title, String fallbackQuestion) {
        String normalized = (title == null || title.isBlank() ? fallbackQuestion : title)
                .strip()
                .replaceAll("[\\r\\n]+", " ")
                .replaceAll("\\s+", " ");

        normalized = stripWrappingQuotes(normalized);
        if (normalized.endsWith(".")) {
            normalized = normalized.substring(0, normalized.length() - 1).strip();
        }

        if (normalized.isBlank()) {
            normalized = fallbackQuestion.strip();
        }

        if (normalized.codePointCount(0, normalized.length()) <= MAX_TITLE_LENGTH) {
            return normalized;
        }

        int endIndex = normalized.offsetByCodePoints(0, MAX_TITLE_LENGTH);
        return normalized.substring(0, endIndex);
    }

    private String stripWrappingQuotes(String value) {
        if (value.length() < 2) {
            return value;
        }
        char first = value.charAt(0);
        char last = value.charAt(value.length() - 1);
        if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
            return value.substring(1, value.length() - 1).strip();
        }
        return value;
    }
}
