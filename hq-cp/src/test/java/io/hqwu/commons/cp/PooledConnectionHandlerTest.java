package io.hqwu.commons.cp;

import com.jolbox.bonecp.MockConnection;
import com.jolbox.bonecp.MockConstant;
import com.jolbox.bonecp.MockJDBCAnswer;
import com.jolbox.bonecp.MockJDBCDriver;
import org.easymock.IMocksControl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

import static org.easymock.EasyMock.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 针对 PooledConnection 连接归还后未屏蔽操作问题的单元测试。
 * <p>
 * 测试场景：
 * <ul>
 *   <li>线程 A 借出连接后归还，保留了引用</li>
 *   <li>连接被线程 B 借出后</li>
 *   <li>线程 A 尝试使用旧引用应该抛出异常</li>
 * </ul>
 *
 * @author wuhongqiang.taige
 * @since 1.17.0
 */
class PooledConnectionProxyTest {

    private Hqcp pool;
    private MockJDBCDriver driver;
    private IMocksControl mocksControl;
    private MockConnection mockConnection;
    private HqcpConfig config;

    @BeforeEach
    void setup() throws SQLException {
        mocksControl = createNiceControl();
        mockConnection = mocksControl.createMock(MockConnection.class);
        config = mocksControl.createMock(HqcpConfig.class);

        // 配置 Mock 期望
        expect(config.getUrl()).andReturn(MockConstant.MOCK_URL).anyTimes();
        expect(config.getDriverClassName()).andReturn(null).anyTimes();
        expect(config.getIdleTimeoutMillisec()).andReturn(10000L).anyTimes();
        expect(config.getCheckoutTimeoutMillisec()).andReturn(5000L).anyTimes();
        expect(config.getCheckStatement()).andReturn("SELECT 1").anyTimes();
        expect(config.getJmxLevel()).andReturn(0).anyTimes();
        expect(config.getMaxConnections()).andReturn(5).anyTimes();
        expect(config.getMinConnections()).andReturn(1).anyTimes();
        expect(config.getMaxPreStatements()).andReturn(10).anyTimes();
        expect(config.getMaxStatements()).andReturn(10).anyTimes();
        expect(config.getUsername()).andReturn("test").anyTimes();
        expect(config.isVerbose()).andReturn(false).anyTimes();
        expect(config.isCommitOnClose()).andReturn(false).anyTimes();
        expect(config.isPrintSql()).andReturn(false).anyTimes();
        expect(config.isTransactionMode()).andReturn(false).anyTimes();
        expect(config.getQueryTimeout()).andReturn(0).anyTimes();
        expect(config.isOracle()).andReturn(false).anyTimes();

        Properties properties = new Properties();
        properties.setProperty("user", "test");
        properties.setProperty("password", "test");
        expect(config.getConnectionProperties()).andReturn(properties).anyTimes();

        // 预先创建所有需要的 Mock Statement 对象
        Statement mockStmt1 = mocksControl.createMock(Statement.class);
        expect(mockStmt1.getResultSetType()).andReturn(java.sql.ResultSet.TYPE_FORWARD_ONLY).anyTimes();
        expect(mockStmt1.getResultSetConcurrency()).andReturn(java.sql.ResultSet.CONCUR_READ_ONLY).anyTimes();
        mockStmt1.close();
        expectLastCall().anyTimes();

        PreparedStatement mockPstmt1 = mocksControl.createMock(PreparedStatement.class);
        expect(mockPstmt1.getResultSetType()).andReturn(java.sql.ResultSet.TYPE_FORWARD_ONLY).anyTimes();
        expect(mockPstmt1.getResultSetConcurrency()).andReturn(java.sql.ResultSet.CONCUR_READ_ONLY).anyTimes();
        mockPstmt1.close();
        expectLastCall().anyTimes();

        // Mock Connection 行为
        expect(mockConnection.getAutoCommit()).andReturn(true).anyTimes();
        expect(mockConnection.createStatement()).andReturn(mockStmt1).anyTimes();
        expect(mockConnection.prepareStatement(anyString())).andReturn(mockPstmt1).anyTimes();

        mockConnection.setAutoCommit(anyBoolean());
        expectLastCall().anyTimes();
        mockConnection.commit();
        expectLastCall().anyTimes();
        mockConnection.rollback();
        expectLastCall().anyTimes();
        mockConnection.close();
        expectLastCall().anyTimes();

        makeThreadSafe(config, true);
        makeThreadSafe(mockConnection, true);

        MockJDBCAnswer answer = mocksControl.createMock(MockJDBCAnswer.class);
        expect(answer.answer()).andReturn(mockConnection).anyTimes();

        mocksControl.replay();

        driver = MockJDBCDriver.getInstance().setMockJDBCAnswer(answer);
        pool = new Hqcp(config);
    }

