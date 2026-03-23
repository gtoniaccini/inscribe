package dev.inscribe.core.spi;

/**
 * SPI interface for workflow step implementations.
 * Each plugin provides one or more StepHandler beans; the core discovers them
 * automatically through Spring component scanning.
 */
public interface StepHandler {

    /**
     * Unique name matching the {@code handler} field in the workflow YAML.
     */
    String getName();

    /**
     * Execute the step logic for the given workflow context.
     */
    StepResult handle(WorkflowContext context);
}
