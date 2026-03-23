package dev.inscribe.core.workflow;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class WorkflowDefinitionTest {

    @Test
    void recordFieldsAreAccessible() {
        WorkflowDefinition.StepDef step = new WorkflowDefinition.StepDef("validazione", "MyHandler", WorkflowDefinition.FailurePolicy.REJECT);
        WorkflowDefinition.TopicDef onComplete = new WorkflowDefinition.TopicDef("test.inserted");
        WorkflowDefinition.TopicDef onReject = new WorkflowDefinition.TopicDef("test.rejected");

        WorkflowDefinition def = new WorkflowDefinition("test", "Container", "Item",
                "AI prompt {userInput}", java.util.List.of(step), onComplete, onReject);

        assertEquals("test", def.name());
        assertEquals("Container", def.containerLabel());
        assertEquals("Item", def.itemLabel());
        assertEquals(1, def.steps().size());
        assertEquals("MyHandler", def.steps().get(0).handler());
        assertEquals("test.inserted", def.onComplete().topic());
    }
}
