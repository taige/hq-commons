package io.hqwu.commons.cp;

import com.jolbox.bonecp.MockJDBCDriver;
import io.hqwu.commons.cp.dialect.OceanBasePooledConnection;
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
 * OceanBasePooledConnection 测试类
 *
 * @author Wu, Hongqiang
 * @since 2026-01-27
 */
@DisplayName("OceanBasePooledConnection 测试")
class OceanBasePooledConnectionTest {

    private Hqcp pool;
    private OceanBasePooledConnection connection;
    private Connection proxyConnection;

    @BeforeEach
    void setUp() throws Exception {
        // 配置 OceanBase 数据源
        var config = new HqcpConfig();
        config.setUrl("jdbc:oceanbase://localhost:2883/testdb");
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

        // 通过 Hqcp 获取连接，会根据 URL 自动创建 OceanBasePooledConnection
        proxyConnection = pool.getConnection();

        // 使用 unwrap 方法获取实际的 OceanBasePooledConnection
        connection = proxyConnection.unwrap(OceanBasePooledConnection.class);
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

    // ==================== 测试 OceanBase 特有错误码范围 (-10000 到 -9000) ====================

    @Test
    @DisplayName("测试OceanBase错误码边界-10000被识别为致命异常")
    void testFetalException_OceanBaseErrorCode_Minus10000() {
        var exception = new SQLException("OceanBase internal error", "42000", -10000);

        assertTrue(connection.isFetalException(exception), "错误码 -10000 应被识别为致命异常");
    }

    @Test
    @DisplayName("测试OceanBase错误码边界-9000被识别为致命异常")
    void testFetalException_OceanBaseErrorCode_Minus9000() {
        var exception = new SQLException("OceanBase internal error", "42000", -9000);

        assertTrue(connection.isFetalException(exception), "错误码 -9000 应被识别为致命异常");
    }

    @Test
    @DisplayName("测试OceanBase错误码中间值-9500被识别为致命异常")
    void testFetalException_OceanBaseErrorCode_Minus9500() {
        var exception = new SQLException("OceanBase internal error", "42000", -9500);

        assertTrue(connection.isFetalException(exception), "错误码 -9500 应被识别为致命异常");
    }

    @Test
    @DisplayName("测试OceanBase错误码-9100被识别为致命异常")
    void testFetalException_OceanBaseErrorCode_Minus9100() {
        var exception = new SQLException("OceanBase internal error", "42000", -9100);

        assertTrue(connection.isFetalException(exception), "错误码 -9100 应被识别为致命异常");
    }

    @Test
    @DisplayName("测试OceanBase错误码-9999被识别为致命异常")
    void testFetalException_OceanBaseErrorCode_Minus9999() {
        var exception = new SQLException("OceanBase internal error", "42000", -9999);

        assertTrue(connection.isFetalException(exception), "错误码 -9999 应被识别为致命异常");
    }

    @Test
    @DisplayName("测试OceanBase错误码-9001被识别为致命异常")
    void testFetalException_OceanBaseErrorCode_Minus9001() {
        var exception = new SQLException("OceanBase internal error", "42000", -9001);

        assertTrue(connection.isFetalException(exception), "错误码 -9001 应被识别为致命异常");
    }

    // ==================== 测试边界外的错误码不被识别 ====================

    @Test
    @DisplayName("测试OceanBase错误码-10001不在范围内")
    void testFetalException_OceanBaseErrorCode_Minus10001_NotInRange() {
        var exception = new SQLException("Some error", "42000", -10001);

        assertFalse(connection.isFetalException(exception), "错误码 -10001 不在范围内，不应被识别为致命异常");
    }

    @Test
    @DisplayName("测试OceanBase错误码-8999不在范围内")
    void testFetalException_OceanBaseErrorCode_Minus8999_NotInRange() {
        var exception = new SQLException("Some error", "42000", -8999);

        assertFalse(connection.isFetalException(exception), "错误码 -8999 不在范围内，不应被识别为致命异常");
    }

    @Test
    @DisplayName("测试普通错误码999不被识别为致命异常")
    void testFetalException_NormalErrorCode999() {
        var exception = new SQLException("Some error", "42000", 999);

        assertFalse(connection.isFetalException(exception), "普通错误码 999 不应被识别为致命异常");
    }

    @Test
    @DisplayName("测试正数错误码9000不被识别为致命异常")
    void testFetalException_PositiveErrorCode9000() {
        var exception = new SQLException("Some error", "42000", 9000);

        assertFalse(connection.isFetalException(exception), "正数错误码 9000 不应被识别为致命异常");
    }

    @Test
    @DisplayName("测试正数错误码10000不被识别为致命异常")
    void testFetalException_PositiveErrorCode10000() {
        var exception = new SQLException("Some error", "42000", 10000);

        assertFalse(connection.isFetalException(exception), "正数错误码 10000 不应被识别为致命异常");
    }

    // ==================== 测试继承自父类的逻辑仍然有效 ====================

    @Test
    @DisplayName("测试继承自父类的SQLRecoverableException仍被识别为致命异常")
    void testFetalException_SQLRecoverableException() {
        var exception = new SQLRecoverableException("Connection lost", "08S01");

        assertTrue(connection.isFetalException(exception), "SQLRecoverableException 应被识别为致命异常");
    }

    @Test
    @DisplayName("测试继承自父类的SQLState_08开头仍被识别为致命异常")
    void testFetalException_SQLState08() {
        var exception = new SQLException("Connection exception", "08001");

        assertTrue(connection.isFetalException(exception), "SQLState 08001 应被识别为致命异常");
    }

    @Test
    @DisplayName("测试继承自父类的SQLState_5开头仍被识别为致命异常")
    void testFetalException_SQLState5() {
        var exception = new SQLException("Implementation error", "5S000");

        assertTrue(connection.isFetalException(exception), "SQLState 5xxxx 应被识别为致命异常");
    }

    @Test
    @DisplayName("测试继承自MySQL的错误码1041仍被识别为致命异常")
    void testFetalException_MySQL_ErrorCode1041() {
        var exception = new SQLException("Out of resources", "42000", 1041);

        assertTrue(connection.isFetalException(exception), "MySQL错误码 1041 应被识别为致命异常");
    }

    @Test
    @DisplayName("测试继承自MySQL的SQLState_40001仍被识别为致命异常")
    void testFetalException_MySQL_SQLState40001_Deadlock() {
        var exception = new SQLException("Deadlock detected", "40001");

        assertTrue(connection.isFetalException(exception), "MySQL SQLState 40001 应被识别为致命异常");
    }

    // ==================== 组合测试：OceanBase错误码 + 特殊SQLState ====================

    @Test
    @DisplayName("测试OceanBase错误码-9500配合SQLState_08仍被识别为致命异常")
    void testFetalException_OceanBaseErrorCode_With_SQLState08() {
        var exception = new SQLException("OceanBase connection error", "08001", -9500);

        assertTrue(connection.isFetalException(exception), "OceanBase错误码配合SQLState 08 应被识别为致命异常");
    }

    @Test
    @DisplayName("测试OceanBase错误码-9500配合正常SQLState被识别为致命异常")
    void testFetalException_OceanBaseErrorCode_With_NormalSQLState() {
        var exception = new SQLException("OceanBase error", "42000", -9500);

        assertTrue(connection.isFetalException(exception), "OceanBase错误码 -9500 应被识别为致命异常");
    }

    // ==================== 边界测试：确保范围判断的准确性 ====================

    @Test
    @DisplayName("测试所有OceanBase错误码范围边界")
    void testFetalException_AllOceanBaseErrorCodeBoundaries() {
        // 范围内：-10000 到 -9000
        int[] fetalErrorCodes = {-10000, -9999, -9500, -9100, -9001, -9000};
        for (int errorCode : fetalErrorCodes) {
            var exception = new SQLException("OceanBase error", "42000", errorCode);
            assertTrue(connection.isFetalException(exception),
                    "错误码 " + errorCode + " 应被识别为致命异常");
        }

        // 范围外
        int[] nonFetalErrorCodes = {-10001, -8999, -1, 0, 1, 999, 9000, 10000};
        for (int errorCode : nonFetalErrorCodes) {
            var exception = new SQLException("Some error", "42000", errorCode);
            assertFalse(connection.isFetalException(exception),
                    "错误码 " + errorCode + " 不应被识别为致命异常");
        }
    }

    @Test
    @DisplayName("测试OceanBase错误码-10000边界精确性")
    void testFetalException_Boundary_Minus10000_Exact() {
        // -10000 应该被识别
        var exception1 = new SQLException("OceanBase error", "42000", -10000);
        assertTrue(connection.isFetalException(exception1), "错误码 -10000 应被识别为致命异常");

        // -10001 不应该被识别
        var exception2 = new SQLException("Some error", "42000", -10001);
        assertFalse(connection.isFetalException(exception2), "错误码 -10001 不应被识别为致命异常");
    }

    @Test
    @DisplayName("测试OceanBase错误码-9000边界精确性")
    void testFetalException_Boundary_Minus9000_Exact() {
        // -9000 应该被识别
        var exception1 = new SQLException("OceanBase error", "42000", -9000);
        assertTrue(connection.isFetalException(exception1), "错误码 -9000 应被识别为致命异常");

        // -8999 不应该被识别
        var exception2 = new SQLException("Some error", "42000", -8999);
        assertFalse(connection.isFetalException(exception2), "错误码 -8999 不应被识别为致命异常");
    }

    // ==================== 确保不影响非OceanBase场景 ====================

    @Test
    @DisplayName("测试普通SQLException不被误判")
    void testFetalException_NormalSQLException() {
        var exception = new SQLException("Normal error", "42000", 1234);

        assertFalse(connection.isFetalException(exception), "普通SQLException不应被识别为致命异常");
    }

    @Test
    @DisplayName("测试空SQLState且非OceanBase错误码会调用父类判断")
    void testFetalException_NullSQLState_NonOceanBaseErrorCode() {
        var exception = new SQLException("Some error", null, 1234);

        assertTrue(connection.isFetalException(exception), "空SQLState会在父类被识别为致命异常");
    }

    @Test
    @DisplayName("测试空SQLState且为OceanBase错误码会被识别为致命异常")
    void testFetalException_NullSQLState_OceanBaseErrorCode() {
        var exception = new SQLException("OceanBase error", null, -9500);

        assertTrue(connection.isFetalException(exception), "OceanBase错误码应被识别为致命异常，即使SQLState为null");
    }
}