    @AfterEach
    void teardown() throws SQLException {
        if (pool != null && !pool.isShutdown()) {
            pool.shutdown();
        }
        if (driver != null) {
            driver.disable();
        }
        mocksControl.verify();
    }

    /**
     * 测试连接归还后，旧代理引用应该失效。
     * <p>
     * 核心逻辑：
     * <ol>
     *   <li>线程 A 借出连接</li>
     *   <li>线程 A 归还连接（调用 close）</li>
     *   <li>线程 A 尝试使用旧引用，应该抛出 SQLException</li>
     * </ol>
     */
    @Test
    void testOldProxyThrowsExceptionAfterReturn() throws SQLException {
        // 1. 借出连接
        Connection conn = pool.getConnection();
        assertNotNull(conn);

        // 2. 验证连接可用
        Statement stmt = conn.createStatement();
        assertNotNull(stmt);
        stmt.close();

        // 3. 归还连接
        conn.close();

        // 4. 尝试使用旧引用 - 应该抛出 SQLException，错误码 08003
        SQLException exception = assertThrows(SQLException.class, () -> {
            conn.createStatement();
        });

        assertEquals("08003", exception.getSQLState());
        assertTrue(exception.getMessage().contains("has been closed"));
    }

    /**
     * 测试连接归还后，各种操作都应该抛出异常。
     * <p>
     * 但是 close() 和 isClosed() 是允许的（符合 JDBC 规范）。
     */
    @Test
    void testAllOperationsThrowExceptionAfterReturn() throws SQLException {
        Connection conn = pool.getConnection();
        conn.close();

        // close() 和 isClosed() 应该允许调用（幂等性）
        conn.close();  // 多次关闭不抛异常
        assertTrue(conn.isClosed());  // 应该返回 true

        // 以下操作应该抛出异常
        // createStatement
        assertThrows(SQLException.class, conn::createStatement);

        // prepareStatement
        assertThrows(SQLException.class, () -> conn.prepareStatement("SELECT 1"));

        // prepareCall
        assertThrows(SQLException.class, () -> conn.prepareCall("{call test()}"));

        // setAutoCommit
        assertThrows(SQLException.class, () -> conn.setAutoCommit(false));

        // commit
        assertThrows(SQLException.class, conn::commit);

        // rollback
        assertThrows(SQLException.class, conn::rollback);
    }

    /**
     * 测试并发场景：线程 A 归还后，线程 B 借出，两者互不干扰。
     * <p>
     * 场景：
     * <ol>
     *   <li>线程 A 借出连接 conn1</li>
     *   <li>线程 A 归还 conn1</li>
     *   <li>线程 B 借出连接 conn2（可能是同一个物理连接，但代理不同）</li>
     *   <li>线程 A 使用 conn1 应该失败</li>
     *   <li>线程 B 使用 conn2 应该成功</li>
     * </ol>
     */
    @Test
    void testConcurrentCheckoutAfterReturn() throws Exception {
        // 第一次借出
        Connection conn1 = pool.getConnection();
        Statement stmt1 = conn1.createStatement();
        stmt1.close();
        conn1.close();

        // 第二次借出（可能复用同一个物理连接）
        Connection conn2 = pool.getConnection();
        Statement stmt2 = conn2.createStatement();
        stmt2.close();

        // 验证：代理不同
        assertNotSame(conn1, conn2);

        // 验证：旧代理失效
        assertThrows(SQLException.class, () -> conn1.createStatement());

        // 验证：新代理可用
        Statement stmt3 = conn2.createStatement();
        assertNotNull(stmt3);
        stmt3.close();

        conn2.close();
    }

