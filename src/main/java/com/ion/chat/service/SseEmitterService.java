package com.ion.chat.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class SseEmitterService {

    private final Map<UUID, SseEmitter> emitters = new ConcurrentHashMap<>();

    public SseEmitter create(UUID sessionId) {
        SseEmitter emitter = new SseEmitter(300_000L); // 5분 타임아웃
        emitters.put(sessionId, emitter);
        emitter.onCompletion(() -> emitters.remove(sessionId));
        emitter.onTimeout(() -> emitters.remove(sessionId));
        emitter.onError(e -> emitters.remove(sessionId));
        return emitter;
    }

    public void sendToken(UUID sessionId, String token) {
        SseEmitter emitter = emitters.get(sessionId);
        if (emitter == null) return;
        try {
            emitter.send(SseEmitter.event()
                    .name("token")
                    .data("{\"token\":\"" + escapeJson(token) + "\"}"));
        } catch (IOException e) {
            log.warn("SSE send failed for session {}", sessionId);
            emitters.remove(sessionId);
        }
    }

    public void sendDone(UUID sessionId, Long messageId) {
        SseEmitter emitter = emitters.get(sessionId);
        if (emitter == null) return;
        try {
            emitter.send(SseEmitter.event()
                    .name("done")
                    .data("{\"messageId\":" + messageId + ",\"finishReason\":\"stop\"}"));
            emitter.complete();
        } catch (IOException e) {
            log.warn("SSE done send failed for session {}", sessionId);
        } finally {
            emitters.remove(sessionId);
        }
    }

    public void sendError(UUID sessionId, String code, String message) {
        SseEmitter emitter = emitters.get(sessionId);
        if (emitter == null) return;
        try {
            emitter.send(SseEmitter.event()
                    .name("error")
                    .data("{\"code\":\"" + code + "\",\"message\":\"" + escapeJson(message) + "\"}"));
            emitter.complete();
        } catch (IOException e) {
            log.warn("SSE error send failed for session {}", sessionId);
        } finally {
            emitters.remove(sessionId);
        }
    }

    private String escapeJson(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
