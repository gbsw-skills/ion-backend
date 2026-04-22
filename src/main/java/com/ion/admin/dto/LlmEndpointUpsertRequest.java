package com.ion.admin.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record LlmEndpointUpsertRequest(
        @NotBlank(message = "엔드포인트 이름은 필수입니다.")
        @Size(max = 100, message = "엔드포인트 이름은 100자 이하여야 합니다.")
        String name,

        @NotBlank(message = "Base URL은 필수입니다.")
        @Size(max = 500, message = "Base URL은 500자 이하여야 합니다.")
        String baseUrl,

        @NotBlank(message = "API Key는 필수입니다.")
        @Size(max = 1000, message = "API Key는 1000자 이하여야 합니다.")
        String apiKey,

        @NotBlank(message = "모델명은 필수입니다.")
        @Size(max = 200, message = "모델명은 200자 이하여야 합니다.")
        String model,

        @NotBlank(message = "시스템 프롬프트는 필수입니다.")
        String systemPrompt,

        @NotNull(message = "temperature는 필수입니다.")
        @DecimalMin(value = "0.0", message = "temperature는 0.0 이상이어야 합니다.")
        @DecimalMax(value = "2.0", message = "temperature는 2.0 이하여야 합니다.")
        Double temperature,

        @NotNull(message = "maxTokens는 필수입니다.")
        @Min(value = 1, message = "maxTokens는 1 이상이어야 합니다.")
        @Max(value = 32768, message = "maxTokens는 32768 이하여야 합니다.")
        Integer maxTokens,

        @NotNull(message = "enabled 값은 필수입니다.")
        Boolean enabled,

        @NotNull(message = "isDefault 값은 필수입니다.")
        Boolean isDefault
) {
}
