package com.ion.llm.service;

import com.ion.admin.dto.LlmEndpointResponse;
import com.ion.admin.dto.LlmEndpointUpsertRequest;
import com.ion.admin.service.AdminLogService;
import com.ion.common.exception.ErrorCode;
import com.ion.common.exception.IonException;
import com.ion.llm.domain.LlmEndpointConfig;
import com.ion.llm.repository.LlmEndpointConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LlmEndpointConfigService {

    private static final String TARGET_TYPE = "LLM_ENDPOINT";

    private final LlmEndpointConfigRepository repository;
    private final AdminLogService adminLogService;

    @Transactional(readOnly = true)
    public List<LlmEndpointResponse> getEndpoints() {
        return repository.findAllByOrderByIsDefaultDescEnabledDescNameAsc().stream()
                .map(LlmEndpointResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public LlmEndpointResponse getEndpoint(Long id) {
        return LlmEndpointResponse.from(getById(id));
    }

    @Transactional(readOnly = true)
    public LlmEndpointConfig getDefaultActiveEndpoint() {
        return repository.findByIsDefaultTrueAndEnabledTrue()
                .orElseThrow(() -> new IonException(ErrorCode.LLM_003));
    }

    @Transactional
    public LlmEndpointResponse create(Long adminId, LlmEndpointUpsertRequest request) {
        validateUniqueName(request.name(), null);

        boolean firstEndpoint = repository.count() == 0;
        boolean enabled = firstEndpoint || request.enabled();
        boolean isDefault = firstEndpoint || request.isDefault();

        validateDefaultEnabled(enabled, isDefault);

        if (isDefault) {
            clearDefaultFlag();
        }

        LlmEndpointConfig saved = repository.save(LlmEndpointConfig.builder()
                .name(request.name().trim())
                .baseUrl(normalizeBaseUrl(request.baseUrl()))
                .apiKey(request.apiKey().trim())
                .model(request.model().trim())
                .systemPrompt(request.systemPrompt().trim())
                .temperature(request.temperature())
                .maxTokens(request.maxTokens())
                .enabled(enabled)
                .isDefault(isDefault)
                .build());

        adminLogService.log(adminId, "LLM_ENDPOINT_CREATE", TARGET_TYPE, saved.getId());
        return LlmEndpointResponse.from(saved);
    }

    @Transactional
    public LlmEndpointResponse update(Long adminId, Long id, LlmEndpointUpsertRequest request) {
        LlmEndpointConfig config = getById(id);
        validateUniqueName(request.name(), id);

        boolean enabled = request.enabled();
        boolean isDefault = request.isDefault();
        validateDefaultEnabled(enabled, isDefault);

        if (!enabled && config.isEnabled() && repository.countByEnabledTrue() <= 1) {
            throw new IonException(ErrorCode.LLM_005);
        }

        if (isDefault) {
            clearDefaultFlag();
        } else if (config.isDefault()) {
            LlmEndpointConfig fallback = repository.findAllByOrderByIsDefaultDescEnabledDescNameAsc().stream()
                    .filter(candidate -> !candidate.getId().equals(id) && candidate.isEnabled())
                    .findFirst()
                    .orElseThrow(() -> new IonException(ErrorCode.LLM_005));
            fallback.setDefault(true);
        }

        config.update(
                request.name().trim(),
                normalizeBaseUrl(request.baseUrl()),
                request.apiKey().trim(),
                request.model().trim(),
                request.systemPrompt().trim(),
                request.temperature(),
                request.maxTokens(),
                enabled,
                isDefault
        );

        adminLogService.log(adminId, "LLM_ENDPOINT_UPDATE", TARGET_TYPE, config.getId());
        return LlmEndpointResponse.from(config);
    }

    @Transactional
    public LlmEndpointResponse setDefault(Long adminId, Long id) {
        LlmEndpointConfig config = getById(id);
        if (!config.isEnabled()) {
            throw new IonException(ErrorCode.LLM_006);
        }

        clearDefaultFlag();
        config.setDefault(true);

        adminLogService.log(adminId, "LLM_ENDPOINT_SET_DEFAULT", TARGET_TYPE, config.getId());
        return LlmEndpointResponse.from(config);
    }

    @Transactional
    public void delete(Long adminId, Long id) {
        LlmEndpointConfig config = getById(id);

        if (repository.count() <= 1) {
            throw new IonException(ErrorCode.LLM_005);
        }

        boolean deletedWasOnlyEnabled = config.isEnabled() && repository.countByEnabledTrue() <= 1;
        if (deletedWasOnlyEnabled) {
            throw new IonException(ErrorCode.LLM_005);
        }

        repository.delete(config);

        if (config.isDefault()) {
            repository.findAllByOrderByIsDefaultDescEnabledDescNameAsc().stream()
                    .filter(LlmEndpointConfig::isEnabled)
                    .findFirst()
                    .ifPresent(candidate -> candidate.setDefault(true));
        }

        adminLogService.log(adminId, "LLM_ENDPOINT_DELETE", TARGET_TYPE, id);
    }

    private void validateUniqueName(String name, Long id) {
        String normalized = name.trim();
        boolean exists = id == null
                ? repository.existsByName(normalized)
                : repository.existsByNameAndIdNot(normalized, id);
        if (exists) {
            throw new IonException(ErrorCode.LLM_004);
        }
    }

    private void validateDefaultEnabled(boolean enabled, boolean isDefault) {
        if (isDefault && !enabled) {
            throw new IonException(ErrorCode.LLM_006);
        }
    }

    private void clearDefaultFlag() {
        repository.findAllByOrderByIsDefaultDescEnabledDescNameAsc().stream()
                .filter(LlmEndpointConfig::isDefault)
                .forEach(existing -> existing.setDefault(false));
    }

    private LlmEndpointConfig getById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new IonException(ErrorCode.LLM_007));
    }

    private String normalizeBaseUrl(String baseUrl) {
        String normalized = baseUrl.trim();
        if (normalized.endsWith("/")) {
            return normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }
}
