package io.hqwu.commons.util;

import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.UndeclaredThrowableException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test cases for ExceptionUtil
 */
public class ExceptionUtilTest {

    // ==================== Test cases for unwrapThrowable ====================

    @Test
    public void testUnwrapThrowableWithNull() {
        // Test with null - should return null
        Throwable result = ExceptionUtil.unwrapThrowable(null);
        assertNull(result, "Unwrapping null should return null");
    }

    @Test
    public void testUnwrapThrowableWithSimpleException() {
        // Test with a simple exception that doesn't need unwrapping
        Exception simple = new RuntimeException("Simple exception");

        Throwable result = ExceptionUtil.unwrapThrowable(simple);

        assertSame(simple, result, "Simple exception should be returned as-is");
    }

    @Test
    public void testUnwrapThrowableWithInvocationTargetException() {
        // Test unwrapping InvocationTargetException
        RuntimeException cause = new RuntimeException("Root cause");
        InvocationTargetException wrapped = new InvocationTargetException(cause);

        Throwable result = ExceptionUtil.unwrapThrowable(wrapped);

        assertSame(cause, result, "Should unwrap to the target exception");
    }

    @Test
    public void testUnwrapThrowableWithUndeclaredThrowableException() {
        // Test unwrapping UndeclaredThrowableException
        RuntimeException cause = new RuntimeException("Root cause");
        UndeclaredThrowableException wrapped = new UndeclaredThrowableException(cause);

        Throwable result = ExceptionUtil.unwrapThrowable(wrapped);

        assertSame(cause, result, "Should unwrap to the undeclared throwable");
    }

    @Test
    public void testUnwrapThrowableWithNestedInvocationTargetExceptions() {
        // Test unwrapping multiple levels of InvocationTargetException
        RuntimeException rootCause = new RuntimeException("Root cause");
        InvocationTargetException level1 = new InvocationTargetException(rootCause);
        InvocationTargetException level2 = new InvocationTargetException(level1);
        InvocationTargetException level3 = new InvocationTargetException(level2);

        Throwable result = ExceptionUtil.unwrapThrowable(level3);

        assertSame(rootCause, result, "Should unwrap all nested InvocationTargetExceptions");
    }

    @Test
    public void testUnwrapThrowableWithNestedUndeclaredThrowableExceptions() {
        // Test unwrapping multiple levels of UndeclaredThrowableException
        RuntimeException rootCause = new RuntimeException("Root cause");
        UndeclaredThrowableException level1 = new UndeclaredThrowableException(rootCause);
        UndeclaredThrowableException level2 = new UndeclaredThrowableException(level1);
        UndeclaredThrowableException level3 = new UndeclaredThrowableException(level2);

        Throwable result = ExceptionUtil.unwrapThrowable(level3);

        assertSame(rootCause, result, "Should unwrap all nested UndeclaredThrowableExceptions");
    }

    @Test
    public void testUnwrapThrowableWithMixedNestedExceptions() {
        // Test unwrapping mixed InvocationTargetException and UndeclaredThrowableException
        RuntimeException rootCause = new RuntimeException("Root cause");
        InvocationTargetException level1 = new InvocationTargetException(rootCause);
        UndeclaredThrowableException level2 = new UndeclaredThrowableException(level1);
        InvocationTargetException level3 = new InvocationTargetException(level2);
        UndeclaredThrowableException level4 = new UndeclaredThrowableException(level3);

        Throwable result = ExceptionUtil.unwrapThrowable(level4);

        assertSame(rootCause, result, "Should unwrap all nested exceptions regardless of type");
    }

    @Test
    public void testUnwrapThrowablePreservesExceptionMessage() {
        // Test that the original exception message is preserved
        String expectedMessage = "Original error message";
        RuntimeException rootCause = new RuntimeException(expectedMessage);
        InvocationTargetException wrapped = new InvocationTargetException(rootCause);

        Throwable result = ExceptionUtil.unwrapThrowable(wrapped);

        assertEquals(expectedMessage, result.getMessage(), "Exception message should be preserved");
    }

    @Test
    public void testUnwrapThrowableWithIOException() {
        // Test with a checked exception
        java.io.IOException cause = new java.io.IOException("IO error");
        InvocationTargetException wrapped = new InvocationTargetException(cause);

        Throwable result = ExceptionUtil.unwrapThrowable(wrapped);

        assertSame(cause, result, "Should unwrap to IOException");
        assertTrue(result instanceof java.io.IOException);
    }

