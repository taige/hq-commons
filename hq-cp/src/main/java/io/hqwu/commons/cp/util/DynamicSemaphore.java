package io.hqwu.commons.cp.util;

import io.hqwu.commons.util.Logger;
import lombok.Getter;

import java.util.concurrent.Semaphore;

/**
 * 动态信号量，扩展自 {@link Semaphore}。
 * <p>
 * 该类支持在运行时动态调整许可（permits）的总量。
 * 适用于需要根据系统负载、业务配额或配置中心动态调整并发限制的场景。
 * </p>
 *
 * @author taige (Wu, Hongqiang)
 * @since 2025-03-04
 */
@Getter
public class DynamicSemaphore extends Semaphore {
    private static final Logger LOGGER = new Logger();

    // 当前的最大连接数
    private volatile int currentPermits;

    public DynamicSemaphore(int initialMaxConnections) {
        super(initialMaxConnections);
        this.currentPermits = initialMaxConnections;
    }

    /**
     * 动态修改permits。
     * 如果 newPermits 高于 currentPermits，则增加许可数；
     * 如果 newPermits 低于 currentPermits，则减少许可数。
     */
    public void updatePermits(int newPermits) {
        if (newPermits > currentPermits) {
            int extra = newPermits - currentPermits;
            super.release(extra);
            LOGGER.debug("updatePermits: ", currentPermits, " -> ", newPermits, ", extra: ", extra);
        } else if (newPermits < currentPermits) {
            int reduction = currentPermits - newPermits;
            super.reducePermits(reduction);
            LOGGER.debug("updatePermits: ", currentPermits, " -> ", newPermits, ", reduction: ", reduction);
        }
        currentPermits = newPermits;
    }

}
