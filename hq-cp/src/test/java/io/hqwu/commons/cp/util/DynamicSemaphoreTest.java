package io.hqwu.commons.cp.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DynamicSemaphore 测试类
 *
 * @author Wu, Hongqiang
 * @since 2026-01-27
 */
@DisplayName("DynamicSemaphore 测试")
class DynamicSemaphoreTest {

    private DynamicSemaphore semaphore;

    @BeforeEach
    void setUp() {
        semaphore = new DynamicSemaphore(5);
    }

    @Test
    @DisplayName("测试构造函数初始化")
    void testConstructor() {
        var sem = new DynamicSemaphore(10);

        assertEquals(10, sem.getCurrentPermits(), "初始许可数应为10");
        assertEquals(10, sem.availablePermits(), "可用许可数应为10");
    }

    @Test
    @DisplayName("测试getCurrentPermits方法")
    void testGetCurrentPermits() {
        assertEquals(5, semaphore.getCurrentPermits(), "getCurrentPermits应返回当前许可数");

        semaphore.updatePermits(10);
        assertEquals(10, semaphore.getCurrentPermits(), "更新后getCurrentPermits应返回新的许可数");

        semaphore.updatePermits(3);
        assertEquals(3, semaphore.getCurrentPermits(), "减少后getCurrentPermits应返回新的许可数");
    }

    @Test
    @DisplayName("测试增加许可数")
    void testUpdatePermits_Increase() {
        assertEquals(5, semaphore.availablePermits(), "初始可用许可数应为5");

        semaphore.updatePermits(10);

        assertEquals(10, semaphore.getCurrentPermits(), "当前许可数应更新为10");
        assertEquals(10, semaphore.availablePermits(), "可用许可数应增加到10");
    }

    @Test
    @DisplayName("测试减少许可数")
    void testUpdatePermits_Decrease() {
        assertEquals(5, semaphore.availablePermits(), "初始可用许可数应为5");

        semaphore.updatePermits(3);

        assertEquals(3, semaphore.getCurrentPermits(), "当前许可数应更新为3");
        assertEquals(3, semaphore.availablePermits(), "可用许可数应减少到3");
    }

    @Test
    @DisplayName("测试许可数不变")
    void testUpdatePermits_NoChange() {
        assertEquals(5, semaphore.availablePermits(), "初始可用许可数应为5");

        semaphore.updatePermits(5);

        assertEquals(5, semaphore.getCurrentPermits(), "当前许可数应保持为5");
        assertEquals(5, semaphore.availablePermits(), "可用许可数应保持为5");
    }

    @Test
    @DisplayName("测试增加许可数后能获取新增的许可")
    void testUpdatePermits_Increase_CanAcquireMore() throws InterruptedException {
        // 先获取所有5个许可
        assertTrue(semaphore.tryAcquire(5), "应该能获取5个许可");
        assertEquals(0, semaphore.availablePermits(), "可用许可数应为0");

        // 增加到10个许可
        semaphore.updatePermits(10);

        assertEquals(5, semaphore.availablePermits(), "应该有5个新增的可用许可");
        assertTrue(semaphore.tryAcquire(5), "应该能获取新增的5个许可");
        assertEquals(0, semaphore.availablePermits(), "获取后可用许可数应为0");
    }

    @Test
    @DisplayName("测试减少许可数后不影响已获取的许可")
    void testUpdatePermits_Decrease_AcquiredNotAffected() throws InterruptedException {
        // 先获取3个许可
        assertTrue(semaphore.tryAcquire(3), "应该能获取3个许可");
        assertEquals(2, semaphore.availablePermits(), "可用许可数应为2");

        // 减少到4个许可（已获取3个，只剩1个可用）
        semaphore.updatePermits(4);

        assertEquals(4, semaphore.getCurrentPermits(), "当前许可数应为4");
        assertEquals(1, semaphore.availablePermits(), "可用许可数应为1");
    }

