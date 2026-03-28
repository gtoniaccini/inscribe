package dev.inscribe.core.engine;

import dev.inscribe.core.command.InsertItemCommand;
import org.redisson.api.RDeque;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Central orchestrator: receives commands, enqueues them per-container in Redis,
 * acquires a distributed lock, and delegates step execution to CommandProcessor.
 */
@Component
public class Orchestrator {

    private static final Logger log = LoggerFactory.getLogger(Orchestrator.class);
    private static final String QUEUE_PREFIX = "inscribe:queue:container:";

    private final RedissonClient redisson;
    private final LockManager lockManager;
    private final CommandProcessor commandProcessor;
    private final TaskExecutor taskExecutor;

    public Orchestrator(RedissonClient redisson,
                        LockManager lockManager,
                        CommandProcessor commandProcessor,
                        TaskExecutor taskExecutor) {
        this.redisson = redisson;
        this.lockManager = lockManager;
        this.commandProcessor = commandProcessor;
        this.taskExecutor = taskExecutor;
    }

    /**
     * Enqueues the command and triggers async draining.
     * Returns immediately — processing happens in background.
     */
    public void submit(InsertItemCommand command) {
        RDeque<InsertItemCommand> queue = redisson.getDeque(QUEUE_PREFIX + command.containerId());
        queue.addLast(command);
        log.info("[{}] Command enqueued for container {}", command.correlationId(), command.containerId());
        taskExecutor.execute(() -> drainQueue(command.containerId()));
    }

    /**
     * Try to process all pending commands for a given container.
     * Only one pod at a time can drain a container's queue (distributed lock).
     */
    public void drainQueue(UUID containerId) {
        if (!lockManager.tryLock(containerId, 0)) {
            log.debug("Container {} is already being processed by another worker", containerId);
            return;
        }
        try {
            RDeque<InsertItemCommand> queue = redisson.getDeque(QUEUE_PREFIX + containerId);
            InsertItemCommand command;
            while ((command = queue.pollFirst()) != null) {
                commandProcessor.process(command);
            }
        } finally {
            lockManager.unlock(containerId);
        }
    }
}
