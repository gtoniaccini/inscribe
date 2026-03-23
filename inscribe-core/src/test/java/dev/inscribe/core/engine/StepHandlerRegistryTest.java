package dev.inscribe.core.engine;

import dev.inscribe.core.spi.StepHandler;
import dev.inscribe.core.spi.StepResult;
import dev.inscribe.core.spi.WorkflowContext;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class StepHandlerRegistryTest {

    @Test
    void registersAndFindsHandlersByName() {
        StepHandler handler1 = new TestHandler("HandlerA");
        StepHandler handler2 = new TestHandler("HandlerB");

        StepHandlerRegistry registry = new StepHandlerRegistry(List.of(handler1, handler2));

        assertEquals(handler1, registry.get("HandlerA"));
        assertEquals(handler2, registry.get("HandlerB"));
    }

    @Test
    void throwsForUnknownHandler() {
        StepHandlerRegistry registry = new StepHandlerRegistry(List.of());
        assertThrows(IllegalArgumentException.class, () -> registry.get("NonExistent"));
    }

    @Test
    void rejectsDuplicateHandlerNames() {
        StepHandler h1 = new TestHandler("Same");
        StepHandler h2 = new TestHandler("Same");
        assertThrows(IllegalStateException.class,
                () -> new StepHandlerRegistry(List.of(h1, h2)));
    }

    private record TestHandler(String name) implements StepHandler {
        @Override
        public String getName() { return name; }

        @Override
        public StepResult handle(WorkflowContext context) {
            return StepResult.success();
        }
    }
}
