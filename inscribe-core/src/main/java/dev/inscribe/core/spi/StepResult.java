package dev.inscribe.core.spi;

/**
 * Result returned by a {@link StepHandler} after processing a workflow step.
 */
public sealed interface StepResult {

    record Success() implements StepResult {}
    record Reject(String reason) implements StepResult {}
    record Retry(String reason) implements StepResult {}

    static StepResult success() { return new Success(); }
    static StepResult reject(String reason) { return new Reject(reason); }
    static StepResult retry(String reason) { return new Retry(reason); }
}
