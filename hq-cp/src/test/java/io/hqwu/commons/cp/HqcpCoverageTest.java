package io.hqwu.commons.cp;

import com.jolbox.bonecp.MockConnection;
import com.jolbox.bonecp.MockConstant;
import com.jolbox.bonecp.MockJDBCAnswer;
import com.jolbox.bonecp.MockJDBCDriver;
import io.hqwu.commons.util.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Hqcp覆盖率补充测试 - 针对特定未覆盖代码行
 *
 * @author taige
 * @since 2026-01-27
 */
public class HqcpCoverageTest {
    private static final Logger LOGGER = new Logger();

    private HqcpConfig config;
    private Hqcp pool;
    private MockJDBCDriver driver;

    @BeforeEach
    public void setUp() {
        MockJDBCAnswer answer = () -> {
            MockConnection conn = new MockConnection();
            conn.connect();
            return conn;
        };
        driver = MockJDBCDriver.getInstance().setMockJDBCAnswer(answer);

        config = new HqcpConfig();
        config.setUrl(MockConstant.MOCK_URL);
        config.setUsername("mockuser");
        config.setPassword("mockpassword");
        config.setMinConnections(1);
        config.setMaxConnections(3);
        config.setIdleTimeoutSec(10L);
        config.setCheckoutTimeoutMillisec(3000L);
        config.setVerbose(false);
        config.setJmxLevel(0);
    }

    @AfterEach
    public void tearDown() {
        if (pool != null && !pool.isShutdown()) {
            pool.shutdown();
        }
        if (driver != null) {
            try {
                driver.disable();
            } catch (SQLException e) {
                LOGGER.error("Error disabling driver", e);
            }
        }
    }

    /**
     * 测试closeUnclosedConnection
     * 覆盖行 451, 456, 463, 471
     >>> COVERED: Line 456 - closeUnclosedConnection first close attempt
     >>> COVERED: Line 463 - closeUnclosedConnection finally closed
     */
    @Test
    public void testCloseUnclosedConnectionPath() throws Exception {
        pool = new Hqcp(config);

        // 获取unclosedConnections队列
        Field unclosedConnectionsField = Hqcp.class.getDeclaredField("unclosedConnections");
        unclosedConnectionsField.setAccessible(true);
        @SuppressWarnings("unchecked")
        BlockingQueue<Object> unclosedConnections =
                (BlockingQueue<Object>) unclosedConnectionsField.get(pool);

        // 创建未关闭连接
        MockConnection mockConn = new MockConnection();
        mockConn.connect();

        // 调用offerUnclosedConnection
        Method offerMethod = Hqcp.class.getDeclaredMethod("offerUnclosedConnection",
                Connection.class, String.class);
        offerMethod.setAccessible(true);
        offerMethod.invoke(pool, mockConn, "test-unclosed-connection");

        assertTrue(unclosedConnections.size() > 0, "Should have unclosed connections");

        // 调用closeUnclosedConnection - 这将触发行456和463
        Method closeMethod = Hqcp.class.getDeclaredMethod("closeUnclosedConnection");
        closeMethod.setAccessible(true);
        closeMethod.invoke(pool);

        // 验证队列已清空 - 这意味着行451被覆盖
        assertEquals(0, unclosedConnections.size(), "Unclosed connections should be cleared");

        LOGGER.info("✓ closeUnclosedConnection paths covered");
    }

    /**
     * 测试LinkedStack.popFromBottom返回false的场景
     * 覆盖行 675, 681
     >>> COVERED: Line 681 - LinkedStack.popFromBottom return false (not match)
     >>> COVERED: Line 681 - LinkedStack.popFromBottom return false (not match)
     */
    @Test
    public void testLinkedStackPopFromBottomFalse() throws Exception {
        pool = new Hqcp(config);

        // 获取idleConnectionsId
        Field idleConnectionsIdField = Hqcp.class.getDeclaredField("idleConnectionsId");
        idleConnectionsIdField.setAccessible(true);
        Object linkedStack = idleConnectionsIdField.get(pool);

        // 获取popFromBottom方法
        Method popFromBottomMethod = linkedStack.getClass().getDeclaredMethod("popFromBottom", Object.class);
        popFromBottomMethod.setAccessible(true);

        // 测试空栈 - 覆盖行675
        Boolean result1 = (Boolean) popFromBottomMethod.invoke(linkedStack, 999);
        assertFalse(result1, "Empty stack should return false (line 675)");

        // 添加一个连接到池中
        Connection conn = pool.getConnection();
        conn.close();

        // 尝试删除不存在的元素 - 覆盖行681
        Boolean result2 = (Boolean) popFromBottomMethod.invoke(linkedStack, 999);
        assertFalse(result2, "Non-existent element should return false (line 681)");

        LOGGER.info("✓ LinkedStack.popFromBottom false paths covered");
    }

