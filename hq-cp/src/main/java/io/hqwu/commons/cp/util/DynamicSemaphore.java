package io.hqwu.commons.cp.util;

import io.hqwu.commons.util.Logger;

import java.util.concurrent.Semaphore;

/**
 * Created with IntelliJ IDEA for hq-commons-parent
 *
 * @author taige (Wu, Hongqiang)
 * Date: 2025-03-04
 * Time: 21:23
 */
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

    public int getCurrentPermits() {
        return currentPermits;
    }
}
