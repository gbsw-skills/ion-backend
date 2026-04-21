package com.ion.llm.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ion.common.exception.ErrorCode;
import com.ion.common.exception.IonException;
import com.ion.llm.dto.ChatCompletionChunk;
import com.ion.llm.dto.ChatCompletionRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

@Slf4j
@Component
public class OpenAiCompatibleClient {

    private final WebClient llmWebClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OpenAiCompatibleClient(WebClient llmWebClient) {
        this.llmWebClient = llmWebClient;
    }

    public Flux<String> streamChat(ChatCompletionRequest request) {
        return llmWebClient.post()
                .uri("/v1/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .bodyValue(request)
                .retrieve()
                .bodyToFlux(String.class)
                .flatMap(raw -> Flux.fromArray(raw.split("\n")))
                .filter(line -> line.startsWith("data: "))
                .map(line -> line.substring(6).trim())
                .takeWhile(data -> !data.equals("[DONE]"))
                .mapNotNull(data -> {
                    try {
                        ChatCompletionChunk chunk = objectMapper.readValue(data, ChatCompletionChunk.class);
                        if (chunk.choices() != null && !chunk.choices().isEmpty()) {
                            ChatCompletionChunk.Delta delta = chunk.choices().get(0).delta();
                            return (delta != null) ? delta.content() : null;
                        }
                        return null;
                    } catch (JsonProcessingException e) {
                        log.debug("Skipping non-JSON SSE line: {}", data);
                        return null;
                    }
                })
                .filter(content -> content != null && !content.isEmpty())
                .onErrorMap(e -> {
                    if (e instanceof IonException) return e;
                    log.error("LLM streaming error: {}", e.getMessage());
                    return new IonException(ErrorCode.LLM_001);
                });
    }
}
