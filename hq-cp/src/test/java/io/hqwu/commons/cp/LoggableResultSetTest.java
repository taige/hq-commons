package io.hqwu.commons.cp;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;

import static org.easymock.EasyMock.*;
import static org.junit.jupiter.api.Assertions.*;

class LoggableResultSetTest {

    private PooledStatement mockPooledStatement;
    private ResultSet mockRealResultSet;
    private ResultSetMetaData mockMetaData;

    private static final String TARGET_LOGGER_NAME = "io.hqwu.commons.cp.PooledStatement";
    private Level originalLevel;

    @BeforeEach
    void setUp() {
        // Set logger level to TRACE for testing
        Logger root = (Logger) LoggerFactory.getLogger(TARGET_LOGGER_NAME);
        originalLevel = root.getLevel();
        root.setLevel(Level.TRACE);

        mockPooledStatement = createNiceMock(PooledStatement.class);
        mockRealResultSet = createMock(ResultSet.class);
        mockMetaData = createMock(ResultSetMetaData.class);
    }

    @AfterEach
    void tearDown() {
        // Restore original logger level
        Logger root = (Logger) LoggerFactory.getLogger(TARGET_LOGGER_NAME);
        root.setLevel(originalLevel);
    }

    @Test
    void testNextDelegationAndLogging() throws SQLException {
        // Setup
        expect(mockPooledStatement.isVerbose()).andReturn(true).anyTimes();
        expect(mockPooledStatement.getStatementName()).andReturn("test-stmt").anyTimes();
        expect(mockPooledStatement.isPrintSQL()).andReturn(true).anyTimes();

        expect(mockRealResultSet.next()).andReturn(true);
        expect(mockRealResultSet.getMetaData()).andReturn(mockMetaData);

        // MetaData setup
        expect(mockMetaData.getColumnCount()).andReturn(2).anyTimes();
        expect(mockMetaData.getColumnLabel(1)).andReturn("ID");
        expect(mockMetaData.getColumnType(1)).andReturn(Types.INTEGER).anyTimes();
        expect(mockMetaData.getColumnLabel(2)).andReturn("NAME");
        expect(mockMetaData.getColumnType(2)).andReturn(Types.VARCHAR).anyTimes();

        // Data fetching for logging
        expect(mockRealResultSet.getString(1)).andReturn("100");
        expect(mockRealResultSet.getString(2)).andReturn("Alice");

        // Second call for next() -> false
        expect(mockRealResultSet.next()).andReturn(false);

        replay(mockPooledStatement, mockRealResultSet, mockMetaData);

        // Create Proxy
        PooledStatement.LoggableResultSet lrs = PooledStatement.LoggableResultSet.newInstance(mockPooledStatement, mockRealResultSet);
        assertNotNull(lrs);
        ResultSet proxy = lrs.getResultSet();

        // Execution
        assertTrue(proxy.next(), "First next() should return true");
        assertFalse(proxy.next(), "Second next() should return false");

        verify(mockPooledStatement, mockRealResultSet, mockMetaData);
    }

    @Test
    void testBlobHandling() throws SQLException {
        // Setup
        expect(mockPooledStatement.isVerbose()).andReturn(true).anyTimes();
        expect(mockPooledStatement.getStatementName()).andReturn("test-stmt").anyTimes();

        expect(mockRealResultSet.next()).andReturn(true);
        expect(mockRealResultSet.getMetaData()).andReturn(mockMetaData);

        // MetaData setup with a BLOB
        expect(mockMetaData.getColumnCount()).andReturn(2).anyTimes();
        expect(mockMetaData.getColumnLabel(1)).andReturn("ID");
        expect(mockMetaData.getColumnType(1)).andReturn(Types.INTEGER).anyTimes();
        expect(mockMetaData.getColumnLabel(2)).andReturn("CONTENT");
        expect(mockMetaData.getColumnType(2)).andReturn(Types.BLOB).anyTimes(); // BLOB type

        // Data fetching: Should NOT call getString(2) for BLOB, but will call getString(1)
        expect(mockRealResultSet.getString(1)).andReturn("100");
        // getString(2) should strictly NOT be called if logic is correct.
        // If it were called, EasyMock would complain (missing expectation) or we can strictly forbid it:
        // expect(mockRealResultSet.getString(2)).andThrow(new SQLException("Should not access BLOB as String"));
        // But the code avoids calling it based on type.

        replay(mockPooledStatement, mockRealResultSet, mockMetaData);

        // Create Proxy
        PooledStatement.LoggableResultSet lrs = PooledStatement.LoggableResultSet.newInstance(mockPooledStatement, mockRealResultSet);
        ResultSet proxy = lrs.getResultSet();

        // Execution
        assertTrue(proxy.next());

        verify(mockPooledStatement, mockRealResultSet, mockMetaData);
    }

