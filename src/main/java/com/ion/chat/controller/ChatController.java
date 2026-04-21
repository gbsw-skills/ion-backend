package com.ion.chat.controller;

import com.ion.chat.dto.MessageResponse;
import com.ion.chat.dto.SendMessageRequest;
import com.ion.chat.dto.SessionResponse;
import com.ion.chat.service.ChatService;
import com.ion.common.response.ApiResponse;
import com.ion.common.response.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @PostMapping("/sessions")
    public ResponseEntity<ApiResponse<SessionResponse>> createSession(
            @AuthenticationPrincipal Long userId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(chatService.createSession(userId)));
    }

    @GetMapping("/sessions")
    public ResponseEntity<ApiResponse<PageResponse<SessionResponse>>> getSessions(
            @AuthenticationPrincipal Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(chatService.getSessions(userId, page, size)));
    }

    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<ApiResponse<Void>> deleteSession(
            @PathVariable UUID sessionId,
            @AuthenticationPrincipal Long userId) {
        chatService.deleteSession(sessionId, userId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PostMapping("/sessions/{sessionId}/messages")
    public ResponseEntity<ApiResponse<MessageResponse>> sendMessage(
            @PathVariable UUID sessionId,
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody SendMessageRequest request) {
        MessageResponse response = chatService.sendMessage(sessionId, userId, request);
        // 트랜잭션 커밋 후 비동기 LLM 처리 시작
        chatService.triggerLlmProcessing(sessionId);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.ok(response));
    }

    @GetMapping(value = "/sessions/{sessionId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(
            @PathVariable UUID sessionId,
            @AuthenticationPrincipal Long userId) {
        return chatService.createSseEmitter(sessionId, userId);
    }

    @GetMapping("/sessions/{sessionId}/messages")
    public ResponseEntity<ApiResponse<PageResponse<MessageResponse>>> getMessages(
            @PathVariable UUID sessionId,
            @AuthenticationPrincipal Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(chatService.getMessages(sessionId, userId, page, size)));
    }
}
