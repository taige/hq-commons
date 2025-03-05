package io.hqwu.commons.util;

import org.junit.jupiter.api.Test;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created with IntelliJ IDEA.
 *
 * Date: 13-10-29
 * Time: 下午4:31
 */
public class LoggerBaseTest {
    Logger logger = new Logger();

    @Test
    public void testAllf() throws Exception {
        logger.trace("%s", "allf");
    }

    @Test
    public void testAll1() throws Exception {
        logger.trace("all1");
    }

    @Test
    public void testAll2() throws Exception {
        logger.trace("all", 2);
    }

    @Test
    public void testDebugf() throws Exception {
        logger.debug("debugf %s-%s", "a", 4);
    }

    @Test
    public void testDebug1() throws Exception {
        logger.debug("debug1");
    }

    @Test
    public void testDebug2() throws Exception {
        logger.debug("debug", 2);
    }

    @Test
    public void testInfof() throws Exception {
        logger.info("infof %s-%b", "is", false);
    }

    @Test
    public void testInfo1() throws Exception {
        logger.info("info1");
    }

    @Test
    public void testInfo2() throws Exception {
        logger.info("info", 2);
    }

    @Test
    public void testWarnf() throws Exception {
        logger.warnf("warnf %s", new Exception());
        logger.warn("warnf %s", new Exception());
    }

    @Test
    public void testWarn1() throws Exception {
        logger.warn(new Exception());
    }

    @Test
    public void testWarn2() throws Exception {
        logger.warn("E: ", new Exception());
    }

    @Test
    public void testErrorf() throws Exception {
        logger.errorf("errorf %s", new Exception());
        logger.error("errorf %s", new Exception());
    }

    @Test
    public void testError1() throws Exception {
        logger.error(new Exception());
    }

    @Test
    public void testError2() throws Exception {
        logger.error("E: " + new Exception());
    }

    @Test
    public void testAccess() throws Exception {
        Logger.accessStart();
        Logger.access("abc");
    }

    @Test
    public void testAccess1() throws Exception {
        Logger.accessStart();
        Logger.access("abc", "abc");
    }

    @Test
    public void testAccess2() throws Exception {
        Logger.access("abc", "abc");
    }

    static ThreadLocal<Long> ss = new ThreadLocal<Long>();

    @Test
    public void testThreadLocal() throws Exception {
        final CountDownLatch cdl = new CountDownLatch(100);
        final CyclicBarrier barrier = new CyclicBarrier(100);
        final AtomicLong totalUse = new AtomicLong(0);
        final int tc = 100, count = 100000;
        for (int i = 0; i < tc; i++) {
            new Thread() {
                public void run() {
                    try {
                        barrier.await();
                    } catch (InterruptedException e) {
                    } catch (BrokenBarrierException e) {
                    }
                    long start = System.nanoTime();
                    for (int jj = 0; jj < count; jj++) {
                        ss.set(System.nanoTime());
                        ss.get();
                        ss.remove();
                    }
                    long use = System.nanoTime() - start;
                    totalUse.addAndGet(use);
                    logger.info("用时: " + (use));
                    cdl.countDown();
                }
            }.start();
        }
        cdl.await();
        logger.info("平均用时: " + (totalUse.get() / tc / count));
    }

    @Test
    public void testTimeSpentMillSec() {
        for (int i = 0; i < 10; i++) {
            long startTime = System.currentTimeMillis();
            TimeUtil.sleepMilliSec(i);
            Logger.timeSpentMillSec("test", startTime, 2);
        }
//        System.out.println("===================================");
        for (int i = 0; i < 10; i++) {
            long startTime = System.currentTimeMillis();
            TimeUtil.sleepMilliSec(i);
            Logger.timeSpentMillSec("test", startTime, 2, 2);
        }
//        System.out.println("===================================");
    }

    @Test
    public void testTimeSpentNanSec() {
        final long sleepNanSec = 1000000;
        for (int i = 0; i < 10; i++) {
            long startTime = System.nanoTime();
            TimeUtil.sleepNanoSec(sleepNanSec);
            Logger.timeSpentNan("test", startTime, sleepNanSec);
        }
//        System.out.println("===================================");
        for (int i = 0; i < 10; i++) {
            long startTime = System.nanoTime();
            String s = "";
            for (int j = 0; j < 24; j++) {
                 s += j;
            }
            if (s.length()>100) {

            }
            Logger.timeSpentNan("test", startTime, sleepNanSec, sleepNanSec);
        }
//        System.out.println("===================================");
    }
}
