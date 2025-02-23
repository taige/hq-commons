package io.hqwu.commons.cp;

import com.jolbox.bonecp.*;
import com.umpay.commons.util.Logger;
import org.easymock.classextension.IMocksControl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.*;
import java.util.Properties;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;

import static org.easymock.EasyMock.*;
import static org.easymock.classextension.EasyMock.createNiceControl;
import static org.easymock.classextension.EasyMock.createNiceMock;
import static org.easymock.classextension.EasyMock.makeThreadSafe;
import static org.easymock.classextension.EasyMock.replay;
import static org.easymock.classextension.EasyMock.verify;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Created with IntelliJ IDEA for plat-arch-svn
 * User: taige
 * Date: 13-10-31
 * Time: 上午10:18
 */
public class HqcpTest {
    private static final Logger LOGGER = new Logger();

    private MockJDBCDriver driver;
    private MockJDBCAnswer answer;

    private MockConnection mockConnection;
    private MockJDBCStatement mockStatement;
    private MockPreparedStatement mockPreparedStatement;
    private MockCallableStatement mockCallableStatement;

    private HqcpConfig config;
    private Hqcp connPool;

    private IMocksControl mocksControl;
    @BeforeEach
    public void setUp() throws SQLException {
        mocksControl = createNiceControl();
        mockConnection = mocksControl.createMock(MockConnection.class);
        mockConnection.close();
        expectLastCall().once();
        config = mocksControl.createMock(HqcpConfig.class);
        expect(config.getUrl()).andReturn(MockConstant.MOCK_URL).atLeastOnce();
        expect(config.getDriverClassName()).andReturn(null).atLeastOnce();
        expect(config.getIdleTimeoutMillisec()).andReturn(10000L).atLeastOnce(); //回收时间10sec
        expect(config.getCheckoutTimeoutMillisec()).andReturn(5000L).anyTimes(); //获取连接的超时时间5sec
        expect(config.getCheckStatement()).andReturn("test").anyTimes();
        expect(config.getJmxLevel()).andReturn(2).atLeastOnce();
        expect(config.getMaxConnections()).andReturn(5).anyTimes(); //最大连接 5
        expect(config.getMinConnections()).andReturn(1).atLeastOnce(); //最小 1
        expect(config.getMaxPreStatements()).andReturn(5).anyTimes();
        expect(config.getMaxStatements()).andReturn(10).anyTimes();
        expect(config.getUsername()).andReturn("mockuser").anyTimes();
//        expect(config.getPassword()).andReturn("mockpassword").anyTimes();
        expect(config.isVerbose()).andReturn(false).once().andReturn(true).anyTimes();
        expect(config.isCommitOnClose()).andReturn(false).once().andReturn(true).anyTimes();
        expect(config.isPrintSql()).andReturn(false).once().andReturn(true).anyTimes();
        expect(config.isTransactionMode()).andReturn(false).once().andReturn(true).anyTimes();
        expect(config.getInfoSqlThreshold()).andReturn(10L).anyTimes();
        expect(config.getWarnSqlThreshold()).andReturn(100L).anyTimes();
        expect(config.isOracle()).andReturn(false).atLeastOnce();
        expect(config.isUseOracleImplicitCache()).andReturn(true).anyTimes();
        expect(config.getQueryTimeout()).andReturn(0).atLeastOnce();
        Properties properties = new Properties();
        properties.setProperty("user", "mockuser");
        properties.setProperty("password", "mockpassword");
        expect(config.getConnectionProperties()).andReturn(properties).atLeastOnce();

        //create mock mockStatement & resultset
        MockResultSet rs = mocksControl.createMock(MockResultSet.class);
        expect(rs.next()).andReturn(true).anyTimes();
        mockStatement = mocksControl.createMock(MockJDBCStatement.class);
        //idle check mockStatement should be call once
        expect(mockStatement.execute((String) anyObject())).andReturn(true).anyTimes();
        expect(mockStatement.executeQuery((String) anyObject())).andReturn(rs).anyTimes();
        expect(mockStatement.executeUpdate((String) anyObject())).andReturn(1).anyTimes();
        int[] n = {1};
        expect(mockStatement.executeBatch()).andReturn(n).anyTimes();
        expect(mockStatement.getResultSetType()).andReturn(ResultSet.TYPE_FORWARD_ONLY).anyTimes();
        expect(mockStatement.getResultSetConcurrency()).andReturn(ResultSet.CONCUR_READ_ONLY).anyTimes();

//        mockPreparedStatement = mocksControl.createMock(MockPreparedStatement.class);
//        expect(mockPreparedStatement.execute()).andReturn(true).anyTimes();
//        expect(mockPreparedStatement.executeQuery()).andReturn(rs).anyTimes();
//        expect(mockPreparedStatement.executeUpdate()).andReturn(1).anyTimes();
//        expect(mockPreparedStatement.getResultSetType()).andReturn(ResultSet.TYPE_FORWARD_ONLY).anyTimes();
//        expect(mockPreparedStatement.getResultSetConcurrency()).andReturn(ResultSet.CONCUR_READ_ONLY).anyTimes();
//        expect(mockPreparedStatement.executeBatch()).andReturn(n).anyTimes();

        mockCallableStatement = mocksControl.createMock(MockCallableStatement.class);
        expect(mockCallableStatement.execute()).andReturn(true).anyTimes();
        expect(mockCallableStatement.executeQuery()).andReturn(rs).anyTimes();
        expect(mockCallableStatement.executeUpdate()).andReturn(1).anyTimes();
        expect(mockCallableStatement.getResultSetType()).andReturn(ResultSet.TYPE_FORWARD_ONLY).anyTimes();
        expect(mockCallableStatement.getResultSetConcurrency()).andReturn(ResultSet.CONCUR_READ_ONLY).anyTimes();

        expect(mockConnection.createStatement()).andReturn(mockStatement).anyTimes();
//        expect(mockConnection.prepareStatement((String) anyObject())).andReturn(mockPreparedStatement).anyTimes();
        expect(mockConnection.prepareCall((String) anyObject())).andReturn(mockCallableStatement).anyTimes();
        expect(mockConnection.prepareCall((String) anyObject(), anyInt(), anyInt())).andReturn(mockCallableStatement).anyTimes();
        expect(mockConnection.prepareCall((String) anyObject(), anyInt(), anyInt(), anyInt())).andReturn(mockCallableStatement).anyTimes();

        makeThreadSafe(config, true);
        makeThreadSafe(mockConnection, true);
    }

