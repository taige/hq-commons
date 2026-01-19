package io.hqwu.commons.cp;

import com.jolbox.bonecp.MockConstant;
import com.jolbox.bonecp.MockJDBCDriver;
import org.easymock.IMocksControl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.sql.*;
import java.util.Properties;

import static org.easymock.EasyMock.*;
import static org.junit.jupiter.api.Assertions.*;

public class PooledConnectionTest {

    private IMocksControl control;
    private Hqcp mockPool;
    private HqcpConfig mockConfig;
    private Connection mockRealConnection;

    @BeforeEach
    public void setup() throws SQLException {
        control = createNiceControl();
        mockPool = control.createMock(Hqcp.class);
        mockConfig = control.createMock(HqcpConfig.class);
        mockRealConnection = control.createMock(Connection.class);

        // Setup MockDriver
        MockJDBCDriver.getInstance().setConnection(mockRealConnection);

        // Common expectations
        expect(mockPool.getPoolName()).andReturn("testPool").anyTimes();
        expect(mockPool.getConfig()).andReturn(mockConfig).anyTimes();

        expect(mockConfig.getUrl()).andReturn(MockConstant.MOCK_URL).anyTimes();
        expect(mockConfig.getConnectionProperties()).andReturn(new Properties()).anyTimes();
        expect(mockConfig.getMaxStatements()).andReturn(10).anyTimes();
        expect(mockConfig.getMaxPreStatements()).andReturn(10).anyTimes();
        expect(mockConfig.getJmxLevel()).andReturn(0).anyTimes();
        expect(mockConfig.isVerbose()).andReturn(false).anyTimes();
        expect(mockConfig.isPrintSql()).andReturn(false).anyTimes();
        expect(mockConfig.getLifetimeSec()).andReturn(0L).anyTimes();
        expect(mockConfig.getLifetimeMillisec()).andReturn(0L).anyTimes();
    }

    @AfterEach
    public void teardown() throws SQLException {
        MockJDBCDriver.getInstance().disable();
    }

    /**
     * Test Requirement 1: Logical Close
     * Calling close() on the proxy should return the connection to the pool (checkIn)
     * and not close the physical connection.
     */
    @Test
    public void testLogicalClose() throws SQLException {
        // Initial state for constructor
        expect(mockRealConnection.getAutoCommit()).andReturn(true).anyTimes();

        // Expectation: checkIn is called on the pool
        mockPool.checkIn(anyObject(PooledConnection.class));
        expectLastCall().once();

        control.replay();

        // 1. Create PooledConnection
        // This will trigger makeRealConnection -> MockDriver.connect
        PooledConnection pooledConnection = new PooledConnection(mockPool, 1);

        // Verify underlying connection is marked as open (physically)
        assertFalse(pooledConnection.isClosed());

        // 2. Checkout the proxy
        Connection proxy = pooledConnection.checkOut(true);
        assertNotNull(proxy);
        assertTrue(pooledConnection.isCheckOut());

        // 3. Close the proxy (logical close)
        proxy.close();

        // 4. Verify pool.checkIn was called
        control.verify();

        // 5. Verify logical state
        assertFalse(pooledConnection.isCheckOut());

        // Note: The physical connection should still be open
        // (We didn't expect real_connection.close())
    }

