package com.ion.chat.service;

import com.ion.chat.domain.ChatMessage;
import com.ion.chat.repository.ChatMessageRepository;
import com.ion.llm.client.OpenAiCompatibleClient;
import com.ion.llm.domain.LlmEndpointConfig;
import com.ion.llm.dto.ChatCompletionRequest;
import com.ion.llm.service.LlmEndpointConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class LlmProcessingService {

    private final ChatMessageRepository messageRepository;
    private final OpenAiCompatibleClient llmClient;
    private final SseEmitterService sseEmitterService;
    private final LlmEndpointConfigService llmEndpointConfigService;

    @Async("llmTaskExecutor")
    public void processAsync(UUID sessionId) {
        log.debug("Starting LLM processing for session {}", sessionId);

        LlmEndpointConfig endpoint = llmEndpointConfigService.getDefaultActiveEndpoint();
        List<ChatCompletionRequest.Message> messages = buildMessages(sessionId, endpoint.getSystemPrompt());

        ChatCompletionRequest request = ChatCompletionRequest.builder()
                .model(endpoint.getModel())
                .messages(messages)
                .stream(true)
                .temperature(endpoint.getTemperature())
                .maxTokens(endpoint.getMaxTokens())
                .build();

        StringBuilder fullContent = new StringBuilder();

        llmClient.streamChat(endpoint, request)
                .doOnNext(token -> {
                    fullContent.append(token);
                    sseEmitterService.sendToken(sessionId, token);
                })
                .doOnComplete(() -> {
                    Long messageId = saveAssistantMessage(sessionId, fullContent.toString());
                    sseEmitterService.sendDone(sessionId, messageId);
                    log.debug("LLM processing completed for session {}", sessionId);
                })
                .doOnError(e -> {
                    log.error("LLM processing failed for session {} with endpoint {}: {}", sessionId, endpoint.getName(), e.getMessage());
                    String errorCode = (e instanceof com.ion.common.exception.IonException ionException)
                            ? ionException.getErrorCode().name()
                            : "LLM_001";
                    String message = (e instanceof com.ion.common.exception.IonException ionException)
                            ? ionException.getErrorCode().getMessage()
                            : "AI 응답 생성에 실패했습니다.";
                    sseEmitterService.sendError(sessionId, errorCode, message);
                })
                .subscribe();
    }

    private List<ChatCompletionRequest.Message> buildMessages(UUID sessionId, String systemPrompt) {
        List<ChatMessage> history = messageRepository.findTop20BySessionIdOrderByCreatedAtDesc(sessionId);

        List<ChatCompletionRequest.Message> messages = new ArrayList<>();
        messages.add(new ChatCompletionRequest.Message("system", systemPrompt));

        // 오래된 순으로 재정렬
        for (int i = history.size() - 1; i >= 0; i--) {
            ChatMessage msg = history.get(i);
            messages.add(new ChatCompletionRequest.Message(msg.getRole().name(), msg.getContent()));
        }
        return messages;
    }

    @Transactional
    public Long saveAssistantMessage(UUID sessionId, String content) {
        ChatMessage message = messageRepository.save(
                ChatMessage.builder()
                        .sessionId(sessionId)
                        .role(ChatMessage.Role.assistant)
                        .content(content)
                        .build()
        );
        return message.getId();
    }
}
