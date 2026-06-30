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
    private static final int MAX_TITLE_LENGTH = 15;

    private final ChatSessionRepository sessionRepository;
    private final LlmEndpointConfigService llmEndpointConfigService;
    private final OpenAiCompatibleClient llmClient;
    private final SseEmitterService sseEmitterService;

    @Async("llmTaskExecutor")
    public void generateTitleAsync(UUID sessionId, String userQuestion) {
        try {
            LlmEndpointConfig endpoint = llmEndpointConfigService.getDefaultActiveEndpoint();
            String title = requestTitle(endpoint, userQuestion, false);
            String sanitizedTitle = sanitizeTitle(title, userQuestion);
            if (sanitizedTitle == null) {
                title = requestTitle(endpoint, userQuestion, true);
                sanitizedTitle = sanitizeTitle(title, userQuestion);
            }
            if (sanitizedTitle == null) {
                throw new IonException(ErrorCode.LLM_001);
            }

            updateTitleIfDefault(sessionId, sanitizedTitle);
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

    private String requestTitle(LlmEndpointConfig endpoint, String userQuestion, boolean retry) {
        ChatCompletionRequest request = ChatCompletionRequest.builder()
                .model(endpoint.getModel())
                .messages(List.of(
                        new ChatCompletionRequest.Message("system", retry ? retrySystemPrompt() : systemPrompt()),
                        new ChatCompletionRequest.Message("user", retry ? retryUserPrompt(userQuestion) : userPrompt(userQuestion))
                ))
                .stream(false)
                .temperature(0.1)
                .maxTokens(Math.min(endpoint.getMaxTokens(), 64))
                .build();

        return llmClient.chat(endpoint, request).block();
    }

    @Transactional
    public void updateTitleIfDefault(UUID sessionId, String title) {
        ChatSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IonException(ErrorCode.CHAT_001));
        if (DEFAULT_SESSION_TITLE.equals(session.getTitle())) {
            session.updateTitle(title);
            sessionRepository.save(session);
            sseEmitterService.sendTitle(sessionId, title);
        }
    }

    private String sanitizeTitle(String title, String originalQuestion) {
        if (title == null || title.isBlank()) {
            return null;
        }

        String normalized = title.strip()
                .replaceAll("[\\r\\n]+", " ")
                .replaceAll("\\s+", " ");

        normalized = stripWrappingQuotes(normalized);
        normalized = stripTitlePrefix(normalized);
        if (normalized.endsWith(".")) {
            normalized = normalized.substring(0, normalized.length() - 1).strip();
        }
        if (normalized.endsWith("?")) {
            normalized = normalized.substring(0, normalized.length() - 1).strip();
        }

        if (normalized.isBlank() || isSameAsQuestion(normalized, originalQuestion)) {
            return null;
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

    private String stripTitlePrefix(String value) {
        return value.replaceFirst("^(제목|채팅 제목|요약 제목)\\s*[:：]\\s*", "").strip();
    }

    private String systemPrompt() {
        return """
                당신은 채팅방 제목 생성기입니다.
                사용자의 질문을 한국어 명사형 제목 하나로 요약하세요.
                질문 문장을 그대로 복사하지 말고 핵심 주제만 남기세요.
                제목은 15자 이내로 작성하세요.
                답변에는 제목만 포함하세요.
                따옴표, 마침표, 물음표, 설명 문장은 쓰지 마세요.
                """;
    }

    private String retrySystemPrompt() {
        return """
                당신은 채팅방 제목 생성기입니다.
                이전 응답이 질문 원문과 같아서 실패했습니다.
                질문 문장을 절대 그대로 쓰지 말고 핵심 주제만 15자 이내 명사형 제목으로 바꾸세요.
                답변에는 제목만 포함하세요.
                """;
    }

    private String userPrompt(String userQuestion) {
        return "질문: " + userQuestion + "\n제목:";
    }

    private String retryUserPrompt(String userQuestion) {
        return "아래 질문을 그대로 복사하지 말고 채팅 제목으로 요약하세요.\n질문: " + userQuestion + "\n요약 제목:";
    }

    private boolean isSameAsQuestion(String title, String question) {
        return normalizeForComparison(title).equals(normalizeForComparison(question));
    }

    private String normalizeForComparison(String value) {
        if (value == null) {
            return "";
        }
        return value.strip()
                .replaceAll("[\\s\\p{Punct}]+", "")
                .toLowerCase();
    }
}
