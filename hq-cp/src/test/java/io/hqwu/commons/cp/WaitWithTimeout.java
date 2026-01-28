package io.hqwu.commons.cp;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created with IntelliJ IDEA for hq-commons-parent
 *
 * @author taige (Wu, Hongqiang)
 * Date: 2026-01-27
 * Time: 15:15
 *
 */
class WaitWithTimeout {
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition condition = lock.newCondition();
    // 增加一个标志，用于标识是否被提前唤醒
    private boolean woken = false;

    /**
     * 类似于 Thread.sleep(timeMillis) 的等待，但当其他线程调用 wakeUp() 后，将立即返回。
     * 如果没有被提前唤醒，则一直等待到超时为止。
     */
    public void awaitWithTimeout(long timeMillis) {
        lock.lock();
        woken = false; // 重置标志
        try {
            long nanos = timeMillis * 1_000_000;
            // 循环等待，直到超时或者收到提前唤醒信号
            while (nanos > 0 && !woken) {
                nanos = condition.awaitNanos(nanos);
            }
        } catch (InterruptedException e) {
        } finally {
            lock.unlock();
        }
    }

    /**
     * 提前唤醒等待的线程
     */
    public void wakeUp() {
        lock.lock();
        try {
            // 设置标志并发送通知
            woken = true;
            condition.signalAll();
        } finally {
            lock.unlock();
        }
    }

    /**
     * 等待直到当前时间满足：
     * 1. 当前秒数（取秒数）个位为 0，即 (seconds % 10 == 0)
     * 2. 当前毫秒数小于指定阈值（例如 10 毫秒）
     * <p>
     * 使用动态计算下次满足条件时刻与当前时刻的时间差来等待，而非固定等待 1 毫秒，
     * 同时允许外部通过其他方式提前唤醒等待线程。
     *
     * @param msThreshold 毫秒阈值（例如 10）
     */
    public void awaitTimeWithSecUnitZero(long msThreshold) {
        lock.lock();
        try {
            while (true) {
                long now = System.currentTimeMillis();
                long seconds = (now / 1000) % 60; // 当前秒数（0-59）
                long millis = now % 1000;         // 当前毫秒数（0-999）

                // 判断秒的个位是否为0和毫秒是否小于阈值
                if (seconds % 10 == 0 && millis < msThreshold) {
                    break;
                }
                // 计算下次满足秒个位为 0 的时刻
                // 设当前秒为 s，则下一满足条件的秒为：s' = s - (s % 10) + 10
                long secondsToWait;
                if (seconds % 10 == 0 && millis >= msThreshold) {
                    // 仍需要等待直到毫秒部分低于阈值
                    secondsToWait = 0;
                } else {
                    secondsToWait = 10 - (seconds % 10);
                }
                // 计算下一个满足条件时刻的时间戳粗略值，
                // 这里简单计算到整秒点（毫秒为0），再等待 msThreshold 毫秒之前
                long targetTime = ((now / 1000) + secondsToWait) * 1000;
                // 如果等待的时间超过 msThreshold，则减去 msThreshold，这样到达目标时刻时，毫秒部分刚好在[0, msThreshold)内
                targetTime -= msThreshold;
                long waitTimeMillis = Math.max(targetTime - now, 1); // 至少等待 1 毫秒
                long nanos = TimeUnit.MILLISECONDS.toNanos(waitTimeMillis);
                condition.awaitNanos(nanos);
            }
        } catch (InterruptedException e) {
        } finally {
            lock.unlock();
        }
    }

}