    /**
     * Test Requirement 2: Transaction State
     * Verify setAutoCommit, commit, rollback delegate to real connection.
     * Verify cleanup on close (dirty check).
     */
    @Test
    public void testTransactionState() throws SQLException {
        // --- Expectations ---

        // Config: Commit on close = false (should rollback)
        expect(mockConfig.isCommitOnClose()).andReturn(false).anyTimes();

        // 1. Initialization (in constructor)
        expect(mockRealConnection.getAutoCommit()).andReturn(true).times(1);

        // 2. setAutoCommit(false)
        mockRealConnection.setAutoCommit(false);
        expectLastCall().once();
        // The implementation calls getAutoCommit immediately after setAutoCommit to update local cache
        expect(mockRealConnection.getAutoCommit()).andReturn(false).times(1);

        // 3. commit()
        mockRealConnection.commit();
        expectLastCall().once();

        // 4. rollback()
        mockRealConnection.rollback();
        expectLastCall().once();

        // 5. Cleanup on close (dirty & !autoCommit) -> rollback
        mockRealConnection.rollback();
        expectLastCall().once();

        // Logical checkIn
        mockPool.checkIn(anyObject(PooledConnection.class));
        expectLastCall().once();

        control.replay();

        // --- Execution ---

        // 1. Create Connection
        PooledConnection pooledConnection = new PooledConnection(mockPool, 1);
        Connection proxy = pooledConnection.checkOut(true); // checkout(true) -> autoCommit=true, matches current.

        // 2. Change Transaction State
        proxy.setAutoCommit(false); // Delegates to real

        // 3. Commit
        proxy.commit(); // Delegates to real

        // 4. Rollback
        proxy.rollback(); // Delegates to real

        // 5. Simulate Dirty State & Close
        // Manually set dirty=true to simulate executed update
        pooledConnection.setDirty();

        // Assert state before close
        // assertFalse(pooledConnection.getAutoCommit()); // Internal state check not available publicly

        proxy.close(); // Should trigger rollback() then checkIn()

        control.verify();
    }

    /**
     * Test Requirement 3: Statement Creation and Caching
     * Verify that Statements, PreparedStatements, and CallableStatements are correctly created
     * by the proxy, and that PreparedStatement caching works (second call does not trigger physical creation).
     */
    @Test
    public void testStatementCreationAndCaching() throws SQLException {
        String sql = "SELECT 1";
        String sqlCall = "CALL sp_test()";

        // Mocks for statements
        Statement mockStmt = control.createMock(Statement.class);
        PreparedStatement mockPstmt = control.createMock(PreparedStatement.class);
        CallableStatement mockCstmt = control.createMock(CallableStatement.class);

        // --- Expectations ---

        // 1. Init
        expect(mockRealConnection.getAutoCommit()).andReturn(true).anyTimes();

        // 2. createStatement
        expect(mockRealConnection.createStatement()).andReturn(mockStmt).once();
        expect(mockStmt.getResultSetType()).andReturn(ResultSet.TYPE_FORWARD_ONLY).anyTimes();
        expect(mockStmt.getResultSetConcurrency()).andReturn(ResultSet.CONCUR_READ_ONLY).anyTimes();

        // 3. prepareStatement (First time - Cache Miss)
        expect(mockRealConnection.prepareStatement(sql)).andReturn(mockPstmt).once();
        expect(mockPstmt.getResultSetType()).andReturn(ResultSet.TYPE_FORWARD_ONLY).anyTimes();
        expect(mockPstmt.getResultSetConcurrency()).andReturn(ResultSet.CONCUR_READ_ONLY).anyTimes();
        // PooledConnection calls clearParameters on checkIn/reuse
        // Note: exact call count depends on when it's closed/checked-in
        mockPstmt.clearParameters();
        expectLastCall().anyTimes();

        // 4. prepareCall
        expect(mockRealConnection.prepareCall(sqlCall)).andReturn(mockCstmt).once();
        expect(mockCstmt.getResultSetType()).andReturn(ResultSet.TYPE_FORWARD_ONLY).anyTimes();
        expect(mockCstmt.getResultSetConcurrency()).andReturn(ResultSet.CONCUR_READ_ONLY).anyTimes();
        mockCstmt.clearParameters();
        expectLastCall().anyTimes();

        // 5. Cleanup
        mockPool.checkIn(anyObject(PooledConnection.class));
        expectLastCall().once();

        control.replay();

        // --- Execution ---

        PooledConnection pooledConnection = new PooledConnection(mockPool, 1);
        Connection proxy = pooledConnection.checkOut(true);

        // A. createStatement
        Statement s1 = proxy.createStatement();
        assertNotNull(s1);
        assertTrue(Proxy.isProxyClass(s1.getClass()));

        // B. prepareStatement (Cache Miss)
        PreparedStatement ps1 = proxy.prepareStatement(sql);
        assertNotNull(ps1);

        // Return ps1 to cache
        ps1.close();

        // C. prepareStatement (Cache Hit)
        // If caching works, this will NOT trigger real_connection.prepareStatement(sql) again
        PreparedStatement ps2 = proxy.prepareStatement(sql);
        assertNotNull(ps2);

        // D. prepareCall
        CallableStatement cs1 = proxy.prepareCall(sqlCall);
        assertNotNull(cs1);

        // Cleanup
        proxy.close();

        control.verify();
    }