    /**
     * 测试每次 checkOut 都会创建新的代理对象。
     */
    @Test
    void testNewProxyCreatedOnEachCheckout() throws SQLException {
        // 第一次借出
        Connection conn1 = pool.getConnection();
        conn1.close();

        // 第二次借出
        Connection conn2 = pool.getConnection();
        conn2.close();

        // 第三次借出
        Connection conn3 = pool.getConnection();
        conn3.close();

        // 验证三个代理对象都不相同
        assertNotSame(conn1, conn2);
        assertNotSame(conn2, conn3);
        assertNotSame(conn1, conn3);
    }

    /**
     * 测试 ConnectionProxy.toString() 返回有意义的信息。
     */
    @Test
    void testProxyToString() throws SQLException {
        Connection conn = pool.getConnection();

        String toString = conn.toString();
        assertNotNull(toString);
        // 格式应该是 "poolName#connectionId{realConnection}"
        assertTrue(toString.contains("HQCP#"), "Should contain pool name");
        assertTrue(toString.contains("{"), "Should contain brace");

        conn.close();
    }

    /**
     * 测试事务模式下的连接归还。
     */
    @Test
    void testTransactionModeConnectionReturn() throws SQLException {
        Connection conn = pool.getConnection();
        conn.setAutoCommit(false);

        // 执行一些操作
        PreparedStatement pstmt = conn.prepareStatement("SELECT 1");
        pstmt.close();

        // 归还
        conn.close();

        // 验证 isClosed() 返回 true
        assertTrue(conn.isClosed());

        // 旧引用失效
        assertThrows(SQLException.class, () -> conn.prepareStatement("SELECT 2"));
    }

    /**
     * 测试在归还的连接上调用 unwrap 应该抛异常。
     */
    @Test
    void testUnwrapAfterReturn() throws SQLException {
        Connection conn = pool.getConnection();
        conn.close();

        assertThrows(SQLException.class, () -> conn.unwrap(Connection.class));
    }

    /**
     * 测试在归还的连接上调用 isWrapperFor 应该抛异常。
     */
    @Test
    void testIsWrapperForAfterReturn() throws SQLException {
        Connection conn = pool.getConnection();
        conn.close();

        assertThrows(SQLException.class, () -> conn.isWrapperFor(Connection.class));
    }

    /**
     * 测试 isClosed() 方法的正确行为。
     * <p>
     * 符合 JDBC 规范：
     * <ul>
     *   <li>借出时返回 false</li>
     *   <li>归还后返回 true</li>
     *   <li>可以多次调用</li>
     * </ul>
     */
    @Test
    void testIsClosedBehavior() throws SQLException {
        Connection conn = pool.getConnection();

        // 借出时应该返回 false
        assertFalse(conn.isClosed(), "Connection should be open after checkout");

        // 归还
        conn.close();

        // 归还后应该返回 true
        assertTrue(conn.isClosed(), "Connection should be closed after return");

        // 可以多次调用 isClosed()
        assertTrue(conn.isClosed());
        assertTrue(conn.isClosed());
    }

    /**
     * 测试多次归还同一个连接（幂等性）。
     * <p>
     * 符合 JDBC 规范：对已关闭的连接多次调用 close() 应该是安全的。
     */
    @Test
    void testMultipleCloseIdempotent() throws SQLException {
        Connection conn = pool.getConnection();

        // 第一次关闭
        conn.close();

        // 第二次关闭 - 应该不抛异常（幂等性）
        assertDoesNotThrow(conn::close);

        // 第三次关闭 - 仍然不抛异常
        assertDoesNotThrow(conn::close);

        // 验证 isClosed() 返回 true
        assertTrue(conn.isClosed());
    }
}
