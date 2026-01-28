package io.hqwu.commons.cp;

import com.jolbox.bonecp.MockJDBCDriver;
import io.hqwu.commons.cp.dialect.MySQLPooledConnection;
import io.hqwu.commons.cp.test.CustomCommunicationsException;
import io.hqwu.commons.cp.test.CustomConnectionIsClosedException;
import io.hqwu.commons.cp.test.CustomStatementIsClosedException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLRecoverableException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * MySQLPooledConnection 测试类
 */
@DisplayName("MySQLPooledConnection 测试")
class MySQLPooledConnectionTest {

    private Hqcp pool;
    private MySQLPooledConnection connection;
    private Connection proxyConnection;

    @BeforeEach
    void setUp() throws Exception {
        // 配置 MySQL 数据源
        var config = new HqcpConfig();
        config.setUrl("jdbc:mysql://localhost:3306/testdb");
        config.setUsername("testuser");
        config.setPassword("testpass");
        config.setMinConnections(1);
        config.setMaxConnections(2);
        config.setMaxStatements(10);
        config.setMaxPreStatements(10);
        config.setJmxLevel(0);

        // 使用 MockJDBCDriver
        MockJDBCDriver.getInstance().setConnection(null);

        pool = new Hqcp(config);

        // 通过 Hqcp 获取连接，会根据 URL 自动创建 MySQLPooledConnection
        proxyConnection = pool.getConnection();

        // 使用 unwrap 方法获取实际的 MySQLPooledConnection
        connection = proxyConnection.unwrap(MySQLPooledConnection.class);
    }

    @AfterEach
    void tearDown() throws SQLException {
        if (proxyConnection != null && !proxyConnection.isClosed()) {
            proxyConnection.close();
        }
        if (pool != null) {
            pool.shutdown();
        }
    }

    @Test
    @DisplayName("测试MySQL特定错误码 0 配合消息被识别为致命异常")
    void testFetalException_ErrorCode0() {
        var exception = new SQLException("COMMUNICATIONS LINK FAILURE", "23S01", 0);

        assertTrue(connection.isFatalException(exception), "错误码 0 配合COMMUNICATIONS LINK FAILURE消息应被识别为致命异常");
    }

    @Test
    @DisplayName("测试MySQL特定错误码 1041 被识别为致命异常")
    void testFetalException_ErrorCode1041() {
        var exception = new SQLException("Out of memory", "HY001", 1041);

        assertTrue(connection.isFatalException(exception), "错误码 1041 应被识别为致命异常");
    }

    @Test
    @DisplayName("测试MySQL特定错误码 1040 被识别为致命异常")
    void testFetalException_ErrorCode1040() {
        var exception = new SQLException("Too many connections", "23004", 1040);

        assertTrue(connection.isFatalException(exception), "错误码 1040 应被识别为致命异常");
    }

    @Test
    @DisplayName("测试MySQL特定错误码 1042 被识别为致命异常")
    void testFetalException_ErrorCode1042() {
        var exception = new SQLException("Can't get hostname for your address", "23S01", 1042);

        assertTrue(connection.isFatalException(exception), "错误码 1042 应被识别为致命异常");
    }

    @Test
    @DisplayName("测试MySQL特定错误码 1045 被识别为致命异常")
    void testFetalException_ErrorCode1045() {
        var exception = new SQLException("Access denied", "28000", 1045);

        assertTrue(connection.isFatalException(exception), "错误码 1045 应被识别为致命异常");
    }

    @Test
    @DisplayName("测试MySQL特定错误码 1037 被识别为致命异常")
    void testFetalException_ErrorCode1037() {
        var exception = new SQLException("Out of memory", "HY001", 1037);

        assertTrue(connection.isFatalException(exception), "错误码 1037 应被识别为致命异常");
    }

    @Test
    @DisplayName("测试MySQL特定错误码 1142 被识别为致命异常")
    void testFetalException_ErrorCode1142() {
        var exception = new SQLException("Command denied", "42000", 1142);

        assertTrue(connection.isFatalException(exception), "错误码 1142 应被识别为致命异常");
    }

    @Test
    @DisplayName("测试MySQL SQLState 40001 被识别为致命异常")
    void testFetalException_SQLState40001() {
        var exception = new SQLException("Deadlock found", "40001", 1213);

        assertTrue(connection.isFatalException(exception), "SQLState 40001 应被识别为致命异常");
    }