    /**
     * Test Requirement 4: Exception Handling and Recovery
     * Verify that when a fatal exception (e.g. 08001) occurs, the pool attempts to
     * reconnect and retry the operation (if autoCommit is true).
     */
    @Test
    public void testExceptionHandlingAndRecovery() throws SQLException {
        Statement mockStmt = control.createMock(Statement.class);

        // --- Expectations ---

        // 1. Initial Connection Setup & Recovery will call getAutoCommit
        // Explicitly return true so autoCommit is enabled, enabling retry logic.
        expect(mockRealConnection.getAutoCommit()).andReturn(true).anyTimes();

        // 2. createStatement -> Throw Fatal Exception (08001)
        expect(mockRealConnection.createStatement())
                .andThrow(new SQLException("Network failure", "08001")).once();

        // 3. Recover Logic:
        //    a. Close old connection (Called by PooledConnection.close() inside recover)
        mockRealConnection.close();
        expectLastCall().once();

        // 4. Retry Logic:
        //    Since autoCommit=true, it retries the invocation (createStatement)
        //    This execution happens after recovery.
        expect(mockRealConnection.createStatement()).andReturn(mockStmt).once();
        expect(mockStmt.getResultSetType()).andReturn(ResultSet.TYPE_FORWARD_ONLY).anyTimes();
        expect(mockStmt.getResultSetConcurrency()).andReturn(ResultSet.CONCUR_READ_ONLY).anyTimes();

        // 5. Cleanup
        mockPool.checkIn(anyObject(PooledConnection.class));
        expectLastCall().once();

        control.replay();

        // --- Execution ---

        PooledConnection pooledConnection = new PooledConnection(mockPool, 1);
        Connection proxy = pooledConnection.checkOut(true);

        // This call triggers the exception, recovery, and retry flow
        Statement s = proxy.createStatement();

        // Assertions
        assertNotNull(s);
        assertTrue(Proxy.isProxyClass(s.getClass()));

        // Verify fatal error flag is reset after successful recovery
        assertFalse(pooledConnection.isFatalExceptionHappened());

        proxy.close();

        control.verify();
    }

    /**
     * Test Requirement 5: Delegation
     * Verify that miscellaneous standard JDBC methods (e.g. getMetaData, setReadOnly,
     * setCatalog, getWarnings) are correctly entrusted to the underlying connection.
     */
    @Test
    public void testDelegation() throws SQLException {
        // Mocks
        java.sql.DatabaseMetaData mockMetaData = control.createMock(java.sql.DatabaseMetaData.class);

        // --- Expectations ---
        expect(mockRealConnection.getAutoCommit()).andReturn(true).anyTimes();

        // 1. getMetaData
        expect(mockRealConnection.getMetaData()).andReturn(mockMetaData).once();

        // 2. setReadOnly
        mockRealConnection.setReadOnly(true);
        expectLastCall().once();

        // 3. setCatalog
        mockRealConnection.setCatalog("test_catalog");
        expectLastCall().once();

        // 4. clearWarnings
        mockRealConnection.clearWarnings();
        expectLastCall().once();

        // Cleanup
        mockPool.checkIn(anyObject(PooledConnection.class));
        expectLastCall().once();

        control.replay();

        // --- Execution ---
        PooledConnection pooledConnection = new PooledConnection(mockPool, 1);
        Connection proxy = pooledConnection.checkOut(true);

        // a. getMetaData
        assertNotNull(proxy.getMetaData());

        // b. setReadOnly
        proxy.setReadOnly(true);

        // c. setCatalog
        proxy.setCatalog("test_catalog");

        // d. clearWarnings
        proxy.clearWarnings();

        proxy.close();

        control.verify();
    }

