package io.hqwu.commons.cp.util;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.sql.Driver;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.easymock.EasyMock.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * JdbcUtil 测试类
 */
public class JdbcUtilTest {

    // Use a real JDBC driver with public constructor for testing
    private static final String TEST_DRIVER_CLASS = "org.hsqldb.jdbc.JDBCDriver";

    /**
     * Test: createDriver with valid driver class name (line 26)
     */
    @Test
    public void testCreateDriverSuccess() throws SQLException {
        // Skip this test if HSQLDB is not available
        try {
            Class.forName(TEST_DRIVER_CLASS);
        } catch (ClassNotFoundException e) {
            System.out.println("Skipping test - HSQLDB not available");
            return;
        }

        Driver driver = JdbcUtil.createDriver(TEST_DRIVER_CLASS);
        assertNotNull(driver);
    }

    /**
     * Test: createDriver with context ClassLoader (line 29-31)
     */
    @Test
    public void testCreateDriverWithContextClassLoader() throws SQLException {
        // Skip this test if HSQLDB is not available
        try {
            Class.forName(TEST_DRIVER_CLASS);
        } catch (ClassNotFoundException e) {
            System.out.println("Skipping test - HSQLDB not available");
            return;
        }

        ClassLoader originalLoader = Thread.currentThread().getContextClassLoader();
        try {
            // Use current class loader as context loader
            Thread.currentThread().setContextClassLoader(getClass().getClassLoader());

            Driver driver = JdbcUtil.createDriver(TEST_DRIVER_CLASS);
            assertNotNull(driver);
        } finally {
            Thread.currentThread().setContextClassLoader(originalLoader);
        }
    }