    @Test
    @DisplayName("测试空SQLState被识别为致命异常")
    void testFetalException_NullSQLState() {
        var exception = new SQLException("Some error", null, 999);

        assertTrue(connection.isFatalException(exception), "空SQLState应被识别为致命异常");
    }

    @Test
    @DisplayName("测试包含COMMUNICATIONS LINK FAILURE消息的异常")
    void testFetalException_CommunicationsLinkFailure() {
        var exception = new SQLException("COMMUNICATIONS LINK FAILURE", "23S01", 0);

        assertTrue(connection.isFatalException(exception), "包含COMMUNICATIONS LINK FAILURE消息应被识别为致命异常");
    }

    @Test
    @DisplayName("测试所有MySQL特定错误码")
    void testFetalException_AllMySQLErrorCodes() {
        int[] fetalErrorCodes = {1040, 1041, 1042, 1043, 1047, 1081, 1129, 1130, 1045,
                                  1004, 1005, 1015, 1021, 1037, 1038, 1142, 1227, 1023, 1290};

        for (int errorCode : fetalErrorCodes) {
            var exception = new SQLException("MySQL Error", "HY000", errorCode);
            assertTrue(connection.isFatalException(exception), "错误码 " + errorCode + " 应被识别为致命异常");
        }
    }

    @Test
    @DisplayName("测试SQLState以5开头被识别为致命异常")
    void testFetalException_SQLState5() {
        var exception = new SQLException("Implementation error", "5S000");

        assertTrue(connection.isFatalException(exception), "SQLState 5xxxx 应被识别为致命异常");
    }

    @Test
    @DisplayName("测试非致命异常不被识别为致命异常")
    void testFetalException_NonFetal() {
        var exception = new SQLException("Constraint violation", "23000", 1062);

        assertFalse(connection.isFatalException(exception), "普通业务异常不应被识别为致命异常");
    }

    @Test
    @DisplayName("测试异常类名以CommunicationsException结尾 - 使用自定义异常避开SQLRecoverableException")
    void testFetalException_CommunicationsExceptionClassName() {
        // 使用自定义异常类，不继承SQLRecoverableException，sqlState="23000"避开父类拦截
        var exception = new CustomCommunicationsException("Communications error");

        assertTrue(connection.isFatalException(exception), "异常类名以CommunicationsException结尾应被识别为致命异常");
    }

    @Test
    @DisplayName("测试流式结果集特殊消息")
    void testFetalException_StreamingResultSetMessage() {
        var message = "Streaming result set com.mysql.jdbc.RowDataDynamic@12345 " +
                      "is still active. No statements may be issued when any streaming result sets are open and in use on a given connection. " +
                      "Ensure that you have called .close() on any active streaming result sets before attempting more queries.";
        var exception = new SQLException(message, "HY000", 0);

        assertTrue(connection.isFatalException(exception), "流式结果集特殊消息应被识别为致命异常");
    }

    @Test
    @DisplayName("测试错误文本包含COULD NOT CREATE CONNECTION")
    void testFetalException_CouldNotCreateConnection() {
        var exception = new SQLException("Could not create connection to database server", "23001", 0);

        assertTrue(connection.isFatalException(exception), "包含COULD NOT CREATE CONNECTION消息应被识别为致命异常");
    }

    @Test
    @DisplayName("测试错误文本包含NO DATASOURCE")
    void testFetalException_NoDatasource() {
        var exception = new SQLException("No datasource available", "23001", 999);

        assertTrue(connection.isFatalException(exception), "包含NO DATASOURCE消息应被识别为致命异常");
    }

    @Test
    @DisplayName("测试错误文本包含NO ALIVE DATASOURCE")
    void testFetalException_NoAliveDatasource() {
        var exception = new SQLException("No alive datasource", "23001", 999);

        assertTrue(connection.isFatalException(exception), "包含NO ALIVE DATASOURCE消息应被识别为致命异常");
    }

    @Test
    @DisplayName("测试cause链中包含SocketTimeoutException")
    void testFetalException_SocketTimeoutExceptionInCause() {
        var rootCause = new java.net.SocketTimeoutException("Read timed out");
        var exception = new SQLException("Query timeout", "HY000", 0, rootCause);

        assertTrue(connection.isFatalException(exception), "cause链中包含SocketTimeoutException应被识别为致命异常");
    }