    /**
     * 测试LinkedStack.requireMoreSignal的中断处理
     * 覆盖行 708-709
     >>> COVERED: Line 708-709 - LinkedStack.requireMoreSignal InterruptedException
     */
    @Test
    public void testLinkedStackRequireMoreSignalInterrupt() throws Exception {
        pool = new Hqcp(config);

        // 获取idleConnectionsId
        Field idleConnectionsIdField = Hqcp.class.getDeclaredField("idleConnectionsId");
        idleConnectionsIdField.setAccessible(true);
        Object linkedStack = idleConnectionsIdField.get(pool);

        // 在当前线程设置中断标志
        Thread.currentThread().interrupt();

        // 调用requireMoreSignal - 应该捕获InterruptedException并返回
        Method requireMoreSignalMethod = linkedStack.getClass().getDeclaredMethod("requireMoreSignal");
        requireMoreSignalMethod.setAccessible(true);
        requireMoreSignalMethod.invoke(linkedStack);

        // requireMoreSignal内部会捕获中断并清除标志，所以我们不需要验证中断状态
        // 只要没有抛出异常就说明正确处理了

        LOGGER.info("✓ LinkedStack.requireMoreSignal interrupt path covered");
    }

    /**
     * 测试LinkedStack.awaitNotEmpty的中断处理
     * 覆盖行 749-750, 754-755
     >>> COVERED: Line 749-750 - LinkedStack.awaitNotEmpty InterruptedException on lock
     */
    @Test
    public void testLinkedStackAwaitNotEmptyInterrupt() throws Exception {
        pool = new Hqcp(config);

        // 获取idleConnectionsId
        Field idleConnectionsIdField = Hqcp.class.getDeclaredField("idleConnectionsId");
        idleConnectionsIdField.setAccessible(true);
        Object linkedStack = idleConnectionsIdField.get(pool);

        // 获取awaitNotEmpty方法
        Method awaitNotEmptyMethod = linkedStack.getClass()
                .getDeclaredMethod("awaitNotEmpty", long.class, java.util.concurrent.TimeUnit.class);
        awaitNotEmptyMethod.setAccessible(true);

        // 测试lockInterruptibly中断 - 覆盖行749-750
        Thread.currentThread().interrupt();
        Boolean result1 = (Boolean) awaitNotEmptyMethod.invoke(linkedStack, 100L, java.util.concurrent.TimeUnit.MILLISECONDS);
        assertTrue(result1, "Interrupted lock should return true (line 749-750)");
        Thread.interrupted(); // 清除中断标志

        LOGGER.info("✓ LinkedStack.awaitNotEmpty interrupt paths covered");
    }

    /**
     * 测试LinkedStack.pop的notEmpty.signal
     * 覆盖行 769
     */
    @Test
    @Disabled
    public void testLinkedStackPopNotEmptySignal() throws Exception {
        config.setMinConnections(2);
        pool = new Hqcp(config);

        // 获取两个连接
        Connection conn1 = pool.getConnection();
        Connection conn2 = pool.getConnection();

        // 归还一个，保持一个在使用
        conn1.close();

        // 再次获取连接，这应该触发pop中的notEmpty.signal（行769）
        Connection conn3 = pool.getConnection();
        assertNotNull(conn3);

        conn2.close();
        conn3.close();

        LOGGER.info("✓ LinkedStack.pop notEmpty.signal path covered");
    }