    @AfterEach
    public void tearDown() throws SQLException {
        if (connPool != null) {
            if (! connPool.isShutdown()) {
                Connection conn = connPool.getConnection();
                Statement stmt = conn.createStatement();
                stmt.executeUpdate("ssss");
                conn.close();
                connPool.shutdown();
            }
            mocksControl.verify();
            connPool = null;
        }
        if (driver != null) {
            driver.disable();
            driver = null;
        }
    }

    @Test
    public void testShutdown() throws Exception {
        answer = mocksControl.createMock(MockJDBCAnswer.class);
        expect(answer.answer()).andReturn(mockConnection).once();
        mocksControl.replay();
        driver = MockJDBCDriver.getInstance().setMockJDBCAnswer(answer);

        connPool = new Hqcp(config);
        Connection conn = connPool.getConnection();
        assertEquals(conn, mockConnection);
        Statement stmt = conn.createStatement();
        int n = stmt.executeUpdate("ssss");
        assertEquals(1, n);
        stmt.close();
        conn.close();
        assertEquals(1, connPool.getActiveConnectionsCount());

        //shutdown() should call real connection's close()
        connPool.shutdown();
        assertEquals(0, connPool.getActiveConnectionsCount());
        try {
            connPool.getConnection();
            fail("should not happend");
        } catch (SQLException e) {
        }

    }

    @Test
    public void testShutdownForce() throws Exception {
        answer = mocksControl.createMock(MockJDBCAnswer.class);
        expect(answer.answer()).andReturn(mockConnection).once();
        mocksControl.replay();
        driver = MockJDBCDriver.getInstance().setMockJDBCAnswer(answer);

        connPool = new Hqcp(config);
        Connection conn = connPool.getConnection();
        assertEquals(conn, mockConnection);
        //conn.close();
        Statement stmt = conn.createStatement();
        int n = stmt.executeUpdate("ssss");
        assertEquals(1, n);
        stmt.close();
        assertEquals(1, connPool.getActiveConnectionsCount());

        //shutdown() should call real connection's close()
        connPool.shutdown();
        assertEquals(0, connPool.getActiveConnectionsCount());
        try {
            connPool.getConnection();
            fail("should not happend");
        } catch (SQLException e) {
        }

    }

