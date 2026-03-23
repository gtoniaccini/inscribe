package dev.inscribe.api.controller;

import dev.inscribe.api.sse.SseEventBroadcaster;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;

@RestController
@RequestMapping("/api/stream")
public class SseController {

    private final SseEventBroadcaster broadcaster;

    public SseController(SseEventBroadcaster broadcaster) {
        this.broadcaster = broadcaster;
    }

    @GetMapping(value = "/{containerId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@PathVariable UUID containerId) {
        return broadcaster.subscribe(containerId);
    }
}