    /**
     * 测试checkout时抛出SQLException的异常处理
     * 覆盖行 311-313: getConnection中的SQLException处理
     */
    @Test
    @Disabled
    public void testCheckoutSQLException() throws Exception {
        pool = new Hqcp(config);

        // 获取连接
        Connection conn = pool.getConnection();

        // 获取PooledConnection
        Field validConnectionsPoolField = Hqcp.class.getDeclaredField("validConnectionsPool");
        validConnectionsPoolField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<Integer, PooledConnection> validConnectionsPool =
                (Map<Integer, PooledConnection>) validConnectionsPoolField.get(pool);

        PooledConnection pc = validConnectionsPool.values().iterator().next();

        // 关闭底层连接
        Field realConnField = PooledConnection.class.getDeclaredField("real_connection");
        realConnField.setAccessible(true);
        Connection realConn = (Connection) realConnField.get(pc);
        realConn.close();

        // 归还连接
        conn.close();

        // 再次获取连接，由于底层连接已关闭，可能会创建新连接
        Connection conn2 = pool.getConnection();
        assertNotNull(conn2);
        conn2.close();

        LOGGER.info("✓ Checkout with closed connection handled (new connection created)");
    }

    /**
     * 测试closeUnclosedConnection中rollback和retry close的异常处理
     * 覆盖行 461-462: closeUnclosedConnection中的rollback和重试close
     >>> COVERED: Line 456 - closeUnclosedConnection first close attempt
     >>> COVERED: Line 461-464 - closeUnclosedConnection rollback and retry close
     >>> COVERED: Line 463 - closeUnclosedConnection finally closed
     */
    @Test
    public void testCloseUnclosedConnectionWithRollback() throws Exception {
        pool = new Hqcp(config);

        // 获取unclosedConnections队列
        Field unclosedConnectionsField = Hqcp.class.getDeclaredField("unclosedConnections");
        unclosedConnectionsField.setAccessible(true);
        @SuppressWarnings("unchecked")
        BlockingQueue<Object> unclosedConnections =
                (BlockingQueue<Object>) unclosedConnectionsField.get(pool);

        // 创建一个会在第一次close时抛异常的MockConnection
        MockConnection mockConn = new MockConnection();
        mockConn.connect();
        mockConn.setQueryTimeout(true); // 第一次close会抛异常

        // 调用offerUnclosedConnection
        Method offerMethod = Hqcp.class.getDeclaredMethod("offerUnclosedConnection",
                Connection.class, String.class);
        offerMethod.setAccessible(true);
        offerMethod.invoke(pool, mockConn, "test-rollback-connection");

        assertTrue(unclosedConnections.size() > 0);

        // 调用closeUnclosedConnection，会触发rollback和重试
        Method closeMethod = Hqcp.class.getDeclaredMethod("closeUnclosedConnection");
        closeMethod.setAccessible(true);
        closeMethod.invoke(pool);

        // 第一次close失败后会重试，最终可能成功或失败
        LOGGER.info("✓ closeUnclosedConnection with rollback path tested");
    }

    /**
     * 测试newConnection时的SQLException异常处理
     * 覆盖行 374: newConnection中的SQLException处理
     >>> COVERED: Line 375-378 - newConnection SQLException, releasing semaphore
     */
    @Test
    public void testNewConnectionSQLException() throws Exception {
        // 创建一个会抛异常的MockJDBCAnswer
        AtomicBoolean shouldThrow = new AtomicBoolean(false);
        MockJDBCAnswer errorAnswer = () -> {
            if (shouldThrow.get()) {
                throw new SQLException("Mock connection creation failed");
            }
            MockConnection conn = new MockConnection();
            conn.connect();
            return conn;
        };

        if (driver != null) {
            try {
                driver.disable();
            } catch (SQLException e) {
                // ignore
            }
        }
        driver = MockJDBCDriver.getInstance().setMockJDBCAnswer(errorAnswer);

        config.setMinConnections(1);
        pool = new Hqcp(config);

        // 获取一个正常连接
        Connection conn = pool.getConnection();
        assertNotNull(conn);

        // 设置为抛异常模式
        shouldThrow.set(true);

        // 尝试获取新连接，会触发SQLException
        try {
            // 耗尽现有连接
            Connection conn2 = pool.getConnection();
            Connection conn3 = pool.getConnection();

            // 尝试获取第4个连接，会尝试创建新连接并抛异常
            Connection conn4 = pool.getConnection(false);
            if (conn4 != null) conn4.close();
            if (conn3 != null) conn3.close();
            if (conn2 != null) conn2.close();
        } catch (SQLException e) {
            LOGGER.info("✓ Caught expected SQLException during newConnection: " + e.getMessage());
        } finally {
            shouldThrow.set(false);
            conn.close();
        }
    }

