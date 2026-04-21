package com.ion.llm.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.util.List;

@Builder
public record ChatCompletionRequest(
        String model,
        List<Message> messages,
        boolean stream,
        double temperature,
        @JsonProperty("max_tokens") int maxTokens
) {
    public record Message(String role, String content) {}
}
