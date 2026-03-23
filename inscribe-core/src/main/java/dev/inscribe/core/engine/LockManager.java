package dev.inscribe.core.engine;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Wraps Redisson distributed locks with a per-container key scheme.
 * The lock has an automatic watchdog that renews the TTL while the holder is alive.
 */
@Component
public class LockManager {

    private static final Logger log = LoggerFactory.getLogger(LockManager.class);
    private static final String LOCK_PREFIX = "inscribe:lock:container:";

    private final RedissonClient redisson;

    public LockManager(RedissonClient redisson) {
        this.redisson = redisson;
    }

    /**
     * Try to acquire the lock for a container.
     * @return true if the lock was acquired within the wait time
     */
    public boolean tryLock(UUID containerId, long waitMs) {
        RLock lock = redisson.getLock(LOCK_PREFIX + containerId);
        try {
            // leaseTime = -1 enables the watchdog (auto-renewal)
            boolean acquired = lock.tryLock(waitMs, -1, TimeUnit.MILLISECONDS);
            if (acquired) {
                log.debug("Lock acquired for container {}", containerId);
            }
            return acquired;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    public void unlock(UUID containerId) {
        RLock lock = redisson.getLock(LOCK_PREFIX + containerId);
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
            log.debug("Lock released for container {}", containerId);
        }
    }

    public boolean isLocked(UUID containerId) {
        return redisson.getLock(LOCK_PREFIX + containerId).isLocked();
    }
}