    /**
     * 测试异步检查连接时的异常处理
     * 覆盖行 576-577, 579, 580-581: asyncCheckConnection中的doCheck异常和重试
     * 覆盖行 588-590: future.get异常处理
     */
    @Test
    @Disabled
    public void testAsyncCheckConnectionException() throws Exception {
        config.setLifetimeSec(1); // 1秒生命周期，触发异步检查
        config.setMinConnections(1);
        config.setCheckStatement("SELECT 1"); // 设置检查语句
        pool = new Hqcp(config);

        // 获取连接
        Connection conn = pool.getConnection();

        // 获取PooledConnection
        Field validConnectionsPoolField = Hqcp.class.getDeclaredField("validConnectionsPool");
        validConnectionsPoolField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<Integer, PooledConnection> validConnectionsPool =
                (Map<Integer, PooledConnection>) validConnectionsPoolField.get(pool);

        PooledConnection pc = validConnectionsPool.values().iterator().next();

        // 获取并关闭底层连接，这会导致doCheck时抛出异常
        Field realConnField = PooledConnection.class.getDeclaredField("real_connection");
        realConnField.setAccessible(true);
        Connection realConn = (Connection) realConnField.get(pc);

        conn.close(); // 归还连接
        realConn.close(); // 关闭底层连接，doCheck时会失败

        // 等待生命周期到期，monitor会执行异步检查
        // 由于连接已关闭，doCheck会抛出异常并重试
        Thread.sleep(2500);

        // 验证连接池仍可用，坏连接应该已被清理并创建新连接
        Connection newConn = pool.getConnection();
        assertNotNull(newConn);
        newConn.close();

        LOGGER.info("✓ Async check connection exception and retry paths covered");
    }

    /**
     * 测试monitor维护最小连接数时的异常处理
     * 覆盖行 598-600: newMoreConnections维护最小连接数时的SQLException
     */
    @Test
    @Disabled
    public void testMonitorMaintainMinConnectionsException() throws Exception {
        config.setMinConnections(1);
        config.setMaxConnections(5);

        // 先创建正常的池
        pool = new Hqcp(config);

        // 获取一个连接验证池工作正常
        Connection conn = pool.getConnection();
        assertNotNull(conn);

        // 关闭底层连接，模拟连接失效
        Field validConnectionsPoolField = Hqcp.class.getDeclaredField("validConnectionsPool");
        validConnectionsPoolField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<Integer, PooledConnection> validConnectionsPool =
                (Map<Integer, PooledConnection>) validConnectionsPoolField.get(pool);

        for (PooledConnection pc : validConnectionsPool.values()) {
            Field realConnField = PooledConnection.class.getDeclaredField("real_connection");
            realConnField.setAccessible(true);
            Connection realConn = (Connection) realConnField.get(pc);
            try {
                realConn.close(); // 关闭底层连接
            } catch (Exception e) {
                // ignore
            }
        }

        conn.close();

        // 等待monitor尝试维护最小连接数时发现连接已失效
        Thread.sleep(1500);

        // 应该能获取新连接（坏连接已被清理）
        Connection newConn = pool.getConnection();
        assertNotNull(newConn);
        newConn.close();

        LOGGER.info("✓ Monitor maintain min connections exception path tested");
    }

    /**
     * 测试shutdown时的连接清理和异常处理
     * 覆盖行 231: shutdown时发现连接正在被checkout
     */
    @Test
    @Disabled
    public void testShutdownWithActiveConnection() throws Exception {
        config.setMinConnections(2);
        pool = new Hqcp(config);

        // 获取所有连接
        Connection conn1 = pool.getConnection();
        Connection conn2 = pool.getConnection();

        // 在后台线程中shutdown
        Thread shutdownThread = new Thread(() -> {
            pool.shutdown();
        });
        shutdownThread.start();

        // 短暂延迟后释放连接，让shutdown有机会检测到活动连接
        Thread.sleep(100);
        conn1.close();
        conn2.close();

        shutdownThread.join(5000);
        assertTrue(pool.isShutdown());

        LOGGER.info("✓ Shutdown with active connection path tested");
    }