    @Test
    @DisplayName("测试cause链中类名以CommunicationsException结尾 - 使用自定义异常")
    void testFetalException_CommunicationsExceptionInCause() {
        // 使用自定义异常类作为cause，不继承SQLRecoverableException
        var rootCause = new CustomCommunicationsException("Communications error");
        var exception = new SQLException("Database error", "23S01", 0, rootCause);

        assertTrue(connection.isFatalException(exception), "cause链中类名以CommunicationsException结尾应被识别为致命异常");
    }

    @Test
    @DisplayName("测试cause链中类名以ConnectionIsClosedException结尾 - 使用自定义异常")
    void testFetalException_ConnectionIsClosedExceptionInCause() {
        // 使用自定义异常类作为cause
        var rootCause = new CustomConnectionIsClosedException("Connection is closed");
        var exception = new SQLException("Database error", "23003", 0, rootCause);

        assertTrue(connection.isFatalException(exception), "cause链中类名以ConnectionIsClosedException结尾应被识别为致命异常");
    }

    @Test
    @DisplayName("测试cause链中类名以StatementIsClosedException结尾 - 使用自定义异常")
    void testFetalException_StatementIsClosedExceptionInCause() {
        // 使用自定义异常类作为cause
        var rootCause = new CustomStatementIsClosedException("Statement is closed");
        var exception = new SQLException("Database error", "23003", 0, rootCause);

        assertTrue(connection.isFatalException(exception), "cause链中类名以StatementIsClosedException结尾应被识别为致命异常");
    }



    @Test
    @DisplayName("测试cause链中第3层包含SocketTimeoutException")
    void testFetalException_SocketTimeoutExceptionInDeepCause() {
        // 创建一个3层深度的cause链
        var rootCause = new java.net.SocketTimeoutException("Read timed out");
        var cause2 = new Exception("Layer 2", rootCause);
        var cause1 = new Exception("Layer 1", cause2);
        var exception = new SQLException("Database error", "23S01", 0, cause1);

        assertTrue(connection.isFatalException(exception), "cause链中第3层的SocketTimeoutException应被识别为致命异常");
    }

    @Test
    @DisplayName("测试空消息不触发致命异常")
    void testFetalException_EmptyMessage() {
        var exception = new SQLException("", "23000", 1062);

        assertFalse(connection.isFatalException(exception), "空消息不应触发致命异常判断");
    }

    @Test
    @DisplayName("测试null消息不触发致命异常")
    void testFetalException_NullMessage() {
        var exception = new SQLException(null, "23000", 1062);

        assertFalse(connection.isFatalException(exception), "null消息不应触发致命异常判断");
    }

    @Test
    @DisplayName("测试错误码0但不包含特殊消息")
    void testFetalException_ErrorCode0WithoutSpecialMessage() {
        var exception = new SQLException("Some random error", "HY000", 0);

        assertFalse(connection.isFatalException(exception), "错误码0但不包含特殊消息不应被识别为致命异常");
    }

    @Test
    @DisplayName("测试SQLRecoverableException被父类识别为致命异常")
    void testFetalException_SQLRecoverableException() {
        var exception = new SQLRecoverableException("Recoverable error", "08006", 0);

        assertTrue(connection.isFatalException(exception), "SQLRecoverableException应被父类识别为致命异常");
    }

    @Test
    @DisplayName("测试错误文本包含COULD NOT CREATE CONNECTION且errorCode != 0")
    void testFetalException_CouldNotCreateConnectionNonZeroErrorCode() {
        // errorCode != 0时，仍然应该识别为致命异常
        var exception = new SQLException("Could not create connection to database server", "23001", 999);

        assertTrue(connection.isFatalException(exception), "包含COULD NOT CREATE CONNECTION消息应被识别为致命异常，无论errorCode是否为0");
    }

    @Test
    @DisplayName("测试错误文本包含NO DATASOURCE且errorCode != 0")
    void testFetalException_NoDatasourceNonZeroErrorCode() {
        // errorCode != 0时，仍然应该识别为致命异常
        var exception = new SQLException("No datasource available", "23001", 999);

        assertTrue(connection.isFatalException(exception), "包含NO DATASOURCE消息应被识别为致命异常，无论errorCode是否为0");
    }

    @Test
    @DisplayName("测试错误文本包含NO ALIVE DATASOURCE且errorCode != 0")
    void testFetalException_NoAliveDatasourceNonZeroErrorCode() {
        // errorCode != 0时，仍然应该识别为致命异常
        var exception = new SQLException("No alive datasource", "23001", 999);

        assertTrue(connection.isFatalException(exception), "包含NO ALIVE DATASOURCE消息应被识别为致命异常，无论errorCode是否为0");
    }
}


