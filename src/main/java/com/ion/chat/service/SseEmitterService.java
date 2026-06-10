package com.ion.chat.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class SseEmitterService {

    private static final int MAX_PENDING_EVENTS = 4096;
    private static final long PENDING_EVENT_TTL_MINUTES = 5L;

    private final Map<UUID, SseEmitter> emitters = new ConcurrentHashMap<>();
    private final Map<UUID, List<PendingEvent>> pendingEvents = new ConcurrentHashMap<>();

    public SseEmitter create(UUID sessionId) {
        SseEmitter emitter = new SseEmitter(300_000L); // 5분 타임아웃
        emitters.put(sessionId, emitter);
        emitter.onCompletion(() -> emitters.remove(sessionId));
        emitter.onTimeout(() -> emitters.remove(sessionId));
        emitter.onError(e -> emitters.remove(sessionId));
        replayPendingEvents(sessionId, emitter);
        return emitter;
    }

    public void sendToken(UUID sessionId, String token) {
        SseEmitter emitter = emitters.get(sessionId);
        PendingEvent event = new PendingEvent("token", "{\"token\":\"" + escapeJson(token) + "\"}", false);
        if (emitter == null) {
            bufferEvent(sessionId, event);
            return;
        }
        sendOrBuffer(sessionId, emitter, event);
    }

    public void sendDone(UUID sessionId, Long messageId) {
        SseEmitter emitter = emitters.get(sessionId);
        PendingEvent event = new PendingEvent("done", "{\"messageId\":" + messageId + ",\"finishReason\":\"stop\"}", true);
        if (emitter == null) {
            bufferEvent(sessionId, event);
            schedulePendingCleanup(sessionId);
            return;
        }
        sendOrBuffer(sessionId, emitter, event);
    }

    public void sendError(UUID sessionId, String code, String message) {
        SseEmitter emitter = emitters.get(sessionId);
        PendingEvent event = new PendingEvent("error", "{\"code\":\"" + code + "\",\"message\":\"" + escapeJson(message) + "\"}", true);
        if (emitter == null) {
            bufferEvent(sessionId, event);
            schedulePendingCleanup(sessionId);
            return;
        }
        sendOrBuffer(sessionId, emitter, event);
    }

    private void sendOrBuffer(UUID sessionId, SseEmitter emitter, PendingEvent event) {
        try {
            send(emitter, event);
            if (event.terminal()) {
                emitter.complete();
                emitters.remove(sessionId);
                pendingEvents.remove(sessionId);
            }
        } catch (IOException e) {
            log.warn("SSE send failed for session {}", sessionId);
            emitters.remove(sessionId);
            bufferEvent(sessionId, event);
            if (event.terminal()) {
                schedulePendingCleanup(sessionId);
            }
        }
    }

    private void replayPendingEvents(UUID sessionId, SseEmitter emitter) {
        List<PendingEvent> events = pendingEvents.remove(sessionId);
        if (events == null || events.isEmpty()) return;

        synchronized (events) {
            for (PendingEvent event : events) {
                try {
                    send(emitter, event);
                    if (event.terminal()) {
                        emitter.complete();
                        emitters.remove(sessionId);
                        return;
                    }
                } catch (IOException e) {
                    log.warn("SSE replay failed for session {}", sessionId);
                    emitters.remove(sessionId);
                    return;
                }
            }
        }
    }

    private void send(SseEmitter emitter, PendingEvent event) throws IOException {
        emitter.send(SseEmitter.event()
                .name(event.name())
                .data(event.data()));
    }

    private void bufferEvent(UUID sessionId, PendingEvent event) {
        List<PendingEvent> events = pendingEvents.computeIfAbsent(
                sessionId,
                ignored -> Collections.synchronizedList(new ArrayList<>())
        );

        synchronized (events) {
            if (events.size() >= MAX_PENDING_EVENTS) {
                events.remove(0);
            }
            events.add(event);
        }
    }

    private void schedulePendingCleanup(UUID sessionId) {
        CompletableFuture.runAsync(
                () -> pendingEvents.remove(sessionId),
                CompletableFuture.delayedExecutor(PENDING_EVENT_TTL_MINUTES, TimeUnit.MINUTES)
        );
    }

    private String escapeJson(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private record PendingEvent(String name, String data, boolean terminal) {}
}
