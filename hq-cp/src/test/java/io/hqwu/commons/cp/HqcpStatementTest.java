package io.hqwu.commons.cp;

import com.jolbox.bonecp.*;
import com.umpay.commons.util.Logger;
import org.easymock.classextension.IMocksControl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.expect;
import static org.easymock.classextension.EasyMock.createNiceControl;
import static org.easymock.classextension.EasyMock.makeThreadSafe;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Created with IntelliJ IDEA for plat-arch-svn
 * User: taige
 * Date: 13-10-31
 * Time: 上午10:18
 */
public class HqcpStatementTest {
    private static final Logger LOGGER = new Logger();

    private MockJDBCDriver driver;
    private MockJDBCAnswer answer;

    private MockConnection mockConnection;
    private MockJDBCStatement mockStatement;

    private HqcpConfig config;
    private Hqcp connPool;

    private IMocksControl mocksControl;
    @BeforeEach
    public void setUp() throws SQLException {
        mocksControl = createNiceControl();
//        mockConnection = mocksControl.createMock(MockConnection.class);
//        mockConnection.close();
//        expectLastCall().once();
        mockConnection = new MockConnection();
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
        expect(config.isVerbose()).andReturn(true).anyTimes();
        expect(config.isCommitOnClose()).andReturn(true).anyTimes();
        expect(config.isPrintSql()).andReturn(true).anyTimes();
        expect(config.isTransactionMode()).andReturn(true).anyTimes();
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
        expect(mockStatement.executeQuery((String) anyObject())).andReturn(rs).anyTimes();
        expect(mockStatement.getResultSetType()).andReturn(ResultSet.TYPE_FORWARD_ONLY).anyTimes();
        expect(mockStatement.getResultSetConcurrency()).andReturn(ResultSet.CONCUR_READ_ONLY).anyTimes();

        //expect(mockConnection.createStatement()).andReturn(mockStatement).anyTimes();

        makeThreadSafe(config, true);
        //makeThreadSafe(mockConnection, true);
    }

    @AfterEach
    public void tearDown() throws SQLException {
        if (connPool != null) {
            connPool.shutdown();
            mocksControl.verify();
            connPool = null;
        }
        if (driver != null) {
            driver.disable();
            driver = null;
        }
    }

    @Test
    public void testCreateStatement() throws Exception {
        //answer = mocksControl.createMock(MockJDBCAnswer.class);
        //expect(answer.answer()).andReturn(mockConnection).once();
        mocksControl.replay();
        driver = new MockJDBCDriver();

        connPool = new Hqcp(config);
        Connection conn = connPool.getConnection();

        try {
            Statement stmt = conn.createStatement();
            stmt.close();

            Statement stmt1 = conn.createStatement();
            assertSame(stmt1, stmt);
            stmt1.close();
        } finally {
            conn.close();

        }
    }

    @Test
    public void testCreateStatement_2() throws Exception {
        //answer = mocksControl.createMock(MockJDBCAnswer.class);
        //expect(answer.answer()).andReturn(mockConnection).once();
        mocksControl.replay();
        driver = new MockJDBCDriver();

        connPool = new Hqcp(config);
        Connection conn = connPool.getConnection();

        try {
            Statement stmt0 = conn.createStatement();
            stmt0.close();

            Statement stmt1 = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
            stmt1.close();

            Statement stmt0_1 = conn.createStatement();
            assertSame(stmt0, stmt0_1);
            stmt0_1.close();

            Statement stmt2 = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
            stmt2.close();

            Statement stmt2_1 = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
            assertNotSame(stmt2, stmt2_1);
            stmt2_1.close();
        } finally {
            conn.close();

        }
    }

}
