package com.ion.llm.config;

import com.ion.llm.domain.LlmEndpointConfig;
import com.ion.llm.repository.LlmEndpointConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class LlmEndpointBootstrapInitializer implements ApplicationRunner {

    private final LlmEndpointConfigRepository repository;
    private final LlmBootstrapProperties properties;

    @Override
    public void run(ApplicationArguments args) {
        if (repository.count() > 0) {
            return;
        }

        repository.save(LlmEndpointConfig.builder()
                .name(properties.name())
                .baseUrl(normalizeBaseUrl(properties.baseUrl()))
                .apiKey(properties.apiKey())
                .model(properties.model())
                .systemPrompt(properties.systemPrompt())
                .temperature(properties.temperature())
                .maxTokens(properties.maxTokens())
                .enabled(true)
                .isDefault(true)
                .build());

        log.info("Bootstrapped default LLM endpoint config from ion.llm.bootstrap.* properties");
    }

    private String normalizeBaseUrl(String baseUrl) {
        if (baseUrl.endsWith("/")) {
            return baseUrl.substring(0, baseUrl.length() - 1);
        }
        return baseUrl;
    }
}