    @Test
    @DisplayName("测试从0增加许可数")
    void testUpdatePermits_IncreaseFromZero() throws InterruptedException {
        // 先获取所有许可
        assertTrue(semaphore.tryAcquire(5), "应该能获取5个许可");
        assertEquals(0, semaphore.availablePermits(), "可用许可数应为0");

        // 从5增加到8
        semaphore.updatePermits(8);

        assertEquals(8, semaphore.getCurrentPermits(), "当前许可数应为8");
        assertEquals(3, semaphore.availablePermits(), "可用许可数应为3");
    }

    @Test
    @DisplayName("测试减少到0")
    void testUpdatePermits_DecreaseToZero() {
        semaphore.updatePermits(0);

        assertEquals(0, semaphore.getCurrentPermits(), "当前许可数应为0");
        assertEquals(0, semaphore.availablePermits(), "可用许可数应为0");
        assertFalse(semaphore.tryAcquire(), "不应该能获取许可");
    }

    @Test
    @DisplayName("测试多次增加许可数")
    void testUpdatePermits_MultipleIncreases() {
        semaphore.updatePermits(10);
        assertEquals(10, semaphore.getCurrentPermits(), "第一次增加后许可数应为10");

        semaphore.updatePermits(15);
        assertEquals(15, semaphore.getCurrentPermits(), "第二次增加后许可数应为15");

        semaphore.updatePermits(20);
        assertEquals(20, semaphore.getCurrentPermits(), "第三次增加后许可数应为20");
        assertEquals(20, semaphore.availablePermits(), "可用许可数应为20");
    }

    @Test
    @DisplayName("测试多次减少许可数")
    void testUpdatePermits_MultipleDecreases() {
        semaphore.updatePermits(10);
        assertEquals(10, semaphore.getCurrentPermits(), "初始许可数应为10");

        semaphore.updatePermits(8);
        assertEquals(8, semaphore.getCurrentPermits(), "第一次减少后许可数应为8");

        semaphore.updatePermits(5);
        assertEquals(5, semaphore.getCurrentPermits(), "第二次减少后许可数应为5");

        semaphore.updatePermits(2);
        assertEquals(2, semaphore.getCurrentPermits(), "第三次减少后许可数应为2");
        assertEquals(2, semaphore.availablePermits(), "可用许可数应为2");
    }

    @Test
    @DisplayName("测试增加和减少交替")
    void testUpdatePermits_AlternateIncreaseDecrease() {
        semaphore.updatePermits(10);
        assertEquals(10, semaphore.getCurrentPermits(), "增加后许可数应为10");

        semaphore.updatePermits(5);
        assertEquals(5, semaphore.getCurrentPermits(), "减少后许可数应为5");

        semaphore.updatePermits(15);
        assertEquals(15, semaphore.getCurrentPermits(), "再次增加后许可数应为15");

        semaphore.updatePermits(8);
        assertEquals(8, semaphore.getCurrentPermits(), "再次减少后许可数应为8");
        assertEquals(8, semaphore.availablePermits(), "可用许可数应为8");
    }