    @Test
    void testDelegationOfOtherMethods() throws SQLException {
        // Setup
        expect(mockRealResultSet.getString("col1")).andReturn("value1");
        expect(mockRealResultSet.getInt(1)).andReturn(99);
        mockRealResultSet.close();
        expectLastCall();

        replay(mockPooledStatement, mockRealResultSet, mockMetaData);

        // Create Proxy
        PooledStatement.LoggableResultSet lrs = PooledStatement.LoggableResultSet.newInstance(mockPooledStatement, mockRealResultSet);
        ResultSet proxy = lrs.getResultSet();

        // Execution
        assertEquals("value1", proxy.getString("col1"));
        assertEquals(99, proxy.getInt(1));
        proxy.close();

        verify(mockPooledStatement, mockRealResultSet, mockMetaData);
    }

    @Test
    void testExceptionPropagation() throws SQLException {
         // Setup
        expect(mockRealResultSet.next()).andThrow(new SQLException("DB Error"));

        replay(mockPooledStatement, mockRealResultSet, mockMetaData);

        // Create Proxy
        PooledStatement.LoggableResultSet lrs = PooledStatement.LoggableResultSet.newInstance(mockPooledStatement, mockRealResultSet);
        ResultSet proxy = lrs.getResultSet();

        // Execution
        assertThrows(SQLException.class, proxy::next);

        verify(mockPooledStatement, mockRealResultSet, mockMetaData);
    }

    /**
     * Test Wrapper Interface Support
     * Verify that isWrapperFor and unwrap work correctly for ResultSet proxy.
     * The implementation should:
     * 1. Check if the requested interface is implemented by the InvocationHandler (LoggableResultSet)
     * 2. Check if the requested interface is implemented by the Proxy itself
     * 3. Delegate to the underlying resultSet's isWrapperFor/unwrap
     */
    @Test
    void testWrapperSupport() throws SQLException {
        replay(mockPooledStatement, mockRealResultSet, mockMetaData);

        PooledStatement.LoggableResultSet lrs = PooledStatement.LoggableResultSet.newInstance(mockPooledStatement, mockRealResultSet);
        ResultSet proxy = lrs.getResultSet();

        // Test 1: isWrapperFor with ResultSet interface (proxy implements it)
        assertTrue(proxy.isWrapperFor(ResultSet.class),
            "Proxy should be a wrapper for ResultSet interface");

        // Test 2: unwrap with ResultSet interface (should return proxy itself)
        ResultSet unwrappedRs = proxy.unwrap(ResultSet.class);
        assertSame(proxy, unwrappedRs,
            "Unwrapping ResultSet should return the proxy itself");

        // Test 3: isWrapperFor with LoggableResultSet class (handler type)
        // Note: LoggableResultSet is a static inner class
        assertTrue(proxy.isWrapperFor(PooledStatement.LoggableResultSet.class),
            "Proxy should be a wrapper for LoggableResultSet (the handler)");

        // Test 4: unwrap with LoggableResultSet class (should return the handler)
        PooledStatement.LoggableResultSet unwrappedHandler = proxy.unwrap(PooledStatement.LoggableResultSet.class);
        assertSame(lrs, unwrappedHandler,
            "Unwrapping LoggableResultSet should return the handler itself");

        verify(mockPooledStatement, mockRealResultSet, mockMetaData);
    }

    /**
     * Test Wrapper Interface Support - Delegation
     * Verify that when the requested interface is not implemented by handler or proxy,
     * it delegates to the underlying real ResultSet.
     */
    @Test
    void testWrapperSupportDelegation() throws SQLException {
        // Define a vendor-specific interface
        Class<Runnable> vendorInterface = Runnable.class;
        Runnable mockVendorObject = createMock(Runnable.class);

        // Delegation expectations
        expect(mockRealResultSet.isWrapperFor(vendorInterface)).andReturn(true).once();
        expect(mockRealResultSet.unwrap(vendorInterface)).andReturn(mockVendorObject).once();

        replay(mockPooledStatement, mockRealResultSet, mockMetaData, mockVendorObject);

        PooledStatement.LoggableResultSet lrs = PooledStatement.LoggableResultSet.newInstance(mockPooledStatement, mockRealResultSet);
        ResultSet proxy = lrs.getResultSet();

        // Test isWrapperFor delegation
        assertTrue(proxy.isWrapperFor(vendorInterface),
            "Should delegate to underlying ResultSet and return true");

        // Test unwrap delegation
        Runnable unwrapped = proxy.unwrap(vendorInterface);
        assertSame(mockVendorObject, unwrapped,
            "Should delegate to underlying ResultSet and return the vendor object");

        verify(mockPooledStatement, mockRealResultSet, mockMetaData);
    }

    /**
     * Test Wrapper Interface Support - Null Handling
     */
    @Test
    void testWrapperSupportNullHandling() throws SQLException {
        replay(mockPooledStatement, mockRealResultSet, mockMetaData);

        PooledStatement.LoggableResultSet lrs = PooledStatement.LoggableResultSet.newInstance(mockPooledStatement, mockRealResultSet);
        ResultSet proxy = lrs.getResultSet();

        // Test 1: isWrapperFor with null should return false
        assertFalse(proxy.isWrapperFor(null),
            "isWrapperFor(null) should return false");

        // Test 2: unwrap with null should throw exception
        assertThrows(NullPointerException.class, () -> proxy.unwrap(null),
            "unwrap(null) should throw NullPointerException");

        verify(mockPooledStatement, mockRealResultSet, mockMetaData);
    }
}
