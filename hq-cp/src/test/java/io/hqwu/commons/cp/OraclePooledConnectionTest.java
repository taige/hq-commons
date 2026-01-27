package io.hqwu.commons.cp;

import com.jolbox.bonecp.MockJDBCDriver;
import io.hqwu.commons.cp.dialect.OraclePooledConnection;
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
 * OraclePooledConnection 测试类
 */
@DisplayName("OraclePooledConnection 测试")
class OraclePooledConnectionTest {

    private Hqcp pool;
    private OraclePooledConnection connection;
    private Connection proxyConnection;

    @BeforeEach
    void setUp() throws Exception {
        // 配置 Oracle 数据源
        var config = new HqcpConfig();
        config.setUrl("jdbc:oracle:thin:@localhost:1521:orcl");
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

        // 通过 Hqcp 获取连接，会根据 URL 自动创建 OraclePooledConnection
        proxyConnection = pool.getConnection();

        // 使用 unwrap 方法获取实际的 OraclePooledConnection
        connection = proxyConnection.unwrap(OraclePooledConnection.class);
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
    @DisplayName("测试Oracle特定错误码 17002 被识别为致命异常")
    void testFetalException_ErrorCode17002() {
        var exception = new SQLException("IO Exception", "42000", 17002);

        assertTrue(connection.isFetalException(exception), "错误码 17002 应被识别为致命异常");
    }

    @Test
    @DisplayName("测试Oracle特定错误码 17008 被识别为致命异常")
    void testFetalException_ErrorCode17008() {
        var exception = new SQLException("Closed Connection", "42000", 17008);

        assertTrue(connection.isFetalException(exception), "错误码 17008 应被识别为致命异常");
    }

    @Test
    @DisplayName("测试Oracle特定错误码 17410 被识别为致命异常")
    void testFetalException_ErrorCode17410() {
        var exception = new SQLException("No more data to read from socket", "42000", 17410);

        assertTrue(connection.isFetalException(exception), "错误码 17410 应被识别为致命异常");
    }

    @Test
    @DisplayName("测试Oracle特定错误码 1089 被识别为致命异常")
    void testFetalException_ErrorCode1089() {
        var exception = new SQLException("Immediate shutdown in progress", "42000", 1089);

        assertTrue(connection.isFetalException(exception), "错误码 1089 应被识别为致命异常");
    }

    @Test
    @DisplayName("测试Oracle特定错误码 1090 被识别为致命异常")
    void testFetalException_ErrorCode1090() {
        var exception = new SQLException("Shutdown in progress", "42000", 1090);

        assertTrue(connection.isFetalException(exception), "错误码 1090 应被识别为致命异常");
    }

    @Test
    @DisplayName("测试Oracle特定错误码 17401 被识别为致命异常")
    void testFetalException_ErrorCode17401() {
        var exception = new SQLException("Protocol violation", "42000", 17401);

        assertTrue(connection.isFetalException(exception), "错误码 17401 应被识别为致命异常");
    }

    @Test
    @DisplayName("测试Oracle特定错误码 17442 被识别为致命异常")
    void testFetalException_ErrorCode17442() {
        var exception = new SQLException("Refcursor value is invalid", "42000", 17442);

        assertTrue(connection.isFetalException(exception), "错误码 17442 应被识别为致命异常");
    }

    @Test
    @DisplayName("测试Oracle特定错误码 25408 被识别为致命异常")
    void testFetalException_ErrorCode25408() {
        var exception = new SQLException("Can not safely replay call", "42000", 25408);

        assertTrue(connection.isFetalException(exception), "错误码 25408 应被识别为致命异常");
    }

    @Test
    @DisplayName("测试父类的SQLRecoverableException被识别为致命异常")
    void testFetalException_SQLRecoverableException() {
        var exception = new SQLRecoverableException("Connection lost");

        assertTrue(connection.isFetalException(exception), "SQLRecoverableException 应被识别为致命异常");
    }

    @Test
    @DisplayName("测试父类的08开头的SQLState被识别为致命异常")
    void testFetalException_SQLState08() {
        var exception = new SQLException("Connection exception", "08006");

        assertTrue(connection.isFetalException(exception), "SQLState 08xxx 应被识别为致命异常");
    }

    @Test
    @DisplayName("测试非致命异常不被识别为致命异常")
    void testFetalException_NonFetal() {
        var exception = new SQLException("Constraint violation", "23000", 1);

        assertFalse(connection.isFetalException(exception), "普通业务异常不应被识别为致命异常");
    }

    @Test
    @DisplayName("测试所有Oracle特定错误码")
    void testFetalException_AllOracleErrorCodes() {
        int[] fetalErrorCodes = {17002, 17008, 17410, 1089, 1090, 17401, 17442, 25408};

        for (int errorCode : fetalErrorCodes) {
            var exception = new SQLException("Oracle Error", "42000", errorCode);
            assertTrue(connection.isFetalException(exception), "错误码 " + errorCode + " 应被识别为致命异常");
        }
    }

    @Test
    @DisplayName("测试SQLState以5开头被识别为致命异常")
    void testFetalException_SQLState5() {
        var exception = new SQLException("Implementation error", "5S000");

        assertTrue(connection.isFetalException(exception), "SQLState 5xxxx 应被识别为致命异常");
    }

    @Test
    @DisplayName("测试空SQLState会被父类识别为致命异常")
    void testFetalException_NullSQLState() {
        var exception = new SQLException("Some error", null, 9999);

        assertTrue(connection.isFetalException(exception), "空SQLState会在父类被识别为致命异常");
    }

    @Test
    @DisplayName("测试TNS错误码范围12100被识别为致命异常")
    void testFetalException_TNSErrorCode12100() {
        var exception = new SQLException("TNS:could not resolve the connect identifier specified", "42000", 12100);

        assertTrue(connection.isFetalException(exception), "TNS错误码 12100 应被识别为致命异常");
    }

    @Test
    @DisplayName("测试TNS错误码范围12299被识别为致命异常")
    void testFetalException_TNSErrorCode12299() {
        var exception = new SQLException("TNS error", "42000", 12299);

        assertTrue(connection.isFetalException(exception), "TNS错误码 12299 应被识别为致命异常");
    }

    @Test
    @DisplayName("测试TNS错误码范围12150被识别为致命异常")
    void testFetalException_TNSErrorCode12150() {
        var exception = new SQLException("TNS:could not send data", "42000", 12150);

        assertTrue(connection.isFetalException(exception), "TNS错误码 12150 应被识别为致命异常");
    }

    @Test
    @DisplayName("测试错误消息包含SOCKET被识别为致命异常")
    void testFetalException_MessageContainsSocket() {
        var exception = new SQLException("SOCKET read error", "42000", 999);

        assertTrue(connection.isFetalException(exception), "错误消息包含SOCKET应被识别为致命异常");
    }

    @Test
    @DisplayName("测试错误消息包含套接字被识别为致命异常")
    void testFetalException_MessageContainsChinese套接字() {
        var exception = new SQLException("套接字连接失败", "42000", 999);

        assertTrue(connection.isFetalException(exception), "错误消息包含套接字应被识别为致命异常");
    }

    @Test
    @DisplayName("测试错误消息包含CONNECTION_HAS_ALREADY_BEEN_CLOSED被识别为致命异常")
    void testFetalException_MessageContainsConnectionClosed() {
        var exception = new SQLException("CONNECTION HAS ALREADY BEEN CLOSED", "42000", 999);

        assertTrue(connection.isFetalException(exception), "错误消息包含CONNECTION HAS ALREADY BEEN CLOSED应被识别为致命异常");
    }

    @Test
    @DisplayName("测试错误消息包含BROKEN_PIPE被识别为致命异常")
    void testFetalException_MessageContainsBrokenPipe() {
        var exception = new SQLException("BROKEN PIPE detected", "42000", 999);

        assertTrue(connection.isFetalException(exception), "错误消息包含BROKEN PIPE应被识别为致命异常");
    }

    @Test
    @DisplayName("测试错误消息包含管道已结束被识别为致命异常")
    void testFetalException_MessageContainsChinese管道已结束() {
        var exception = new SQLException("管道已结束", "42000", 999);

        assertTrue(connection.isFetalException(exception), "错误消息包含管道已结束应被识别为致命异常");
    }

    @Test
    @DisplayName("测试Oracle用户自定义错误码20000-20999不检查消息文本")
    void testFetalException_UserDefinedErrorCodeNotCheckMessage() {
        // 即使消息包含SOCKET，但错误码在20000-20999范围内，不会匹配消息文本
        var exception = new SQLException("SOCKET error in user code", "42000", 20500);

        assertFalse(connection.isFetalException(exception), "用户自定义错误码20000-20999不应通过消息文本匹配");
    }

    @Test
    @DisplayName("测试TNS错误码12300不在范围内")
    void testFetalException_TNSErrorCode12300NotInRange() {
        var exception = new SQLException("Not TNS error", "42000", 12300);

        assertFalse(connection.isFetalException(exception), "错误码12300不在TNS范围内，不应被识别为致命异常");
    }

    @Test
    @DisplayName("测试TNS错误码12099不在范围内")
    void testFetalException_TNSErrorCode12099NotInRange() {
        var exception = new SQLException("Not TNS error", "42000", 12099);

        assertFalse(connection.isFetalException(exception), "错误码12099不在TNS范围内，不应被识别为致命异常");
    }

    @Test
    @DisplayName("测试Oracle特定错误码28被识别为致命异常")
    void testFetalException_ErrorCode28() {
        var exception = new SQLException("your session has been killed", "42000", 28);

        assertTrue(connection.isFetalException(exception), "错误码 28 应被识别为致命异常");
    }

    @Test
    @DisplayName("测试Oracle特定错误码600被识别为致命异常")
    void testFetalException_ErrorCode600() {
        var exception = new SQLException("Internal oracle error", "42000", 600);

        assertTrue(connection.isFetalException(exception), "错误码 600 应被识别为致命异常");
    }

    @Test
    @DisplayName("测试Oracle特定错误码1012被识别为致命异常")
    void testFetalException_ErrorCode1012() {
        var exception = new SQLException("not logged on", "42000", 1012);

        assertTrue(connection.isFetalException(exception), "错误码 1012 应被识别为致命异常");
    }

    @Test
    @DisplayName("测试Oracle特定错误码3113被识别为致命异常")
    void testFetalException_ErrorCode3113() {
        var exception = new SQLException("end-of-file on communication channel", "42000", 3113);

        assertTrue(connection.isFetalException(exception), "错误码 3113 应被识别为致命异常");
    }

    @Test
    @DisplayName("测试Oracle特定错误码17001被识别为致命异常")
    void testFetalException_ErrorCode17001() {
        var exception = new SQLException("Internal Error", "42000", 17001);

        assertTrue(connection.isFetalException(exception), "错误码 17001 应被识别为致命异常");
    }

    @Test
    @DisplayName("测试Oracle特定错误码17024被识别为致命异常")
    void testFetalException_ErrorCode17024() {
        var exception = new SQLException("No data read", "42000", 17024);

        assertTrue(connection.isFetalException(exception), "错误码 17024 应被识别为致命异常");
    }

    @Test
    @DisplayName("测试Oracle特定错误码25407被识别为致命异常")
    void testFetalException_ErrorCode25407() {
        var exception = new SQLException("connection terminated", "42000", 25407);

        assertTrue(connection.isFetalException(exception), "错误码 25407 应被识别为致命异常");
    }

    @Test
    @DisplayName("测试Oracle特定错误码30676被识别为致命异常")
    void testFetalException_ErrorCode30676() {
        var exception = new SQLException("socket read or write failed", "42000", 30676);

        assertTrue(connection.isFetalException(exception), "错误码 30676 应被识别为致命异常");
    }

    @Test
    @DisplayName("测试用户自定义错误码边界20000不检查消息文本")
    void testFetalException_UserDefinedErrorCode20000() {
        var exception = new SQLException("SOCKET error", "42000", 20000);

        assertFalse(connection.isFetalException(exception), "用户自定义错误码20000不应通过消息文本匹配");
    }

    @Test
    @DisplayName("测试用户自定义错误码边界20999不检查消息文本")
    void testFetalException_UserDefinedErrorCode20999() {
        var exception = new SQLException("BROKEN PIPE", "42000", 20999);

        assertFalse(connection.isFetalException(exception), "用户自定义错误码20999不应通过消息文本匹配");
    }

    @Test
    @DisplayName("测试错误码21000会检查消息文本")
    void testFetalException_ErrorCode21000ChecksMessage() {
        var exception = new SQLException("SOCKET error", "42000", 21000);

        assertTrue(connection.isFetalException(exception), "错误码21000应检查消息文本并匹配SOCKET");
    }

    @Test
    @DisplayName("测试错误码19999会检查消息文本")
    void testFetalException_ErrorCode19999ChecksMessage() {
        var exception = new SQLException("套接字 error", "42000", 19999);

        assertTrue(connection.isFetalException(exception), "错误码19999应检查消息文本并匹配套接字");
    }

    @Test
    @DisplayName("测试错误消息小写socket也能匹配")
    void testFetalException_MessageContainsLowercaseSocket() {
        var exception = new SQLException("socket read error", "42000", 999);

        assertTrue(connection.isFetalException(exception), "错误消息包含小写socket应被识别为致命异常(会转大写)");
    }

    @Test
    @DisplayName("测试错误消息小写broken_pipe也能匹配")
    void testFetalException_MessageContainsLowercaseBrokenPipe() {
        var exception = new SQLException("broken pipe detected", "42000", 999);

        assertTrue(connection.isFetalException(exception), "错误消息包含小写broken pipe应被识别为致命异常(会转大写)");
    }

    @Test
    @DisplayName("测试SQLState以6开头被识别为致命异常")
    void testFetalException_SQLState6() {
        var exception = new SQLException("Implementation error", "6S000");

        assertTrue(connection.isFetalException(exception), "SQLState 6xxxx 应被识别为致命异常");
    }

    @Test
    @DisplayName("测试SQLState以9开头被识别为致命异常")
    void testFetalException_SQLState9() {
        var exception = new SQLException("Implementation error", "9S000");

        assertTrue(connection.isFetalException(exception), "SQLState 9xxxx 应被识别为致命异常");
    }

    @Test
    @DisplayName("测试SQLState以4开头不被识别为致命异常")
    void testFetalException_SQLState4() {
        var exception = new SQLException("Some error", "42000", 999);

        assertFalse(connection.isFetalException(exception), "SQLState 4xxxx 不应被识别为致命异常");
    }

    @Test
    @DisplayName("测试错误消息不包含关键字不被识别为致命异常")
    void testFetalException_MessageNoKeywords() {
        var exception = new SQLException("Some random error", "42000", 999);

        assertFalse(connection.isFetalException(exception), "错误消息不包含关键字不应被识别为致命异常");
    }

    @Test
    @DisplayName("测试负数错误码也能被识别-17002")
    void testFetalException_NegativeErrorCode17002() {
        var exception = new SQLException("IO Exception", "42000", -17002);

        assertTrue(connection.isFetalException(exception), "负数错误码 -17002 应被识别为致命异常(取绝对值)");
    }

    @Test
    @DisplayName("测试负数TNS错误码-12150")
    void testFetalException_NegativeTNSErrorCode() {
        var exception = new SQLException("TNS error", "42000", -12150);

        assertTrue(connection.isFetalException(exception), "负数TNS错误码 -12150 应被识别为致命异常(取绝对值)");
    }
}