    @Test
    @DisplayName("测试并发场景：多线程同时获取许可")
    void testConcurrentAcquire() throws InterruptedException {
        var semaphore10 = new DynamicSemaphore(10);
        var successCount = new AtomicInteger(0);
        var threadCount = 20;
        var latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                try {
                    if (semaphore10.tryAcquire(1, TimeUnit.SECONDS)) {
                        successCount.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    latch.countDown();
                }
            }).start();
        }

        assertTrue(latch.await(5, TimeUnit.SECONDS), "所有线程应在5秒内完成");
        assertEquals(10, successCount.get(), "应该只有10个线程成功获取到许可");
        assertEquals(0, semaphore10.availablePermits(), "所有许可应被获取");
    }

    @Test
    @DisplayName("测试并发场景：动态增加许可后更多线程能获取")
    void testConcurrentAcquire_WithDynamicIncrease() throws InterruptedException {
        var semaphore5 = new DynamicSemaphore(5);
        var successCount = new AtomicInteger(0);
        var threadCount = 10;
        var latch = new CountDownLatch(threadCount);
        var startLatch = new CountDownLatch(1);

        // 启动10个线程尝试获取许可
        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                try {
                    startLatch.await(); // 等待统一开始
                    if (semaphore5.tryAcquire(2, TimeUnit.SECONDS)) {
                        successCount.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    latch.countDown();
                }
            }).start();
        }

        Thread.sleep(100); // 等待线程启动
        startLatch.countDown(); // 统一开始

        Thread.sleep(200); // 等待前5个线程获取许可

        // 动态增加许可到10
        semaphore5.updatePermits(10);

        assertTrue(latch.await(5, TimeUnit.SECONDS), "所有线程应在5秒内完成");
        assertEquals(10, successCount.get(), "应该有10个线程成功获取到许可");
    }

    @Test
    @DisplayName("测试边界值：许可数为1")
    void testBoundary_PermitsOne() {
        var sem = new DynamicSemaphore(1);

        assertEquals(1, sem.getCurrentPermits(), "许可数应为1");
        assertTrue(sem.tryAcquire(), "应该能获取1个许可");
        assertFalse(sem.tryAcquire(), "不应该能再获取许可");

        sem.release();
        assertTrue(sem.tryAcquire(), "释放后应该能再次获取许可");
    }

    @Test
    @DisplayName("测试边界值：许可数为0")
    void testBoundary_PermitsZero() {
        var sem = new DynamicSemaphore(0);

        assertEquals(0, sem.getCurrentPermits(), "许可数应为0");
        assertEquals(0, sem.availablePermits(), "可用许可数应为0");
        assertFalse(sem.tryAcquire(), "不应该能获取许可");
    }

    @Test
    @DisplayName("测试边界值：大量许可数")
    void testBoundary_LargePermits() {
        var sem = new DynamicSemaphore(10000);

        assertEquals(10000, sem.getCurrentPermits(), "许可数应为10000");
        assertEquals(10000, sem.availablePermits(), "可用许可数应为10000");
        assertTrue(sem.tryAcquire(5000), "应该能获取5000个许可");
        assertEquals(5000, sem.availablePermits(), "剩余可用许可数应为5000");
    }

    @Test
    @DisplayName("测试极端场景：从大量减少到0")
    void testExtreme_LargeToZero() {
        var sem = new DynamicSemaphore(1000);
        assertEquals(1000, sem.getCurrentPermits(), "初始许可数应为1000");

        sem.updatePermits(0);
        assertEquals(0, sem.getCurrentPermits(), "许可数应减少到0");
        assertEquals(0, sem.availablePermits(), "可用许可数应为0");
    }

    @Test
    @DisplayName("测试极端场景：从0增加到大量")
    void testExtreme_ZeroToLarge() {
        var sem = new DynamicSemaphore(0);
        assertEquals(0, sem.getCurrentPermits(), "初始许可数应为0");

        sem.updatePermits(1000);
        assertEquals(1000, sem.getCurrentPermits(), "许可数应增加到1000");
        assertEquals(1000, sem.availablePermits(), "可用许可数应为1000");
    }

    @Test
    @DisplayName("测试连续相同值更新")
    void testUpdatePermits_SameValueMultipleTimes() {
        semaphore.updatePermits(5);
        assertEquals(5, semaphore.getCurrentPermits(), "许可数应保持为5");
        assertEquals(5, semaphore.availablePermits(), "可用许可数应保持为5");

        semaphore.updatePermits(5);
        assertEquals(5, semaphore.getCurrentPermits(), "许可数应保持为5");
        assertEquals(5, semaphore.availablePermits(), "可用许可数应保持为5");

        semaphore.updatePermits(5);
        assertEquals(5, semaphore.getCurrentPermits(), "许可数应保持为5");
        assertEquals(5, semaphore.availablePermits(), "可用许可数应保持为5");
    }
}
