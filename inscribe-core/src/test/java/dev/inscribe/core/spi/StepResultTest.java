package dev.inscribe.core.spi;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StepResultTest {

    @Test
    void successResult() {
        StepResult result = StepResult.success();
        assertInstanceOf(StepResult.Success.class, result);
    }

    @Test
    void rejectResultContainsReason() {
        StepResult result = StepResult.reject("Invalid item");
        assertInstanceOf(StepResult.Reject.class, result);
        assertEquals("Invalid item", ((StepResult.Reject) result).reason());
    }

    @Test
    void retryResultContainsReason() {
        StepResult result = StepResult.retry("Temporary failure");
        assertInstanceOf(StepResult.Retry.class, result);
        assertEquals("Temporary failure", ((StepResult.Retry) result).reason());
    }

    @Test
    void sealedInterfaceExhaustivenessWithSwitch() {
        StepResult result = StepResult.success();
        String label = switch (result) {
            case StepResult.Success s -> "ok";
            case StepResult.Reject r -> "rejected: " + r.reason();
            case StepResult.Retry r -> "retry: " + r.reason();
        };
        assertEquals("ok", label);
    }
}