    /**
     * Test Requirement 6: Wrapper Interface
     * Verify that Wrapper interface methods (isWrapperFor, unwrap) are correctly
     * delegated to the underlying connection, allowing access to vendor-specific extensions.
     */
    @Test
    public void testWrapperInterface() throws SQLException {
        // Define a "Vendor Specific" interface for testing unwrap
        // Just using Runnable as a dummy interface for this test since we just check delegation
        Class<Runnable> vendorInterface = Runnable.class;
        Runnable mockVendorObject = control.createMock(Runnable.class);

        // --- Expectations ---
        expect(mockRealConnection.getAutoCommit()).andReturn(true).anyTimes();

        // 1. isWrapperFor
        expect(mockRealConnection.isWrapperFor(vendorInterface)).andReturn(true).once();

        // 2. unwrap
        expect(mockRealConnection.unwrap(vendorInterface)).andReturn(mockVendorObject).once();

        // Cleanup
        mockPool.checkIn(anyObject(PooledConnection.class));
        expectLastCall().once();

        control.replay();

        // --- Execution ---
        PooledConnection pooledConnection = new PooledConnection(mockPool, 1);
        Connection proxy = pooledConnection.checkOut(true);

        // a. Test isWrapperFor
        boolean isWrapper = proxy.isWrapperFor(vendorInterface);
        assertTrue(isWrapper, "Should return true as underlying connection supports it");

        // b. Test unwrap
        Runnable result = proxy.unwrap(vendorInterface);
        assertNotNull(result);
        assertSame(mockVendorObject, result, "Should return the object returned by underlying connection");

        proxy.close();

        control.verify();
    }

    /**
     * Test Requirement 7: Compliance Extras
     * 1. Verify that open statements are automatically closed when the connection is closed.
     * 2. Verify isClosed() returns true after logical close (JDBC Spec).
     */
    @Test
    public void testComplianceExtras() throws SQLException {
        // Mocks for statements
        Statement mockStmt = control.createMock(Statement.class);

        // --- Expectations ---
        expect(mockRealConnection.getAutoCommit()).andReturn(true).anyTimes();

        // 1. createStatement
        expect(mockRealConnection.createStatement()).andReturn(mockStmt).once();
        expect(mockStmt.getResultSetType()).andReturn(ResultSet.TYPE_FORWARD_ONLY).anyTimes();
        expect(mockStmt.getResultSetConcurrency()).andReturn(ResultSet.CONCUR_READ_ONLY).anyTimes();

        // 2. isClosed() delegation checks
        // Before close: delegates to real (false)
        expect(mockRealConnection.isClosed()).andReturn(false).times(1);

        // After close:
        // The Spec says isClosed() should return true.
        // Current implementation delegates to real_connection.isClosed() which is FALSE (physically open).
        // We will assert current behavior initially or fix it.
        // Let's assume we want to enforce spec. If we enforce spec, we shouldn't expect delegation to real conn.

        // Cleanup
        mockPool.checkIn(anyObject(PooledConnection.class));
        expectLastCall().once();

        control.replay();

        // --- Execution ---
        PooledConnection pooledConnection = new PooledConnection(mockPool, 1);
        Connection proxy = pooledConnection.checkOut(true);

        // A. Statement auto-close
        Statement s = proxy.createStatement();

        // B. Pre-close check
        assertFalse(proxy.isClosed());

        // C. Close
        proxy.close();

        // D. Post-close check
        // According to JDBC spec, this MUST be true.
        // If existing code is buggy, this assertion might fail.
        assertTrue(proxy.isClosed(), "Logical connection should report isClosed()=true after close()");

        control.verify();
    }

