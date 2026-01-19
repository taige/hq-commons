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
}