    @Test
    public void testGetConnectionTimeout_lazyInit() throws Exception {
        /*
         * lasyInit true的时候，getConnection才会抛出Timeout的SQLException
         */
        expect(config.isLazyInit()).andReturn(true).anyTimes();
        final CyclicBarrier cyclicBarrier = new CyclicBarrier(2);
        answer = new MockJDBCAnswer() {
            @Override
            public Connection answer() throws SQLException {
                LOGGER.info("wait util getConnection timeout...");
                try {
                    cyclicBarrier.await(6, TimeUnit.SECONDS); //timeout is 5000 ms
                //} catch (TimeoutException e) {
                } catch (Exception e) {
                    e.printStackTrace();
                    fail("test getConnection timeout fail..." + e.getMessage());
                }
                return mockConnection;
            }
        };
        mocksControl.replay();
        driver = MockJDBCDriver.getInstance().setMockJDBCAnswer(answer);

        connPool = new Hqcp(config);
        try {
            Connection conn = connPool.getConnection();
            conn.close();
            fail("test getConnection timeout fail...");
        } catch (SQLException e) {
        }
        cyclicBarrier.await();
    }

    @Test
    public void testGetConnectionExhaustedTimeout() throws Exception {
        MockConnection mockConnection2 = new MockConnection();
        //mockConnection2.close();
        //expectLastCall().times(4);

        answer = mocksControl.createMock(MockJDBCAnswer.class);
        expect(answer.answer()).andReturn(mockConnection).andReturn(mockConnection2).times(4);
        mocksControl.replay();
        driver = MockJDBCDriver.getInstance().setMockJDBCAnswer(answer);

        connPool = new Hqcp(config);
        Connection[] conns = new Connection[5]; //max connections is 5
        for (int i = 0; i < conns.length; i++) {
            conns[i] = connPool.getConnection();
        }
        try {
            Connection conn = connPool.getConnection();
            fail("getConnection exhausted timeout fail...");
        } catch (SQLException e) {
        }
        for (Connection conn : conns) {
            conn.close();
        }
    }

    @Test
    public void testStatementUnnormal() throws Exception {
        MockJDBCStatement mockJDBCStatement = mocksControl.createMock(MockJDBCStatement.class);
        expect(mockJDBCStatement.execute((String) anyObject())).andReturn(true).anyTimes();
        expect(mockJDBCStatement.executeUpdate((String) anyObject())).andReturn(1).anyTimes();
        expect(mockJDBCStatement.getResultSetType()).andReturn(ResultSet.TYPE_SCROLL_SENSITIVE).anyTimes();
        expect(mockJDBCStatement.getResultSetConcurrency()).andReturn(ResultSet.CONCUR_UPDATABLE).anyTimes();
        MockResultSet rs = mocksControl.createMock(MockResultSet.class);
        expect(mockJDBCStatement.getResultSet()).andReturn(rs).anyTimes();

        expect(mockConnection.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE)).andReturn(mockJDBCStatement).anyTimes();

        answer = mocksControl.createMock(MockJDBCAnswer.class);
        expect(answer.answer()).andReturn(mockConnection).atLeastOnce();
        mocksControl.replay();
        driver = MockJDBCDriver.getInstance().setMockJDBCAnswer(answer);