    @Test
    public void testUnwrapThrowableWithError() {
        // Test with an Error
        OutOfMemoryError error = new OutOfMemoryError("Out of memory");
        InvocationTargetException wrapped = new InvocationTargetException(error);

        Throwable result = ExceptionUtil.unwrapThrowable(wrapped);

        assertSame(error, result, "Should unwrap to Error");
        assertTrue(result instanceof OutOfMemoryError);
    }

    @Test
    public void testUnwrapThrowableWithNullCause() {
        // Test InvocationTargetException with null cause
        InvocationTargetException wrapped = new InvocationTargetException(null);

        Throwable result = ExceptionUtil.unwrapThrowable(wrapped);

        assertNull(result, "Should return null when wrapped exception has null cause");
    }

    @Test
    public void testUnwrapThrowableWithUndeclaredThrowableNullCause() {
        // Test UndeclaredThrowableException with null cause
        UndeclaredThrowableException wrapped = new UndeclaredThrowableException(null);

        Throwable result = ExceptionUtil.unwrapThrowable(wrapped);

        assertNull(result, "Should return null when wrapped exception has null cause");
    }

    @Test
    public void testUnwrapThrowableDeepNesting() {
        // Test with deep nesting (10 levels)
        RuntimeException rootCause = new RuntimeException("Deep root cause");
        Throwable current = rootCause;

        for (int i = 0; i < 10; i++) {
            if (i % 2 == 0) {
                current = new InvocationTargetException(current);
            } else {
                current = new UndeclaredThrowableException(current);
            }
        }

        Throwable result = ExceptionUtil.unwrapThrowable(current);

        assertSame(rootCause, result, "Should unwrap deeply nested exceptions");
    }

    @Test
    public void testUnwrapThrowableWithCustomException() {
        // Test with custom exception
        CustomException custom = new CustomException("Custom error");
        InvocationTargetException wrapped = new InvocationTargetException(custom);

        Throwable result = ExceptionUtil.unwrapThrowable(wrapped);

        assertSame(custom, result, "Should unwrap to custom exception");
        assertTrue(result instanceof CustomException);
    }

    @Test
    public void testUnwrapThrowableElseBranch() {
        // Test the else branch - exception that is neither InvocationTargetException
        // nor UndeclaredThrowableException
        IllegalArgumentException exception = new IllegalArgumentException("Invalid argument");

        Throwable result = ExceptionUtil.unwrapThrowable(exception);

        assertSame(exception, result, "Should return the exception as-is (else branch)");
    }

    @Test
    public void testUnwrapThrowableWithNullPointerException() {
        // Test with NullPointerException
        NullPointerException npe = new NullPointerException("Null pointer");
        InvocationTargetException wrapped = new InvocationTargetException(npe);

        Throwable result = ExceptionUtil.unwrapThrowable(wrapped);

        assertSame(npe, result);
        assertTrue(result instanceof NullPointerException);
    }

    @Test
    public void testUnwrapThrowableMultipleCallsConsistency() {
        // Test that multiple calls with same input produce same result
        RuntimeException cause = new RuntimeException("Test");
        InvocationTargetException wrapped = new InvocationTargetException(cause);

        Throwable result1 = ExceptionUtil.unwrapThrowable(wrapped);
        Throwable result2 = ExceptionUtil.unwrapThrowable(wrapped);

        assertSame(result1, result2, "Multiple calls should return same result");
    }

    @Test
    public void testUnwrapThrowableWithStackTrace() {
        // Test that stack trace is preserved
        RuntimeException cause = new RuntimeException("Error with stack trace");
        cause.fillInStackTrace();
        InvocationTargetException wrapped = new InvocationTargetException(cause);

        Throwable result = ExceptionUtil.unwrapThrowable(wrapped);

        assertNotNull(result.getStackTrace(), "Stack trace should be preserved");
        assertTrue(result.getStackTrace().length > 0, "Stack trace should not be empty");
    }

    @Test
    public void testUnwrapThrowableRealWorldScenario() {
        // Simulate a real-world scenario: reflection call failure
        try {
            // This would typically happen in reflection scenarios
            throw new InvocationTargetException(
                new UndeclaredThrowableException(
                    new IllegalStateException("Invalid state in business logic")
                )
            );
        } catch (Exception e) {
            Throwable unwrapped = ExceptionUtil.unwrapThrowable(e);

            assertTrue(unwrapped instanceof IllegalStateException);
            assertEquals("Invalid state in business logic", unwrapped.getMessage());
        }
    }

    // ==================== Helper Classes ====================

    /**
     * Custom exception for testing
     */
    private static class CustomException extends Exception {
        public CustomException(String message) {
            super(message);
        }
    }
}
