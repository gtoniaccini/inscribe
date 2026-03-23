package dev.inscribe.core.spi;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class WorkflowContextTest {

    @Test
    void shortConstructorGeneratesCorrelationId() {
        UUID containerId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();

        WorkflowContext ctx = new WorkflowContext(containerId, itemId, "CODE-1", "Test Item", "medical");

        assertEquals(containerId, ctx.containerId());
        assertEquals(itemId, ctx.itemId());
        assertEquals("CODE-1", ctx.catalogItemId());
        assertEquals("Test Item", ctx.catalogItemName());
        assertEquals("medical", ctx.workflowName());
        assertNotNull(ctx.correlationId());
        assertEquals(Map.of(), ctx.metadata());
    }

    @Test
    void fullConstructorPreservesAllFields() {
        UUID containerId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        UUID correlationId = UUID.randomUUID();
        Map<String, Object> meta = Map.of("key", "value");

        WorkflowContext ctx = new WorkflowContext(
                containerId, itemId, "SKU-1", "Product", "ecommerce", correlationId, meta);

        assertEquals(correlationId, ctx.correlationId());
        assertEquals(meta, ctx.metadata());
    }
}
