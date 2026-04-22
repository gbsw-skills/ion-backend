package com.ion.llm.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ion.common.exception.ErrorCode;
import com.ion.common.exception.IonException;
import com.ion.llm.domain.LlmEndpointConfig;
import com.ion.llm.dto.ChatCompletionChunk;
import com.ion.llm.dto.ChatCompletionRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

@Slf4j
@Component
public class OpenAiCompatibleClient {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final WebClient.Builder webClientBuilder;

    public OpenAiCompatibleClient(WebClient.Builder webClientBuilder) {
        this.webClientBuilder = webClientBuilder;
    }

    public Flux<String> streamChat(LlmEndpointConfig endpoint, ChatCompletionRequest request) {
        return buildClient(endpoint).post()
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

    private WebClient buildClient(LlmEndpointConfig endpoint) {
        return webClientBuilder
                .baseUrl(endpoint.getBaseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + endpoint.getApiKey())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                .codecs(configurer -> configurer.defaultCodecs()
                        .maxInMemorySize(10 * 1024 * 1024))
                .build();
    }
}
