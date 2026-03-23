package dev.inscribe.core.workflow;

import java.util.List;

/**
 * Java representation of a workflow YAML definition file.
 * Loaded at startup by {@link WorkflowDefinitionLoader}.
 */
public record WorkflowDefinition(
        String name,
        String containerLabel,
        String itemLabel,
        String aiPrompt,
        List<StepDef> steps,
        TopicDef onComplete,
        TopicDef onReject
) {
    public record StepDef(String name, String handler, FailurePolicy onFailure) {}
    public record TopicDef(String topic) {}
    public enum FailurePolicy { REJECT, RETRY }
}
