package com.ion.llm.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ion.llm.bootstrap")
public record LlmBootstrapProperties(
        String name,
        String baseUrl,
        String apiKey,
        String model,
        String systemPrompt,
        double temperature,
        int maxTokens
) {
}
