package dev.inscribe.core.engine;

import dev.inscribe.core.spi.StepHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Auto-discovers all {@link StepHandler} beans in the Spring context
 * and provides lookup by handler name.
 */
@Component
public class StepHandlerRegistry {

    private static final Logger log = LoggerFactory.getLogger(StepHandlerRegistry.class);
    private final Map<String, StepHandler> handlers;

    public StepHandlerRegistry(List<StepHandler> handlerBeans) {
        this.handlers = handlerBeans.stream()
                .collect(Collectors.toMap(StepHandler::getName, Function.identity()));
        log.info("Registered {} step handlers: {}", handlers.size(), handlers.keySet());
    }

    public StepHandler get(String name) {
        StepHandler handler = handlers.get(name);
        if (handler == null) {
            throw new IllegalArgumentException("No StepHandler registered with name: " + name);
        }
        return handler;
    }
}