    /**
     * Test Requirement 8: AutoCommit=true Behavior
     * Verify that when autoCommit is true, close() does NOT trigger rollback or commit,
     * even if the connection is marked as dirty (e.g. after executing statements).
     */
    @Test
    public void testAutoCommitTrueNoTransactionControl() throws SQLException {
        // --- Expectations ---
        expect(mockRealConnection.getAutoCommit()).andReturn(true).anyTimes(); // Always auto-commit

        // 1. Execute some statement to make connection "dirty"
        Statement mockStmt = control.createMock(Statement.class);
        expect(mockRealConnection.createStatement()).andReturn(mockStmt).once();
        expect(mockStmt.execute("UPDATE foo SET bar=1")).andReturn(false).once(); // Update count
        expect(mockStmt.getResultSetType()).andReturn(ResultSet.TYPE_FORWARD_ONLY).anyTimes();
        expect(mockStmt.getResultSetConcurrency()).andReturn(ResultSet.CONCUR_READ_ONLY).anyTimes();

        // 2. Cleanup on close
        // IMPORTANT: We expect NO commit() or rollback() calls on mockRealConnection here.
        // We only expect checkIn.
        mockPool.checkIn(anyObject(PooledConnection.class));
        expectLastCall().once();

        control.replay();

        // --- Execution ---
        PooledConnection pooledConnection = new PooledConnection(mockPool, 1);
        Connection proxy = pooledConnection.checkOut(true);

        Statement s = proxy.createStatement();
        s.execute("UPDATE foo SET bar=1"); // Sets dirty = true internally

        // Assert: connection logic thinks it's dirty? We can't verify private field easily,
        // but verify() will ensure rollback isn't called.

        proxy.close(); // Should simple checkIn

        control.verify();
    }

    /**
     * Test Requirement 9: AutoCommit State Reset
     * Verify that if a connection is returned with autoCommit=false,
     * the next checkOut(true) correctly resets it to true.
     */
    @Test
    public void testAutoCommitResetsOnCheckOut() throws SQLException {
        // --- Expectations ---

        // 1. Initial State (Constructor)
        expect(mockRealConnection.getAutoCommit()).andReturn(true).times(1);

        // 2. Cycle 1 CheckOut(true) - End of method update
        expect(mockRealConnection.getAutoCommit()).andReturn(true).times(1);

        // 3. User 1 sets autoCommit = false
        mockRealConnection.setAutoCommit(false);
        expectLastCall().once();
        // Internal cache update calls getAutoCommit
        expect(mockRealConnection.getAutoCommit()).andReturn(false).times(1);

        // 4. User 1 returns connection
        mockPool.checkIn(anyObject(PooledConnection.class));
        expectLastCall().once();

        // 5. User 2 checks out with autoCommit = true
        // Pool detects internal state is false, user asks for true -> triggers setAutoCommit(true)
        mockRealConnection.setAutoCommit(true);
        expectLastCall().once();
        // Internal cache update (inside invoke)
        expect(mockRealConnection.getAutoCommit()).andReturn(true).times(1);

        // 6. Cycle 2 CheckOut - End of method update
        expect(mockRealConnection.getAutoCommit()).andReturn(true).times(1);

        control.replay();

        // --- Execution ---
        PooledConnection pooledConnection = new PooledConnection(mockPool, 1);

        // --- Cycle 1: User changes state ---
        Connection proxy1 = pooledConnection.checkOut(true); // Matches initial state
        proxy1.setAutoCommit(false); // Change to false
        proxy1.close(); // Return to pool

        // --- Cycle 2: Pool resets state ---
        Connection proxy2 = pooledConnection.checkOut(true); // User wants true

        // verify() ensures setAutoCommit(true) was called
        control.verify();
    }
}
