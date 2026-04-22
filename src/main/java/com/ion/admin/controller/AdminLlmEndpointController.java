package com.ion.admin.controller;

import com.ion.admin.dto.LlmEndpointResponse;
import com.ion.admin.dto.LlmEndpointUpsertRequest;
import com.ion.llm.service.LlmEndpointConfigService;
import com.ion.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/llm/endpoints")
@RequiredArgsConstructor
public class AdminLlmEndpointController {

    private final LlmEndpointConfigService llmEndpointConfigService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<LlmEndpointResponse>>> getEndpoints() {
        return ResponseEntity.ok(ApiResponse.ok(llmEndpointConfigService.getEndpoints()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<LlmEndpointResponse>> getEndpoint(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(llmEndpointConfigService.getEndpoint(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<LlmEndpointResponse>> create(
            @AuthenticationPrincipal Long adminId,
            @Valid @RequestBody LlmEndpointUpsertRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(llmEndpointConfigService.create(adminId, request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<LlmEndpointResponse>> update(
            @AuthenticationPrincipal Long adminId,
            @PathVariable Long id,
            @Valid @RequestBody LlmEndpointUpsertRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(llmEndpointConfigService.update(adminId, id, request)));
    }

    @PostMapping("/{id}/default")
    public ResponseEntity<ApiResponse<LlmEndpointResponse>> setDefault(
            @AuthenticationPrincipal Long adminId,
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(ApiResponse.ok(llmEndpointConfigService.setDefault(adminId, id)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @AuthenticationPrincipal Long adminId,
            @PathVariable Long id
    ) {
        llmEndpointConfigService.delete(adminId, id);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
