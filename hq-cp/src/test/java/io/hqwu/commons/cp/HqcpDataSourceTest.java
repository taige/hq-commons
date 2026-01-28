package io.hqwu.commons.cp;

import com.jolbox.bonecp.MockConstant;
import com.jolbox.bonecp.MockJDBCDriver;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.naming.Reference;
import javax.naming.StringRefAddr;
import javax.sql.DataSource;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Hashtable;

import static org.junit.jupiter.api.Assertions.*;

/**
 * HqcpDataSource 测试类
 */
public class HqcpDataSourceTest {

    private static HqcpDataSource dataSource;

    @BeforeAll
    public static void setUp() {
        if (dataSource == null) {
            MockJDBCDriver.getInstance();
            dataSource = new HqcpDataSource();
            dataSource.setUrl(MockConstant.MOCK_URL);
            dataSource.setDriverClassName(MockJDBCDriver.class.getName());
            dataSource.setUsername("testuser");
            dataSource.setPassword("testpassword");
            dataSource.setMinConnections(0);
            dataSource.setLazyInit(true);
        }
    }

    @AfterAll
    public static void tearDown() {
        if (dataSource != null) {
            dataSource.shutdown();
        }
    }

    /**
     * Test: getLogWriter and setLogWriter (lines 30, 34)
     */
    @Test
    public void testLogWriter() throws SQLException {
        PrintWriter writer = new PrintWriter(new StringWriter());
        dataSource.setLogWriter(writer);

        PrintWriter result = dataSource.getLogWriter();
        assertSame(writer, result);
    }

    /**
     * Test: getParentLogger (line 39)
     */
    @Test
    public void testGetParentLogger() {
        assertThrows(SQLFeatureNotSupportedException.class, () -> {
            dataSource.getParentLogger();
        });
    }

    /**
     * Test: unwrap - unwrap to HqcpDataSource itself (line 55)
     */
    @Test
    public void testUnwrapToSelf() throws SQLException {
        HqcpDataSource unwrapped = dataSource.unwrap(HqcpDataSource.class);
        assertSame(dataSource, unwrapped);
    }

    /**
     * Test: unwrap - unwrap to DataSource (line 55)
     */
    @Test
    public void testUnwrapToDataSource() throws SQLException {
        DataSource unwrapped = dataSource.unwrap(DataSource.class);
        assertSame(dataSource, unwrapped);
    }

    /**
     * Test: unwrap - unwrap to pool (line 59)
     */
    @Test
    public void testUnwrapToPool() throws SQLException {
        // Initialize pool first
        Connection conn = dataSource.getConnection();
        conn.close();

        Hqcp pool = dataSource.unwrap(Hqcp.class);
        assertNotNull(pool);
    }

    /**
     * Test: unwrap - unsupported class (line 62)
     */
    @Test
    public void testUnwrapUnsupportedClass() {
        SQLException exception = assertThrows(SQLException.class, () -> {
            dataSource.unwrap(String.class);
        });
        assertTrue(exception.getMessage().contains("Cannot unwrap to"));
    }

    /**
     * Test: isWrapperFor - true for HqcpDataSource (line 67)
     */
    @Test
    public void testIsWrapperForSelf() throws SQLException {
        assertTrue(dataSource.isWrapperFor(HqcpDataSource.class));
        assertTrue(dataSource.isWrapperFor(DataSource.class));
    }

    /**
     * Test: isWrapperFor - true for pool (line 67)
     */
    @Test
    public void testIsWrapperForPool() throws SQLException {
        // Initialize pool first
        Connection conn = dataSource.getConnection();
        conn.close();

        assertTrue(dataSource.isWrapperFor(Hqcp.class));
    }

    /**
     * Test: isWrapperFor - false for unsupported class (line 67)
     */
    @Test
    public void testIsWrapperForUnsupportedClass() throws SQLException {
        assertFalse(dataSource.isWrapperFor(String.class));
    }

    /**
     * Test: getObjectInstance (lines 72-86)
     */
    @Test
    public void testGetObjectInstance() throws Exception {
        Reference ref = new Reference(HqcpDataSource.class.getName());
        ref.add(new StringRefAddr("url", MockConstant.MOCK_URL));
        ref.add(new StringRefAddr("driverClassName", MockJDBCDriver.class.getName()));
        ref.add(new StringRefAddr("username", "testuser"));
        ref.add(new StringRefAddr("password", "testpassword"));
        ref.add(new StringRefAddr("minConnections", "0"));

        HqcpDataSource factory = new HqcpDataSource();
        Object result = factory.getObjectInstance(ref, null, null, new Hashtable<>());

        assertNotNull(result);
        assertTrue(result instanceof HqcpDataSource);

        HqcpDataSource ds = (HqcpDataSource) result;
        // The properties are set via setProperties which loads from Properties
        // We can at least verify it's a HqcpDataSource
        assertNotNull(ds);
    }

    /**
     * Test: getObjectInstance with driver property (line 76)
     */
    @Test
    public void testGetObjectInstanceWithDriver() throws Exception {
        Reference ref = new Reference(HqcpDataSource.class.getName());
        ref.add(new StringRefAddr("url", MockConstant.MOCK_URL));
        ref.add(new StringRefAddr("driver", MockJDBCDriver.class.getName()));
        ref.add(new StringRefAddr("username", "testuser"));

        HqcpDataSource factory = new HqcpDataSource();
        Object result = factory.getObjectInstance(ref, null, null, new Hashtable<>());

        assertNotNull(result);
        assertTrue(result instanceof HqcpDataSource);
    }

