package io.hqwu.commons.bean.converters;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Created with IntelliJ IDEA for hq-commons
 * User: taige
 * Date: 2026/1/26
 * Time: 13:35
 */
class Number2StringTest {

    @Test
    void test_valueOf() {
        Number2String number2String = new Number2String();

        // Test with default format (#.#)
        String result = number2String.valueOf(123.456, null, null, null, null);
        assertEquals("123.5", result);

        result = number2String.valueOf(123, null, null, null, null);
        assertEquals("123", result);

        // Test with custom format (#.##)
        result = number2String.valueOf(123.456, null, null, null, null, "#.##");
        assertEquals("123.46", result);

        result = number2String.valueOf(123.4, null, null, null, null, "#.##");
        assertEquals("123.4", result);

        // Test with format (0.00) - always shows two decimal places
        result = number2String.valueOf(123.4, null, null, null, null, "0.00");
        assertEquals("123.40", result);

        result = number2String.valueOf(123, null, null, null, null, "0.00");
        assertEquals("123.00", result);

        // Test with comma separator (#,###.##)
        result = number2String.valueOf(1234567.89, null, null, null, null, "#,###.##");
        assertEquals("1,234,567.89", result);

        result = number2String.valueOf(1234567, null, null, null, null, "#,###");
        assertEquals("1,234,567", result);

        // Test with zero value
        result = number2String.valueOf(0, null, null, null, null);
        assertEquals("0", result);

        result = number2String.valueOf(0.0, null, null, null, null, "0.00");
        assertEquals("0.00", result);

        // Test with negative values
        result = number2String.valueOf(-123.45, null, null, null, null, "#.##");
        assertEquals("-123.45", result);

        result = number2String.valueOf(-1234567.89, null, null, null, null, "#,###.##");
        assertEquals("-1,234,567.89", result);

        // Test with Integer
        result = number2String.valueOf(100, null, null, null, null, "0.00");
        assertEquals("100.00", result);

        // Test with Long
        result = number2String.valueOf(1000000L, null, null, null, null, "#,###");
        assertEquals("1,000,000", result);

        // Test with Float
        result = number2String.valueOf(99.99f, null, null, null, null, "#.##");
        assertEquals("99.99", result);

        // Test with Double
        result = number2String.valueOf(12345.6789, null, null, null, null, "#,###.####");
        assertEquals("12,345.6789", result);

        // Test with very small number
        result = number2String.valueOf(0.0001, null, null, null, null, "0.0000");
        assertEquals("0.0001", result);

        // Test rounding
        result = number2String.valueOf(123.456789, null, null, null, null, "#.##");
        assertEquals("123.46", result);

        result = number2String.valueOf(123.454, null, null, null, null, "#.##");
        assertEquals("123.45", result);

        // Test percentage format
        result = number2String.valueOf(0.95, null, null, null, null, "#.##%");
        assertEquals("95%", result);

        // Test scientific notation
        result = number2String.valueOf(1234567, null, null, null, null, "0.##E0");
        assertEquals("1.23e6", result);
    }
}