    /**
     * Test: createDriver when context ClassLoader is null (line 29)
     */
    @Test
    public void testCreateDriverWithNullContextClassLoader() throws SQLException {
        // Skip this test if HSQLDB is not available
        try {
            Class.forName(TEST_DRIVER_CLASS);
        } catch (ClassNotFoundException e) {
            System.out.println("Skipping test - HSQLDB not available");
            return;
        }

        ClassLoader originalLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(null);

            Driver driver = JdbcUtil.createDriver(TEST_DRIVER_CLASS);
            assertNotNull(driver);
        } finally {
            Thread.currentThread().setContextClassLoader(originalLoader);
        }
    }

    /**
     * Test: createDriver with context ClassLoader that cannot find class (line 33)
     */
    @Test
    public void testCreateDriverContextLoaderClassNotFound() throws SQLException {
        // Skip this test if HSQLDB is not available
        try {
            Class.forName(TEST_DRIVER_CLASS);
        } catch (ClassNotFoundException e) {
            System.out.println("Skipping test - HSQLDB not available");
            return;
        }

        ClassLoader originalLoader = Thread.currentThread().getContextClassLoader();
        try {
            // Use a limited ClassLoader that won't find the class
            ClassLoader limitedLoader = new ClassLoader(null) {
                @Override
                public Class<?> loadClass(String name) throws ClassNotFoundException {
                    throw new ClassNotFoundException("Simulated not found");
                }
            };
            Thread.currentThread().setContextClassLoader(limitedLoader);

            // Should fall back to Class.forName
            Driver driver = JdbcUtil.createDriver(TEST_DRIVER_CLASS);
            assertNotNull(driver);
        } finally {
            Thread.currentThread().setContextClassLoader(originalLoader);
        }
    }

    /**
     * Test: createDriver with non-existent class (line 39-42)
     */
    @Test
    public void testCreateDriverClassNotFound() {
        SQLException exception = assertThrows(SQLException.class, () -> {
            JdbcUtil.createDriver("com.nonexistent.DriverClass");
        });

        assertNotNull(exception.getCause());
        assertTrue(exception.getCause() instanceof ClassNotFoundException);
    }

    /**
     * Test: createDriver with class that cannot be instantiated (line 46-50)
     */
    @Test
    public void testCreateDriverInstantiationError() {
        // Abstract class cannot be instantiated
        SQLException exception = assertThrows(SQLException.class, () -> {
            JdbcUtil.createDriver("java.sql.Driver");
        });

        assertTrue(exception.getCause() instanceof InstantiationException
                || exception.getCause() instanceof NoSuchMethodException
                || exception.getCause() instanceof InvocationTargetException
                || exception.getCause() instanceof IllegalAccessException);
    }

    /**
     * Test: closeQuietly(Closeable) with null (line 54-59)
     */
    @Test
    public void testCloseQuietlyCloseableNull() {
        // Should not throw exception
        assertDoesNotThrow(() -> JdbcUtil.closeQuietly((Closeable) null));
    }

    /**
     * Test: closeQuietly(Closeable) with valid closeable (line 54-59)
     */
    @Test
    public void testCloseQuietlyCloseableSuccess() {
        InputStream stream = new ByteArrayInputStream("test".getBytes());
        assertDoesNotThrow(() -> JdbcUtil.closeQuietly(stream));
    }

    /**
     * Test: closeQuietly(Closeable) with IOException (line 58-59)
     */
    @Test
    public void testCloseQuietlyCloseableWithException() {
        Closeable closeable = () -> {
            throw new IOException("Test exception");
        };

        // Should catch and ignore IOException
        assertDoesNotThrow(() -> JdbcUtil.closeQuietly(closeable));
    }

    /**
     * Test: closeQuietly(ResultSet) with null (line 64-71)
     */
    @Test
    public void testCloseQuietlyResultSetNull() {
        assertDoesNotThrow(() -> JdbcUtil.closeQuietly((ResultSet) null));
    }

    /**
     * Test: closeQuietly(ResultSet) with valid ResultSet (line 64-71)
     */
    @Test
    public void testCloseQuietlyResultSetSuccess() throws SQLException {
        ResultSet rs = createMock(ResultSet.class);
        rs.close();
        expectLastCall().once();
        replay(rs);

        JdbcUtil.closeQuietly(rs);

        verify(rs);
    }

    /**
     * Test: closeQuietly(ResultSet) with SQLException (line 68-71)
     */
    @Test
    public void testCloseQuietlyResultSetWithException() throws SQLException {
        ResultSet rs = createMock(ResultSet.class);
        rs.close();
        expectLastCall().andThrow(new SQLException("Test exception")).once();
        replay(rs);

        // Should catch and log SQLException
        assertDoesNotThrow(() -> JdbcUtil.closeQuietly(rs));

        verify(rs);
    }

    /**
     * Test: closeQuietly(Statement) with null (line 75-82)
     */
    @Test
    public void testCloseQuietlyStatementNull() {
        assertDoesNotThrow(() -> JdbcUtil.closeQuietly((Statement) null));
    }

    /**
     * Test: closeQuietly(Statement) with valid Statement (line 75-82)
     */
    @Test
    public void testCloseQuietlyStatementSuccess() throws SQLException {
        Statement stmt = createMock(Statement.class);
        stmt.close();
        expectLastCall().once();
        replay(stmt);

        JdbcUtil.closeQuietly(stmt);

        verify(stmt);
    }

    /**
     * Test: closeQuietly(Statement) with SQLException (line 79-82)
     */
    @Test
    public void testCloseQuietlyStatementWithException() throws SQLException {
        Statement stmt = createMock(Statement.class);
        stmt.close();
        expectLastCall().andThrow(new SQLException("Test exception")).once();
        replay(stmt);

        // Should catch and log SQLException
        assertDoesNotThrow(() -> JdbcUtil.closeQuietly(stmt));

        verify(stmt);
    }

    /**
     * Test: removeBreakingWhitespace (line 84-91)
     */
    @Test
    public void testRemoveBreakingWhitespace() {
        String input = "SELECT *\nFROM\ttable\r\nWHERE  id=1";
        String result = JdbcUtil.removeBreakingWhitespace(input);

        assertEquals("SELECT * FROM table WHERE id=1 ", result);
    }

    /**
     * Test: removeBreakingWhitespace with single token
     */
    @Test
    public void testRemoveBreakingWhitespaceSingleToken() {
        String input = "test";
        String result = JdbcUtil.removeBreakingWhitespace(input);

        assertEquals("test ", result);
    }

    /**
     * Test: removeBreakingWhitespace with empty string
     */
    @Test
    public void testRemoveBreakingWhitespaceEmpty() {
        String result = JdbcUtil.removeBreakingWhitespace("");
        assertEquals("", result);
    }

    /**
     * Test: multiLinesToOneLine with \r\n (line 93-100)
     */
    @Test
    public void testMultiLinesToOneLineCRLF() {
        String input = "line1\r\nline2\r\nline3";
        String result = JdbcUtil.multiLinesToOneLine(input, " ");

        assertEquals("line1 line2 line3", result);
    }

    /**
     * Test: multiLinesToOneLine with \n
     */
    @Test
    public void testMultiLinesToOneLineLF() {
        String input = "line1\nline2\nline3";
        String result = JdbcUtil.multiLinesToOneLine(input, " ");

        assertEquals("line1 line2 line3", result);
    }

    /**
     * Test: multiLinesToOneLine with \r
     */
    @Test
    public void testMultiLinesToOneLineCR() {
        String input = "line1\rline2\rline3";
        String result = JdbcUtil.multiLinesToOneLine(input, " ");

        assertEquals("line1 line2 line3", result);
    }

    /**
     * Test: multiLinesToOneLine with null replacement (line 94-96)
     */
    @Test
    public void testMultiLinesToOneLineNullReplacement() {
        String input = "line1\nline2\nline3";
        String result = JdbcUtil.multiLinesToOneLine(input, null);

        assertEquals("line1line2line3", result);
    }

    /**
     * Test: multiLinesToOneLine with mixed line endings
     */
    @Test
    public void testMultiLinesToOneLineMixed() {
        String input = "line1\r\nline2\nline3\rline4";
        String result = JdbcUtil.multiLinesToOneLine(input, "|");

        assertEquals("line1|line2|line3|line4", result);
    }

    /**
     * Test: replace basic functionality (line 106)
     */
    @Test
    public void testReplaceBasic() {
        String result = JdbcUtil.replace("hello world", "world", "java");
        assertEquals("hello java", result);
    }

    /**
     * Test: replace with max parameter (line 110-138)
     */
    @Test
    public void testReplaceWithMax() {
        String result = JdbcUtil.replace("a b a b a b", "a", "x", 2);
        assertEquals("x b x b a b", result);
    }

    /**
     * Test: replace with text is null or empty (line 112)
     */
    @Test
    public void testReplaceTextNull() {
        assertNull(JdbcUtil.replace(null, "search", "replacement", 10));
        assertEquals("", JdbcUtil.replace("", "search", "replacement", 10));
    }

    /**
     * Test: replace with searchString is null or empty (line 112)
     */
    @Test
    public void testReplaceSearchStringEmpty() {
        String text = "hello";
        assertEquals(text, JdbcUtil.replace(text, null, "replacement", 10));
        assertEquals(text, JdbcUtil.replace(text, "", "replacement", 10));
    }

    /**
     * Test: replace with replacement is null (line 112)
     */
    @Test
    public void testReplaceReplacementNull() {
        String text = "hello world";
        assertEquals(text, JdbcUtil.replace(text, "world", null, 10));
    }

    /**
     * Test: replace with max is 0 (line 112)
     */
    @Test
    public void testReplaceMaxZero() {
        String text = "hello world";
        assertEquals(text, JdbcUtil.replace(text, "world", "java", 0));
    }

    /**
     * Test: replace when searchString not found (line 117-119)
     */
    @Test
    public void testReplaceNotFound() {
        String text = "hello world";
        assertEquals(text, JdbcUtil.replace(text, "xyz", "abc", 10));
    }

    /**
     * Test: replace with multiple occurrences
     */
    @Test
    public void testReplaceMultiple() {
        String result = JdbcUtil.replace("a a a a", "a", "b", -1);
        assertEquals("b b b b", result);
    }

    /**
     * Test: replace with larger replacement (line 124-128)
     */
    @Test
    public void testReplaceLargerReplacement() {
        String result = JdbcUtil.replace("a a a", "a", "abc", -1);
        assertEquals("abc abc abc", result);
    }

    /**
     * Test: replace with max < 0 (line 125)
     */
    @Test
    public void testReplaceMaxNegative() {
        String result = JdbcUtil.replace("a a a a a", "a", "x", -1);
        assertEquals("x x x x x", result);
    }

    /**
     * Test: replace with max > 64 (line 125)
     */
    @Test
    public void testReplaceMaxLarge() {
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            text.append("a ");
        }
        String result = JdbcUtil.replace(text.toString(), "a", "x", 100);
        assertTrue(result.startsWith("x x x"));
    }

    /**
     * Test: replace with smaller replacement (line 124)
     */
    @Test
    public void testReplaceSmallerReplacement() {
        String result = JdbcUtil.replace("abc abc abc", "abc", "x", -1);
        assertEquals("x x x", result);
    }

    /**
     * Test: replace edge case - substring at start
     */
    @Test
    public void testReplaceAtStart() {
        String result = JdbcUtil.replace("abc def", "abc", "xyz", -1);
        assertEquals("xyz def", result);
    }

    /**
     * Test: replace edge case - substring at end
     */
    @Test
    public void testReplaceAtEnd() {
        String result = JdbcUtil.replace("abc def", "def", "xyz", -1);
        assertEquals("abc xyz", result);
    }

    /**
     * Test: isEmpty with null (line 141)
     */
    @Test
    public void testIsEmptyNull() {
        assertTrue(JdbcUtil.isEmpty(null));
    }

    /**
     * Test: isEmpty with empty string (line 141)
     */
    @Test
    public void testIsEmptyEmptyString() {
        assertTrue(JdbcUtil.isEmpty(""));
    }

    /**
     * Test: isEmpty with non-empty string (line 141)
     */
    @Test
    public void testIsEmptyNonEmpty() {
        assertFalse(JdbcUtil.isEmpty("test"));
    }

    /**
     * Test: isEmpty with whitespace
     */
    @Test
    public void testIsEmptyWhitespace() {
        assertFalse(JdbcUtil.isEmpty(" "));
    }

    /**
     * Test: CRLF constant
     */
    @Test
    public void testCRLF() {
        assertNotNull(JdbcUtil.CRLF);
        assertEquals(System.getProperty("line.separator"), JdbcUtil.CRLF);
    }
}