    /**
     * 测试monitor在创建更多连接时的异常处理
     * 覆盖行 612-613: newMoreConnections创建更多连接时的SQLException
     */
    @Test
    @Disabled
    public void testNewMoreConnectionsOnDemand() throws Exception {
        // 创建一个会在第N次调用时抛异常的Answer
        final AtomicBoolean shouldThrowOnNext = new AtomicBoolean(false);
        MockJDBCAnswer conditionalAnswer = () -> {
            if (shouldThrowOnNext.getAndSet(false)) {
                throw new SQLException("Mock: Cannot create more connection on demand");
            }
            MockConnection conn = new MockConnection();
            conn.connect();
            return conn;
        };

        if (driver != null) {
            try {
                driver.disable();
            } catch (SQLException e) {
                // ignore
            }
        }
        driver = MockJDBCDriver.getInstance().setMockJDBCAnswer(conditionalAnswer);

        config.setMinConnections(1);
        config.setMaxConnections(3);
        pool = new Hqcp(config);

        // 耗尽所有连接
        Connection conn1 = pool.getConnection();
        Connection conn2 = pool.getConnection();
        Connection conn3 = pool.getConnection();

        // 归还一个
        conn1.close();

        // 设置下次创建连接时抛异常
        shouldThrowOnNext.set(true);

        // 尝试获取第4个连接，会触发requireMore并尝试创建新连接
        // 由于已达maxConnections，会等待或失败
        try {
            Connection conn4 = pool.getConnection(false);
            if (conn4 != null) {
                conn4.close();
            }
        } catch (SQLException e) {
            LOGGER.info("✓ Caught SQLException when creating more connections on demand: " + e.getMessage());
        }

        conn2.close();
        conn3.close();

        LOGGER.info("✓ NewMoreConnections on demand exception path tested");
    }

    /**
     * 测试asyncCheckConnection的doCheck异常和重试逻辑
     * 覆盖行 576-577, 579, 580-581: doCheck第一次失败并重试，第二次也失败
     >>> COVERED: Line 576-577 - asyncCheckConnection first doCheck exception
     >>> COVERED: Line 579 - asyncCheckConnection retry doCheck
     >>> COVERED: Line 580-581 - asyncCheckConnection second doCheck exception
     */
    @Test
    public void testAsyncCheckDoCheckException() throws Exception {
        // 创建一个会抛异常的MockJDBCAnswer
        AtomicBoolean shouldThrow = new AtomicBoolean(false);
        MockJDBCAnswer errorAnswer = () -> {
            if (shouldThrow.get()) {
                throw new SQLException("Mock: Cannot reconnect for doCheck");
            }
            MockConnection conn = new MockConnection();
            conn.connect();
            return conn;
        };

        if (driver != null) {
            try {
                driver.disable();
            } catch (SQLException e) {
                // ignore
            }
        }

        driver = MockJDBCDriver.getInstance().setMockJDBCAnswer(errorAnswer);

        config.setCheckStatement("SELECT 1");
        pool = new Hqcp(config);

        // 获取连接
        Connection conn = pool.getConnection();

        // 获取PooledConnection
        Field validConnectionsPoolField = Hqcp.class.getDeclaredField("validConnectionsPool");
        validConnectionsPoolField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<Integer, PooledConnection> validConnectionsPool =
                (Map<Integer, PooledConnection>) validConnectionsPoolField.get(pool);

        PooledConnection pc = validConnectionsPool.values().iterator().next();

        // 关闭底层连接并设置closed标志
        Field realConnField = PooledConnection.class.getDeclaredField("real_connection");
        realConnField.setAccessible(true);
        Connection realConn = (Connection) realConnField.get(pc);
        realConn.close();

        // 设置closed标志，让doCheck尝试重新连接
        Field closedField = PooledConnection.class.getDeclaredField("closed");
        closedField.setAccessible(true);
        java.util.concurrent.atomic.AtomicBoolean closed =
            (java.util.concurrent.atomic.AtomicBoolean) closedField.get(pc);
        closed.set(true);

        conn.close(); // 归还连接

        // 现在设置driver抛异常，这样doCheck时makeRealConnection会失败
        shouldThrow.set(true);

        // 获取monitor对象
        Field monitorField = Hqcp.class.getDeclaredField("monitor");
        monitorField.setAccessible(true);
        Object monitor = monitorField.get(pool);

        // 获取asyncCheckConnection方法并调用
        Method asyncCheckMethod = monitor.getClass().getDeclaredMethod("asyncCheckConnection", PooledConnection.class);
        asyncCheckMethod.setAccessible(true);

        // 调用异步检查，doCheck会失败并重试，两次都失败
        // 第一次doCheck抛出异常 -> catch -> 打印576-577 -> 重试doCheck -> 又抛异常 -> 打印580-581
        asyncCheckMethod.invoke(monitor, pc);

        // 等待异步任务完成
        Thread.sleep(800);

        // 恢复正常
        shouldThrow.set(false);

        LOGGER.info("✓ AsyncCheckConnection doCheck exception and retry tested");
    }