        connPool = new Hqcp(config);
        config.isPrintSql();
        Connection conn = connPool.getConnection();
        try {
            Statement stmt = conn.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
            stmt.execute("stmt ....");
            stmt.close();
        } catch (SQLException e) {
            fail("testStatement fail...");
        }
        conn.commit();
        conn.close();
    }

    @Test
    public void testStatementUnnormal_false() throws Exception {
        MockJDBCStatement mockJDBCStatement = mocksControl.createMock(MockJDBCStatement.class);
        expect(mockJDBCStatement.execute((String) anyObject())).andReturn(false).anyTimes();
        expect(mockJDBCStatement.executeUpdate((String) anyObject())).andReturn(1).anyTimes();
        expect(mockJDBCStatement.executeLargeUpdate((String) anyObject())).andReturn(100L).anyTimes();
        expect(mockJDBCStatement.getResultSetType()).andReturn(ResultSet.TYPE_SCROLL_SENSITIVE).anyTimes();
        expect(mockJDBCStatement.getResultSetConcurrency()).andReturn(ResultSet.CONCUR_UPDATABLE).anyTimes();
        MockResultSet rs = mocksControl.createMock(MockResultSet.class);
        expect(mockJDBCStatement.getResultSet()).andReturn(rs).anyTimes();
        expect(mockJDBCStatement.getUpdateCount()).andReturn(10).anyTimes();

        expect(mockConnection.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE)).andReturn(mockJDBCStatement).anyTimes();

        answer = mocksControl.createMock(MockJDBCAnswer.class);
        expect(answer.answer()).andReturn(mockConnection).atLeastOnce();
        mocksControl.replay();
        driver = MockJDBCDriver.getInstance().setMockJDBCAnswer(answer);

        connPool = new Hqcp(config);
        config.isPrintSql();
        Connection conn = connPool.getConnection();
        try {
            Statement stmt = conn.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
            stmt.execute("stmt ....");
            stmt.executeLargeUpdate("stmt ....");
            stmt.close();
        } catch (SQLException e) {
            fail("testStatement fail...");
        }
        conn.commit();
        conn.close();
    }

    @Test
    public void testStatement() throws Exception {
        answer = mocksControl.createMock(MockJDBCAnswer.class);
        expect(answer.answer()).andReturn(mockConnection).atLeastOnce();
        mocksControl.replay();
        driver = MockJDBCDriver.getInstance().setMockJDBCAnswer(answer);

        connPool = new Hqcp(config);
        Connection conn = connPool.getConnection();
        Statement stmts[] = new Statement[10];
        for (int i = 0; i < stmts.length; i++) {
            stmts[i] = conn.createStatement();
            stmts[i].executeQuery("stmt"+i);
        }
        try {
            conn.createStatement();
            fail("testStatement fail...");
        } catch (SQLException e) {}

        for (Statement stmt : stmts) {
            stmt.close();
        }
        conn.commit();
        for (int i = 0; i < stmts.length; i++) {
            stmts[i] = conn.createStatement();
            stmts[i].executeUpdate("stmt"+i);
            stmts[i].close();
        }
        conn.rollback();
        try {
            Statement stmt = conn.createStatement();
            stmt.execute("stmt ....");
        } catch (SQLException e) {
            fail("testStatement fail...");
        }
        try {
            Statement stmt = conn.createStatement();
            stmt.addBatch("stmt ....");
            stmt.executeBatch();
        } catch (SQLException e) {
            fail("testStatement fail...");
        }

        //conn.close();
    }

    @Test
    public void testPreparedStatement_fail() throws Exception {
        mockPreparedStatement = mocksControl.createMock(MockPreparedStatement.class);
        expect(mockPreparedStatement.execute()).andThrow(new SQLException("aaa")).anyTimes(); //.andReturn(true).anyTimes();
        expect(mockPreparedStatement.executeQuery()).andThrow(new SQLException("aaa")).anyTimes(); //.andReturn(rs).anyTimes();
        expect(mockPreparedStatement.executeUpdate()).andThrow(new SQLException("aaa")).anyTimes(); //.andReturn(1).anyTimes();
        expect(mockPreparedStatement.getResultSetType()).andReturn(ResultSet.TYPE_FORWARD_ONLY).anyTimes();
        expect(mockPreparedStatement.getResultSetConcurrency()).andReturn(ResultSet.CONCUR_READ_ONLY).anyTimes();
        expect(mockPreparedStatement.executeBatch()).andReturn(new int[]{1}).anyTimes();

        expect(mockConnection.prepareStatement((String) anyObject())).andReturn(mockPreparedStatement).anyTimes();

        answer = mocksControl.createMock(MockJDBCAnswer.class);
        expect(answer.answer()).andReturn(mockConnection).atLeastOnce();
        mocksControl.replay();
        driver = MockJDBCDriver.getInstance().setMockJDBCAnswer(answer);

        connPool = new Hqcp(config);
        connPool.getConnection().close();
        Connection conn = connPool.getConnection(false);
        try {
            PreparedStatement pstmt = conn.prepareStatement("prestmt #101 where a=? and bb=? and cc=? and d=? and ee=? and e=1");
            pstmt.setString(1, "aaa");
            pstmt.setInt(2, 123);
            pstmt.setDate(3, new Date(System.currentTimeMillis()));
            pstmt.setTimestamp(4, new Timestamp(System.currentTimeMillis()));
            pstmt.setTime(5, new Time(System.currentTimeMillis()));
            pstmt.execute();
            pstmt.close();
            fail("testPreparedStatement_fail fail...");
        } catch (SQLException e) {
        }
        try {
            PreparedStatement pstmt = conn.prepareStatement("prestmt #101 where a=? and bb=? and cc=? and d=? and ee=? and e=1");
            pstmt.setString(1, "aaa");
            pstmt.setInt(2, 123);
            pstmt.setDate(3, new Date(System.currentTimeMillis()));
            pstmt.setTimestamp(4, new Timestamp(System.currentTimeMillis()));
            pstmt.setTime(5, new Time(System.currentTimeMillis()));
            pstmt.executeUpdate();
            pstmt.close();
            fail("testPreparedStatement_fail fail...");
        } catch (SQLException e) {
        }
        try {
            PreparedStatement pstmt = conn.prepareStatement("prestmt #101 where a=? and bb=? and cc=? and d=? and ee=? and e=1");
            pstmt.setString(1, "aaa");
            pstmt.setInt(2, 123);
            pstmt.setDate(3, new Date(System.currentTimeMillis()));
            pstmt.setTimestamp(4, new Timestamp(System.currentTimeMillis()));
            pstmt.setTime(5, new Time(System.currentTimeMillis()));
            pstmt.executeQuery();
            pstmt.close();
            fail("testPreparedStatement_fail fail...");
        } catch (SQLException e) {
        }
        conn.close();
    }

    @Test
    public void testPreparedStatement_2() throws Exception {
        mockPreparedStatement = mocksControl.createMock(MockPreparedStatement.class);
        expect(mockPreparedStatement.execute()).andReturn(true).once().andReturn(false).anyTimes();
//        expect(mockPreparedStatement.executeQuery()).andReturn(rs).anyTimes();
        expect(mockPreparedStatement.executeUpdate()).andReturn(1).anyTimes();
        expect(mockPreparedStatement.getResultSetType()).andReturn(ResultSet.TYPE_FORWARD_ONLY).anyTimes();
        expect(mockPreparedStatement.getResultSetConcurrency()).andReturn(ResultSet.CONCUR_READ_ONLY).anyTimes();
        expect(mockPreparedStatement.executeBatch()).andReturn(new int[]{1}).anyTimes();
        MockResultSet rs = mocksControl.createMock(MockResultSet.class);
        expect(mockPreparedStatement.getResultSet()).andReturn(rs).anyTimes();
        expect(mockPreparedStatement.getUpdateCount()).andReturn(10).anyTimes();

        expect(mockConnection.prepareStatement((String) anyObject())).andReturn(mockPreparedStatement).anyTimes();
        expect(mockConnection.prepareStatement((String) anyObject(), (int []) anyObject())).andReturn(mockPreparedStatement).anyTimes();
        expect(mockConnection.prepareStatement((String) anyObject(), (String []) anyObject())).andReturn(mockPreparedStatement).anyTimes();
        expect(mockConnection.prepareStatement((String) anyObject(), anyInt())).andReturn(mockPreparedStatement).anyTimes();
        expect(mockConnection.prepareStatement((String) anyObject(), anyInt(), anyInt())).andReturn(mockPreparedStatement).anyTimes();
        expect(mockConnection.prepareStatement((String) anyObject(), anyInt(), anyInt(), anyInt())).andReturn(mockPreparedStatement).anyTimes();

        answer = mocksControl.createMock(MockJDBCAnswer.class);
        expect(answer.answer()).andReturn(mockConnection).atLeastOnce();
        mocksControl.replay();
        driver = MockJDBCDriver.getInstance().setMockJDBCAnswer(answer);

        connPool = new Hqcp(config);
        config.isPrintSql();
        connPool.getConnection().close();
        Connection conn = connPool.getConnection(false);
        PreparedStatement pstmt = conn.prepareStatement("prestmt #100");
        pstmt.executeUpdate();
        pstmt.execute();
        pstmt.execute();
        pstmt.close();

        PreparedStatement pstmt1 = conn.prepareStatement("prestmt #200", new int[] {0, 1});
        pstmt1.executeUpdate();
        pstmt1.close();

        PreparedStatement pstmt2 = conn.prepareStatement("prestmt #300", new String[] {"a", "b"});
        pstmt2.executeUpdate();
        pstmt2.close();

        PreparedStatement pstmt3 = conn.prepareStatement("prestmt #400", ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
        pstmt3.executeUpdate();
        pstmt3.close();

        PreparedStatement pstmt4 = conn.prepareStatement("prestmt #500", ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE, ResultSet.HOLD_CURSORS_OVER_COMMIT);
        pstmt4.executeUpdate();
        pstmt4.close();

        PreparedStatement pstmt5 = conn.prepareStatement("prestmt #600", Statement.RETURN_GENERATED_KEYS);
        pstmt5.executeUpdate();
        pstmt5.close();
    }

    @Test
    public void testPreparedStatement() throws Exception {
        mockPreparedStatement = mocksControl.createMock(MockPreparedStatement.class);
        expect(mockPreparedStatement.execute()).andReturn(true).anyTimes();
//        expect(mockPreparedStatement.executeQuery()).andReturn(rs).anyTimes();
        expect(mockPreparedStatement.executeUpdate()).andReturn(1).anyTimes();
        expect(mockPreparedStatement.getResultSetType()).andReturn(ResultSet.TYPE_FORWARD_ONLY).anyTimes();
        expect(mockPreparedStatement.getResultSetConcurrency()).andReturn(ResultSet.CONCUR_READ_ONLY).anyTimes();
        expect(mockPreparedStatement.executeBatch()).andReturn(new int[]{1}).anyTimes();

        expect(mockConnection.prepareStatement((String) anyObject())).andReturn(mockPreparedStatement).anyTimes();

        answer = mocksControl.createMock(MockJDBCAnswer.class);
        expect(answer.answer()).andReturn(mockConnection).atLeastOnce();
        mocksControl.replay();
        driver = MockJDBCDriver.getInstance().setMockJDBCAnswer(answer);

        connPool = new Hqcp(config);
        connPool.getConnection().close();
        Connection conn = connPool.getConnection(false);
        PreparedStatement stmts[] = new PreparedStatement[10];
        for (int i = 0; i < 10; i++) {
            stmts[i] = conn.prepareStatement("prestmt #" + i);
            stmts[i].executeQuery();
        }
        conn.rollback();
        try {
            PreparedStatement pstmt = conn.prepareStatement("prestmt #100");
            pstmt.executeUpdate();
            pstmt.close();
        } catch (SQLException e) {
            fail("testPreparedStatement fail...");
        }
        for (PreparedStatement pstmt : stmts) {
            pstmt.close();
        }
        conn.commit();
        try {
            PreparedStatement pstmt = conn.prepareStatement("prestmt #101 where a=? and bb=? and cc=? and d=? and ee=? and e=1");
            pstmt.setString(1, "aaa");
            pstmt.setInt(2, 123);
            pstmt.setDate(3, new Date(System.currentTimeMillis()));
            pstmt.setTimestamp(4, new Timestamp(System.currentTimeMillis()));
            pstmt.setTime(5, new Time(System.currentTimeMillis()));
            pstmt.execute();
            pstmt.close();
        } catch (SQLException e) {
            fail("testPreparedStatement fail...");
        }
        try {
            PreparedStatement pstmt = conn.prepareStatement("prestmt #101 where a=? and c = ? And dd=?");
            pstmt.setString(1, "ABC");
            pstmt.setString(3, "AAA");
            pstmt.addBatch();
            pstmt.executeBatch();
            pstmt.close();
        } catch (SQLException e) {
            fail("testPreparedStatement fail...");
        }
        try {
            PreparedStatement pstmt = conn.prepareStatement("prestmt #101");
            pstmt.execute();
            PreparedStatement pstmt2 = conn.prepareStatement("prestmt #101");
            pstmt.close();
            pstmt2.close();
        } catch (SQLException e) {
            fail("testPreparedStatement fail...");
        }
        conn.close();
    }

    public static void main(String[] args) throws Exception {
        HqcpTest testCase = new HqcpTest();
        testCase.setUp();
        testCase.testPreparedStatement();
        testCase.tearDown();
    }

    @Test
    public void testCallableStatement_2() throws Exception {
        answer = mocksControl.createMock(MockJDBCAnswer.class);
        expect(answer.answer()).andReturn(mockConnection).atLeastOnce();
        mocksControl.replay();
        driver = MockJDBCDriver.getInstance().setMockJDBCAnswer(answer);

        connPool = new Hqcp(config);
        connPool.getConnection().close();
        Connection conn = connPool.getConnection(false);

        CallableStatement pstmt = conn.prepareCall("prestmt #100");
        pstmt.executeUpdate();
        pstmt.close();

        CallableStatement pstmt1 = conn.prepareCall("prestmt #200", ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
        pstmt1.executeUpdate();
        pstmt1.close();

        CallableStatement pstmt2 = conn.prepareCall("prestmt #300", ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE, ResultSet.HOLD_CURSORS_OVER_COMMIT);
        pstmt2.executeUpdate();
        pstmt2.close();
    }

    @Test
    public void testCallableStatement() throws Exception {
        answer = mocksControl.createMock(MockJDBCAnswer.class);
        expect(answer.answer()).andReturn(mockConnection).atLeastOnce();
        mocksControl.replay();
        driver = MockJDBCDriver.getInstance().setMockJDBCAnswer(answer);

        connPool = new Hqcp(config);
        connPool.getConnection().close();
        Connection conn = connPool.getConnection(false);
        CallableStatement stmts[] = new CallableStatement[10];
        for (int i = 0; i < 10; i++) {
            stmts[i] = conn.prepareCall("prestmt #" + i);
            stmts[i].executeQuery();
        }
        conn.rollback();
        try {
            CallableStatement pstmt = conn.prepareCall("prestmt #100");
            pstmt.executeUpdate();
            pstmt.close();
        } catch (SQLException e) {
            fail("testPreparedStatement fail...");
        }
        for (CallableStatement pstmt : stmts) {
            pstmt.close();
        }
        conn.commit();
        try {
            CallableStatement pstmt = conn.prepareCall("prestmt #101");
            pstmt.execute();
            pstmt.close();
        } catch (SQLException e) {
            fail("testPreparedStatement fail...");
        }
        conn.close();
    }

    @Test
    public void testGetConnectionExhaustedWaitAndOK() throws Exception {
        MockConnection mockConnection2 = mocksControl.createMock(MockConnection.class);
        mockConnection2.close();
        expectLastCall().times(4);

        answer = mocksControl.createMock(MockJDBCAnswer.class);
        expect(answer.answer()).andReturn(mockConnection).andReturn(mockConnection2).times(4);
        mocksControl.replay();
        driver = MockJDBCDriver.getInstance().setMockJDBCAnswer(answer);

        connPool = new Hqcp(config);
        final Connection conn = connPool.getConnection();
        Connection[] conns = new Connection[4]; //max connections is 5
        for (int i = 0; i < conns.length; i++) {
            conns[i] = connPool.getConnection();
        }
        final CyclicBarrier cyclicBarrier = new CyclicBarrier(2);

        //用于异步的将conn放回连接池的线程
        new Thread() {
            public void run() {
                try {
                    cyclicBarrier.await();
                    LOGGER.debug("sleep 3000ms");
                    Thread.sleep(3000); //timeout is 5 sec
                    conn.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.start();
        cyclicBarrier.await();
        Connection conn6 = connPool.getConnection();
        assertEquals(conn6, mockConnection);

        for (Connection _conn : conns) {
            _conn.close();
        }
        conn6.close();
    }

    @Test
    public void testConnectionIdleAndRealClose() throws Exception {
        MockConnection mockConnection2 = createNiceMock(MockConnection.class);
        //mockConnection2 should be real closed.
        mockConnection2.close();
        expectLastCall().once();
        expect(mockConnection2.createStatement()).andReturn(mockStatement).anyTimes();
        makeThreadSafe(mockConnection2, true);
        replay(mockConnection2);

        answer = mocksControl.createMock(MockJDBCAnswer.class);
        expect(answer.answer()).andReturn(mockConnection).once().andReturn(mockConnection2).once();
        mocksControl.replay();
        driver = MockJDBCDriver.getInstance().setMockJDBCAnswer(answer);
        connPool = new Hqcp(config);

        //create 2 connections
        Connection conn1 = connPool.getConnection();
        Connection conn2 = connPool.getConnection();
        conn2.close();

        LOGGER.info("conn2 back to pool. now wait conn1 back to pool... 1 sec");
        Thread.sleep(1000); //模拟conn1的工作
        conn1.close();

        LOGGER.info("conn1 back to pool. now wait 11 secs util conn2 to be real closed...");
        Thread.sleep(11000); //conn2放回连接池已经11+1秒，回收时间设置的10秒，conn2应该已经被回收
        assertEquals(1, connPool.getActiveConnectionsCount());
        assertEquals(1, connPool.getIdleConnectionsCount());

        //verify mockConnection2.close() is be called
        verify(mockConnection2);

    }

    @Test
    public void testGetConnection() throws Exception {
        answer = mocksControl.createMock(MockJDBCAnswer.class);
        expect(answer.answer()).andReturn(mockConnection).once();
        mocksControl.replay();
        driver = MockJDBCDriver.getInstance().setMockJDBCAnswer(answer);

        connPool = new Hqcp(config);
        Connection conn = connPool.getConnection();
        assertEquals(conn, mockConnection);
        conn.close();

        Connection conn1 = connPool.getConnection();
        assertEquals(conn1, mockConnection);
        conn1.close();

        connPool.reloadProperties();
    }

    @Test
    public void testGetConnectionAutoCommit() throws Exception {
        answer = mocksControl.createMock(MockJDBCAnswer.class);
        expect(answer.answer()).andReturn(mockConnection).once();
        mocksControl.replay();
        driver = MockJDBCDriver.getInstance().setMockJDBCAnswer(answer);

        connPool = new Hqcp(config);
        Connection conn = connPool.getConnection(true);
        assertEquals(conn, mockConnection);
        conn.close();

        Connection conn1 = connPool.getConnection();
        assertEquals(conn1, mockConnection);
        conn1.close();
    }

    @Test
    public void testGetConnectionLIFO() throws Exception {
        MockConnection mockConnection2 = new MockConnection();
        answer = mocksControl.createMock(MockJDBCAnswer.class);
        expect(answer.answer()).andReturn(mockConnection).once().andReturn(mockConnection2).once();
        mocksControl.replay();
        driver = MockJDBCDriver.getInstance().setMockJDBCAnswer(answer);
        connPool = new Hqcp(config);

        Connection conn1 = connPool.getConnection();
        assertEquals(conn1, mockConnection);
        Connection conn2 = connPool.getConnection();
        assertEquals(conn2, mockConnection2);

        conn1.close();
        conn2.close(); //mockConnection2 is LI

        Connection conn3 = connPool.getConnection();
        assertEquals(conn3, mockConnection2); //mockConnection2 is FO

        conn3.close();
    }

    @Test
    public void testGetPoolName() throws Exception {
        answer = mocksControl.createMock(MockJDBCAnswer.class);
        expect(answer.answer()).andReturn(mockConnection).times(2);
        mocksControl.replay();
        driver = MockJDBCDriver.getInstance().setMockJDBCAnswer(answer);

        connPool = new Hqcp(config);
        Connection conn = connPool.getConnection();
        Statement stmt = conn.createStatement();
        int n = stmt.executeUpdate("ssss");
        assertEquals(1, n);
        stmt.close();
        conn.close();
        Hqcp connPool2 = new Hqcp(config);
        connPool2.getConnection().close();
        assertFalse(connPool.getPoolName().equals(connPool2.getPoolName()));
        connPool2.shutdown();

        connPool.setPoolName("testPool");
        assertEquals("testPool", connPool.getPoolName());

        mocksControl.verify();
    }

    @Test
    public void testGetConnectionsCount() throws Exception {
        MockConnection mockConnection2 = new MockConnection();
        answer = mocksControl.createMock(MockJDBCAnswer.class);
        expect(answer.answer()).andReturn(mockConnection).once().andReturn(mockConnection2).once();
        mocksControl.replay();
        driver = MockJDBCDriver.getInstance().setMockJDBCAnswer(answer);

        connPool = new Hqcp(config);
        assertEquals(1, connPool.getActiveConnectionsCount());
        assertEquals(1, connPool.getIdleConnectionsCount());

        Connection conn1 = connPool.getConnection();
        assertEquals(conn1, mockConnection);
        assertEquals(1, connPool.getActiveConnectionsCount());
        assertEquals(0, connPool.getIdleConnectionsCount());

        Connection conn2 = connPool.getConnection();
        assertEquals(conn2, mockConnection2);
        assertEquals(2, connPool.getActiveConnectionsCount());
        assertEquals(0, connPool.getIdleConnectionsCount());

        conn1.close();
        assertEquals(1, connPool.getIdleConnectionsCount());
        conn2.close();
        assertEquals(2, connPool.getIdleConnectionsCount());
        assertEquals(2, connPool.getActiveConnectionsCount());
    }

    @Test
    public void testGetConfig() throws Exception {
        answer = mocksControl.createMock(MockJDBCAnswer.class);
        expect(answer.answer()).andReturn(mockConnection).once();
        mocksControl.replay();
        driver = MockJDBCDriver.getInstance().setMockJDBCAnswer(answer);

        connPool = new Hqcp(config);
        connPool.getConnection().close();
        assertSame(config, connPool.getConfig());
    }

    @Test
    public void testGetPoolId() throws Exception {
        answer = mocksControl.createMock(MockJDBCAnswer.class);
        expect(answer.answer()).andReturn(mockConnection).times(2);
        mocksControl.replay();
        driver = MockJDBCDriver.getInstance().setMockJDBCAnswer(answer);

        connPool = new Hqcp(config);
        connPool.getConnection().close();
        Hqcp connPool2 = new Hqcp(config);
        connPool2.getConnection().close();
        assertEquals(connPool.getPoolId() + 1, connPool2.getPoolId());
        connPool2.shutdown();
    }

}