    /**
     * Test: getConnection - lazy initialization (line 97)
     */
    @Test
    public void testGetConnectionLazyInit() throws SQLException {
        Connection conn = dataSource.getConnection();
        assertNotNull(conn);
        conn.close();
    }

    /**
     * Test: getConnection with username and password (line 102)
     */
    @Test
    public void testGetConnectionWithCredentials() {
        assertThrows(UnsupportedOperationException.class, () -> {
            dataSource.getConnection("user", "pass");
        });
    }

    /**
     * Test: init when initOnStartup is true (line 108-109)
     */
    @Test
    public void testInitWhenInitOnStartupTrue() throws SQLException {
        HqcpDataSource ds = new HqcpDataSource();
        ds.setUrl(MockConstant.MOCK_URL);
        ds.setDriverClassName(MockJDBCDriver.class.getName());
        ds.setMinConnections(0);
        ds.setLazyInit(true);
        ds.setInitOnStartup(true);

        // Init should create the pool
        ds.init();

        // Should be able to get connection
        Connection conn = ds.getConnection();
        assertNotNull(conn);
        conn.close();
        ds.shutdown();
    }

    /**
     * Test: init when initOnStartup is false (line 108)
     */
    @Test
    public void testInitWhenInitOnStartupFalse() throws SQLException {
        dataSource.setInitOnStartup(false);

        // Init should do nothing
        dataSource.init();

        // Pool should still be null until getConnection is called
        // We can't directly check pool field, but getConnection will init it
    }

    /**
     * Test: init when pool already exists (line 109)
     */
    @Test
    public void testInitWhenPoolAlreadyExists() throws SQLException {
        dataSource.setInitOnStartup(true);

        // First init
        dataSource.init();
        Connection conn = dataSource.getConnection();
        conn.close();

        // Second init should do nothing
        dataSource.init();
    }

    /**
     * Test: shutdown (line 114-115)
     */
    @Test
    public void testShutdown() throws SQLException {
        // Initialize pool
        Connection conn = dataSource.getConnection();
        conn.close();

        // Shutdown
        dataSource.shutdown();

        // After shutdown, getConnection will reinitialize the pool
        // So we just verify shutdown doesn't throw exception
        assertDoesNotThrow(() -> dataSource.shutdown());
    }

    /**
     * Test: shutdown when pool is null (line 114)
     */
    @Test
    public void testShutdownWhenPoolNull() {
        HqcpDataSource ds = new HqcpDataSource();

        // Should not throw exception
        ds.shutdown();
    }

    /**
     * Test: setInitOnStartup
     */
    @Test
    public void testSetInitOnStartup() {
        dataSource.setInitOnStartup(true);
        // No exception should be thrown
    }

    /**
     * Test: maybeInit - double-checked locking (concurrent init)
     */
    @Test
    public void testMaybeInitConcurrent() throws Exception {
        HqcpDataSource ds = new HqcpDataSource();
        ds.setUrl(MockConstant.MOCK_URL);
        ds.setDriverClassName(MockJDBCDriver.class.getName());
        ds.setMinConnections(0);
        ds.setLazyInit(true);

        // Try to initialize from multiple threads simultaneously
        Thread t1 = new Thread(() -> {
            try {
                ds.getConnection().close();
            } catch (SQLException e) {
                fail("Thread 1 failed: " + e.getMessage());
            }
        });

        Thread t2 = new Thread(() -> {
            try {
                ds.getConnection().close();
            } catch (SQLException e) {
                fail("Thread 2 failed: " + e.getMessage());
            }
        });

        t1.start();
        t2.start();
        t1.join();
        t2.join();

        // Should have initialized only one pool
        Connection conn = ds.getConnection();
        assertNotNull(conn);
        conn.close();
        ds.shutdown();
    }

    /**
     * Test: maybeInit - read lock then write lock upgrade
     */
    @Test
    public void testMaybeInitLockUpgrade() throws SQLException {
        // This tests the double-checked locking pattern in maybeInit
        Connection conn1 = dataSource.getConnection();
        assertNotNull(conn1);

        Connection conn2 = dataSource.getConnection();
        assertNotNull(conn2);

        conn1.close();
        conn2.close();
    }

    /**
     * Test: getConnection after shutdown and reinit
     */
    @Test
    public void testGetConnectionAfterShutdownAndReinit() throws SQLException {
        // First connection
        Connection conn1 = dataSource.getConnection();
        conn1.close();

        // Shutdown
        dataSource.shutdown();

        // Get connection again - should reinitialize
        Connection conn2 = dataSource.getConnection();
        assertNotNull(conn2);
        conn2.close();
    }

    /**
     * Test: unwrap when pool is null
     */
    @Test
    public void testUnwrapWhenPoolNull() throws SQLException {
        HqcpDataSource ds = new HqcpDataSource();

        // Unwrap to self should work
        assertSame(ds, ds.unwrap(HqcpDataSource.class));

        // Unwrap to pool should fail
        SQLException exception = assertThrows(SQLException.class, () -> {
            ds.unwrap(Hqcp.class);
        });
        assertTrue(exception.getMessage().contains("Cannot unwrap"));
    }

    /**
     * Test: isWrapperFor when pool is null
     */
    @Test
    public void testIsWrapperForWhenPoolNull() throws SQLException {
        HqcpDataSource ds = new HqcpDataSource();

        // Should be wrapper for self
        assertTrue(ds.isWrapperFor(HqcpDataSource.class));

        // Should not be wrapper for pool when pool is null
        assertFalse(ds.isWrapperFor(Hqcp.class));
    }
}
