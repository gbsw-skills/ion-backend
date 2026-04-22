package com.ion.admin.dto;

import com.ion.llm.domain.LlmEndpointConfig;

import java.time.Instant;

public record LlmEndpointResponse(
        Long id,
        String name,
        String baseUrl,
        String apiKey,
        String model,
        String systemPrompt,
        double temperature,
        int maxTokens,
        boolean enabled,
        boolean isDefault,
        Instant createdAt,
        Instant updatedAt
) {
    public static LlmEndpointResponse from(LlmEndpointConfig config) {
        return new LlmEndpointResponse(
                config.getId(),
                config.getName(),
                config.getBaseUrl(),
                config.getApiKey(),
                config.getModel(),
                config.getSystemPrompt(),
                config.getTemperature(),
                config.getMaxTokens(),
                config.isEnabled(),
                config.isDefault(),
                config.getCreatedAt(),
                config.getUpdatedAt()
        );
    }
}
