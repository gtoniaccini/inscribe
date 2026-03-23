package dev.inscribe.api.sse;

import dev.inscribe.core.event.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Manages SSE connections per container and broadcasts workflow events.
 */
@Component
public class SseEventBroadcaster {

    private static final Logger log = LoggerFactory.getLogger(SseEventBroadcaster.class);
    private static final long SSE_TIMEOUT = 5 * 60 * 1000L; // 5 minutes

    private final Map<UUID, CopyOnWriteArrayList<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public SseEmitter subscribe(UUID containerId) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);
        emitters.computeIfAbsent(containerId, k -> new CopyOnWriteArrayList<>()).add(emitter);

        emitter.onCompletion(() -> removeEmitter(containerId, emitter));
        emitter.onTimeout(() -> removeEmitter(containerId, emitter));
        emitter.onError(e -> removeEmitter(containerId, emitter));

        return emitter;
    }

    @EventListener
    public void onItemRequested(ItemRequestedEvent event) {
        broadcast(event.containerId(), "ITEM_REQUESTED", event);
    }

    @EventListener
    public void onItemValidated(ItemValidatedEvent event) {
        broadcast(event.containerId(), "ITEM_VALIDATED", event);
    }

    @EventListener
    public void onItemRejected(ItemRejectedEvent event) {
        broadcast(event.containerId(), "ITEM_REJECTED", event);
    }

    @EventListener
    public void onItemInserted(ItemInsertedEvent event) {
        broadcast(event.containerId(), "ITEM_INSERTED", event);
    }

    private void broadcast(UUID containerId, String eventName, Object data) {
        CopyOnWriteArrayList<SseEmitter> list = emitters.get(containerId);
        if (list == null || list.isEmpty()) return;

        list.forEach(emitter -> {
            try {
                emitter.send(SseEmitter.event()
                        .name(eventName)
                        .data(data));
            } catch (IOException e) {
                removeEmitter(containerId, emitter);
            }
        });
    }

    private void removeEmitter(UUID containerId, SseEmitter emitter) {
        CopyOnWriteArrayList<SseEmitter> list = emitters.get(containerId);
        if (list != null) {
            list.remove(emitter);
            if (list.isEmpty()) {
                emitters.remove(containerId);
            }
        }
    }
}