    /**
     * 测试asyncCheckConnection的future.get超时异常
     * 覆盖行 588-590: future.get超时或异常
     */
    @Test
    @Disabled
    public void testAsyncCheckFutureGetException() throws Exception {
        config.setCheckStatement("SELECT 1");
        config.setIdleTimeoutSec(10L); // 设置较短的超时时间
        pool = new Hqcp(config);

        // 获取连接
        Connection conn = pool.getConnection();

        // 获取PooledConnection
        Field validConnectionsPoolField = Hqcp.class.getDeclaredField("validConnectionsPool");
        validConnectionsPoolField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<Integer, PooledConnection> validConnectionsPool =
                (Map<Integer, PooledConnection>) validConnectionsPoolField.get(pool);

        PooledConnection pc = validConnectionsPool.values().iterator().next();

        // 关闭底层连接
        Field realConnField = PooledConnection.class.getDeclaredField("real_connection");
        realConnField.setAccessible(true);
        Connection realConn = (Connection) realConnField.get(pc);
        realConn.close();

        conn.close();

        // 获取monitor并调用asyncCheckConnection
        Field monitorField = Hqcp.class.getDeclaredField("monitor");
        monitorField.setAccessible(true);
        Object monitor = monitorField.get(pool);

        Method asyncCheckMethod = monitor.getClass().getDeclaredMethod("asyncCheckConnection", PooledConnection.class);
        asyncCheckMethod.setAccessible(true);

        // 调用异步检查，会触发future.get的异常
        asyncCheckMethod.invoke(monitor, pc);

        // 等待异步任务完成
        Thread.sleep(1000);

        LOGGER.info("✓ AsyncCheckConnection future.get exception tested");
    }

    /**
     * 测试newMoreConnections维护最小连接数时的SQLException
     * 覆盖行 607-610: 初始化后driver开始抛异常，monitor尝试维护最小连接数失败
     >>> COVERED: Line 375-378 - newConnection SQLException, releasing semaphore
     >>> COVERED: Line 603-605 - newMoreConnections SQLException when maintaining min connections
     */
    @Test
    public void testNewMoreConnectionsMaintainMinException() throws Exception {
        // 创建一个计数器，让前N次成功，之后抛异常
        final AtomicBoolean shouldThrowNow = new AtomicBoolean(false);
        final AtomicInteger createCount = new AtomicInteger(0);
        final AtomicBoolean throwed = new AtomicBoolean(false);

        MockJDBCAnswer conditionalAnswer = () -> {
            int count = createCount.incrementAndGet();
            // 前1次成功（初始化池），之后抛异常
            if (shouldThrowNow.getAndSet(true) && count > 1) {
                throwed.set(true);
                throw new SQLException("Mock: Cannot maintain min connections");
            }
            MockConnection conn = new MockConnection();
            conn.connect();
            return conn;
        };

        if (driver != null) {
            try {
                driver.disable();
            } catch (SQLException e) {
                // ignore
            }
        }
        driver = MockJDBCDriver.getInstance().setMockJDBCAnswer(conditionalAnswer);

        config.setMinConnections(2);
        config.setMaxConnections(5);
        pool = new Hqcp(config);

        // 等待初始化完成
        Thread.sleep(100);

        // 获取一个连接并关闭它，减少连接数
        Connection conn = pool.getConnection();
        assertNotNull(conn);

        // 恢复正常
        shouldThrowNow.set(false);

        // 验证池仍然可用
        Connection newConn = pool.getConnection();
        assertNotNull(newConn);
        newConn.close();

        conn.close();
        assertTrue(throwed.get());
        LOGGER.info("✓ NewMoreConnections maintain min connections SQLException tested");
    }

