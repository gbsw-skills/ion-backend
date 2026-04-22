package com.ion.llm.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(LlmBootstrapProperties.class)
public class LlmBootstrapConfig {
}
