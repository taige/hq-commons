package io.hqwu.commons.cp;

import com.jolbox.bonecp.MockJDBCAnswer;
import com.jolbox.bonecp.MockJDBCDriver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Created with IntelliJ IDEA for plat-arch-svn
 * User: taige
 * Date: 13-11-5
 * Time: 下午10:53
 */
public class ConnectionFactoryTest {

    MockJDBCDriver driver;

    @BeforeEach
    public void setUp() throws Exception {
        driver = MockJDBCDriver.getInstance();
    }

    @AfterEach
    public void tearDown() throws Exception {
        if (driver != null) {
            driver.disable();
            driver = null;
        }
        ConnectionFactory.shutdown();
        ConnectionFactory.shutdown("jdbc2");
        ConnectionFactory.shutdown("jdbc1_zh_CN");
        assertEquals(0, getPoolCache().size());
    }

    // 获取并重置poolCache的工具方法
    private ConcurrentHashMap<String, Hqcp> getPoolCache() throws Exception {
        Field field = ConnectionFactory.class.getDeclaredField("poolCache");
        field.setAccessible(true);
        return (ConcurrentHashMap<String, Hqcp>) field.get(null);
    }

    @Test
    void test_getHqcpInstance_shouldCacheInstancesByJdbc() throws SQLException {
        // Act
        Hqcp instance1 = ConnectionFactory.getHqcpInstance("jdbc");
        Hqcp instance2 = ConnectionFactory.getHqcpInstance("jdbc");
        Hqcp instance3 = ConnectionFactory.getHqcpInstance("jdbc2");

        // Assert
        assertAll(
                () -> assertNotNull(instance1, "实例不应为null"),
                () -> assertSame(instance1, instance2, "相同JDBC应返回缓存实例"),
                () -> assertNotSame(instance3, instance2, "不同JDBC应返回不同实例"),
                () -> assertEquals(2, getPoolCache().size(), "缓存池应包含2个实例")
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"jdbc1", "invalid_jdbc"})
    void test_getHqcpInstance_shouldThrowWhenInvalidJdbc(String invalidJdbc) {
        // Act & Assert
        SQLException exception = assertThrows(SQLException.class,
                () -> ConnectionFactory.getHqcpInstance(invalidJdbc));

        assertEquals("Invalid jdbc properties: " + invalidJdbc, exception.getMessage());
    }

    @Test
    void test_getHqcpInstance_newHqcpInstance_failed_shouldThrowSQLException() throws Exception {
        driver.setMockJDBCAnswer(new MockJDBCAnswer() {
            @Override
            public Connection answer() throws SQLException {
                throw new SQLException("mock sql exception");
            }
        });
        // Act & Assert
        SQLException exception = assertThrows(SQLException.class,
                () -> ConnectionFactory.getHqcpInstance());

        assertEquals("mock sql exception", exception.getMessage());
    }

    @Test
    public void testGetConnection_autoCommit() throws Exception {
        Connection conn = ConnectionFactory.getConnection();
        assertNotNull(conn);
        assertTrue(conn.getAutoCommit());
        conn.close();
        Connection conn2 = ConnectionFactory.getConnection(false);
        assertFalse(conn.getAutoCommit());
        conn2.close();
        assertSame(conn, conn2);
        ConnectionFactory.shutdown();
    }

    @Test
    public void testGetConnection1() throws Exception {
        Locale.setDefault(Locale.CHINA);
        Connection conn = ConnectionFactory.getConnection("jdbc1");
        conn.close();
        Connection conn2 = ConnectionFactory.getConnection("jdbc1", true);
        conn2.close();
        ConnectionFactory.shutdown("jdbc1");
    }

    @Test
    public void testGetConnection2() throws Exception {
        Connection conn = ConnectionFactory.getConnection("jdbc2");
        conn.close();
        Connection conn2 = ConnectionFactory.getConnection("jdbc2", true);
        conn2.close();
        ConnectionFactory.shutdown("jdbc2");
    }

}
