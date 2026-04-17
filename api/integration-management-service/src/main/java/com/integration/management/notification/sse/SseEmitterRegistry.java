package com.integration.management.notification.sse;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Component
@RequiredArgsConstructor
public class SseEmitterRegistry {

    private final ObjectMapper objectMapper;

    private final ConcurrentHashMap<String, CopyOnWriteArrayList<SseEmitter>> emitters =
            new ConcurrentHashMap<>();

    public SseEmitter register(String userId) {
        SseEmitter emitter = new SseEmitter(30L * 60 * 1000); // 30 minutes
        emitters.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>()).add(emitter);

        emitter.onCompletion(() -> remove(userId, emitter));
        emitter.onTimeout(() -> {
            emitter.complete();
            remove(userId, emitter);
        });
        emitter.onError(ex -> {
            log.debug("SSE emitter error for user {}: {}", userId, ex.getMessage());
            remove(userId, emitter);
        });

        log.info("Registered SSE emitter for user: {} — active emitters for user: {}",
                userId, emitters.getOrDefault(userId, new CopyOnWriteArrayList<>()).size());
        return emitter;
    }

    public void send(String userId, Object payload) {
        List<SseEmitter> userEmitters = emitters.get(userId);
        if (userEmitters == null || userEmitters.isEmpty()) {
            log.debug("No active SSE emitters for user: {}", userId);
            return;
        }
        try {
            String json = objectMapper.writeValueAsString(payload);
            SseEmitter.SseEventBuilder event = SseEmitter.event()
                    .name("notification")
                    .data(json);
            List<SseEmitter> dead = new java.util.ArrayList<>();
            for (SseEmitter emitter : userEmitters) {
                try {
                    emitter.send(event);
                } catch (IOException | IllegalStateException ex) {
                    log.debug("Failed to send SSE to user {}, removing emitter: {}", userId, ex.getMessage());
                    dead.add(emitter);
                }
            }
            dead.forEach(e -> remove(userId, e));
        } catch (Exception ex) {
            log.error("Error serializing SSE payload for user: {}", userId, ex);
        }
    }

    private void remove(String userId, SseEmitter emitter) {
        CopyOnWriteArrayList<SseEmitter> userEmitters = emitters.get(userId);
        if (userEmitters != null) {
            userEmitters.remove(emitter);
            if (userEmitters.isEmpty()) {
                emitters.remove(userId);
            }
        }
    }

    @Scheduled(fixedDelay = 25_000)
    public void heartbeat() {
        if (emitters.isEmpty()) {
            return;
        }
        log.debug("Sending SSE heartbeat to {} users", emitters.size());
        SseEmitter.SseEventBuilder ping = SseEmitter.event().comment("heartbeat");
        emitters.forEach((userId, userEmitters) -> {
            List<SseEmitter> dead = new ArrayList<>();
            for (SseEmitter emitter : userEmitters) {
                try {
                    emitter.send(ping);
                } catch (IOException | IllegalStateException ex) {
                    dead.add(emitter);
                }
            }
            dead.forEach(e -> remove(userId, e));
        });
    }
}