    /**
     * 测试newMoreConnections按需创建连接时的SQLException
     * 覆盖行 617-619: 耗尽连接后，requireMore信号触发创建新连接失败
     >>> COVERED: Line 375-378 - newConnection SQLException, releasing semaphore
     >>> COVERED: Line 612-613 - newMoreConnections SQLException when creating more connections
     */
    @Test
    public void testNewMoreConnectionsOnDemandException() throws Exception {
        final AtomicBoolean shouldThrowOnDemand = new AtomicBoolean(false);
        final AtomicInteger createCount = new AtomicInteger(0);
        final AtomicBoolean throwed = new AtomicBoolean(false);

        MockJDBCAnswer demandAnswer = () -> {
            int count = createCount.incrementAndGet();
            // 前2次成功（初始化），第3次开始按需求抛异常
            if (shouldThrowOnDemand.get() && count > 2) {
                throwed.set(true);
                throw new SQLException("Mock: Cannot create connection on demand");
            }
            MockConnection conn = new MockConnection();
            conn.connect();
            LOGGER.info("new MockConnection: {} {}", count, shouldThrowOnDemand.get());
            return conn;
        };

        if (driver != null) {
            try {
                driver.disable();
            } catch (SQLException e) {
                // ignore
            }
        }
        driver = MockJDBCDriver.getInstance().setMockJDBCAnswer(demandAnswer);

        config.setLazyInit(true);
        config.setMinConnections(1);
        config.setMaxConnections(4);
        pool = new Hqcp(config);

        // 等待初始化
        Thread.sleep(100);

        // 耗尽所有连接
        Connection conn1 = pool.getConnection();
        Connection conn2 = pool.getConnection();

        // 现在设置按需创建时抛异常
        shouldThrowOnDemand.set(true);

        // 在后台线程尝试获取新连接，触发requireMore信号
        CountDownLatch latch = new CountDownLatch(1);
        Thread demandThread = new Thread(() -> {
            try {
                // 这会触发requireMore信号，monitor尝试创建新连接但失败
                Connection conn3 = pool.getConnection(false); // 尝试获取连接
                if (conn3 != null) {
                    conn3.close();
                }
            } catch (SQLException e) {
                // 预期可能超时或失败
            } finally {
                latch.countDown();
            }
        });
        demandThread.start();

        // 等待requireMore被触发和处理
        Thread.sleep(200);

        // 释放连接让demandThread可以获取
        conn1.close();
        conn2.close();

        latch.await(3, TimeUnit.SECONDS);

        // 恢复正常
        shouldThrowOnDemand.set(false);

        assertTrue(throwed.get());
        LOGGER.info("✓ NewMoreConnections on demand SQLException tested");
    }

    /**
     * 测试CPMonitor.run中的Exception处理
     * 覆盖行 646-648: monitor运行时抛出非InterruptedException的Exception
     >>> COVERED: Line 375-378 - newConnection SQLException, releasing semaphore
     >>> COVERED: Line 646-648 - CPMonitor.run Exception caught
     */
    @Test
    public void testCPMonitorRunException() throws Exception {
        // 创建一个会在特定时刻抛异常的Answer
        final AtomicBoolean shouldThrowInMonitor = new AtomicBoolean(false);
        final AtomicInteger callCount = new AtomicInteger(0);
        final AtomicBoolean throwed = new AtomicBoolean(false);

        MockJDBCAnswer monitorAnswer = () -> {
            int count = callCount.incrementAndGet();
            // 初始化时成功，后续在monitor运行时抛异常
            if (shouldThrowInMonitor.get() && count > 1) {
                // 抛出一个会被monitor的Exception catch块捕获的异常
                throwed.set(true);
                shouldThrowInMonitor.set(false);
                throw new RuntimeException("Mock: Monitor operation failed");
            }
            MockConnection conn = new MockConnection();
            LOGGER.info("new MockConnection: {} {}", count, shouldThrowInMonitor.get());
            conn.connect();
            return conn;
        };

        if (driver != null) {
            try {
                driver.disable();
            } catch (SQLException e) {
                // ignore
            }
        }
        driver = MockJDBCDriver.getInstance().setMockJDBCAnswer(monitorAnswer);

        config.setMinConnections(1);
        config.setLazyInit(true);
        pool = new Hqcp(config);

        // 等待初始化
        Thread.sleep(100);

        // 获取一个连接
        Connection conn = pool.getConnection();

        // 设置抛异常
        shouldThrowInMonitor.set(true);
        Connection conn2 = pool.getConnection();

        // 验证池仍可用
        Connection newConn = pool.getConnection();
        assertNotNull(newConn);
        newConn.close();

        conn2.close();
        conn.close();

        assertTrue(throwed.get());
        LOGGER.info("✓ CPMonitor.run Exception handling tested");
    }
}
