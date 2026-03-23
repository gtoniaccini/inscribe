package dev.inscribe.core.engine;

import org.redisson.api.RDeque;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Recovers orphaned queues: if a pod crashes while holding a lock,
 * the Redisson watchdog will eventually expire the lock. This job
 * detects queues with pending commands and no active lock, then
 * triggers the Orchestrator to drain them.
 */
@Component
public class QueueRecoveryJob {

    private static final Logger log = LoggerFactory.getLogger(QueueRecoveryJob.class);
    private static final String QUEUE_PREFIX = "inscribe:queue:container:";

    private final RedissonClient redisson;
    private final LockManager lockManager;
    private final Orchestrator orchestrator;

    public QueueRecoveryJob(RedissonClient redisson, LockManager lockManager, Orchestrator orchestrator) {
        this.redisson = redisson;
        this.lockManager = lockManager;
        this.orchestrator = orchestrator;
    }

    @Scheduled(fixedDelayString = "${inscribe.recovery.interval-ms:30000}")
    public void recoverOrphanedQueues() {
        // Scan for known queue keys
        redisson.getKeys().getKeysByPattern(QUEUE_PREFIX + "*").forEach(key -> {
            String containerIdStr = key.substring(QUEUE_PREFIX.length());
            UUID containerId;
            try {
                containerId = UUID.fromString(containerIdStr);
            } catch (IllegalArgumentException e) {
                return; // skip malformed keys
            }

            RDeque<?> queue = redisson.getDeque(key);
            if (!queue.isEmpty() && !lockManager.isLocked(containerId)) {
                log.info("Recovering orphaned queue for container {}", containerId);
                orchestrator.drainQueue(containerId);
            }
        });
    }
}
