package io.hqwu.commons.bean.converters;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Created with IntelliJ IDEA for pp-gopay-fa
 * User: taige
 * Date: 2020/5/9
 * Time: 23:53
 */
class Double2BigDecimalTest {

    @Test
    void test_valueOf() {
        Double2BigDecimal double2BigDecimal = new Double2BigDecimal();

        // Test basic conversion
        BigDecimal result = double2BigDecimal.valueOf(123.45, null, null, null, null);
        assertNotNull(result);
        assertEquals(new BigDecimal("123.45"), result);
        assertEquals(123.45, result.doubleValue());

        // Test zero value
        result = double2BigDecimal.valueOf(0.0, null, null, null, null);
        assertNotNull(result);
        assertEquals(new BigDecimal("0.0"), result);
        assertEquals(0.0, result.doubleValue());

        // Test negative value
        result = double2BigDecimal.valueOf(-456.78, null, null, null, null);
        assertNotNull(result);
        assertEquals(new BigDecimal("-456.78"), result);
        assertEquals(-456.78, result.doubleValue());

        // Test very small value
        result = double2BigDecimal.valueOf(0.0001, null, null, null, null);
        assertNotNull(result);
        assertEquals(0.0001, result.doubleValue(), 0.000001);

        // Test very large value
        result = double2BigDecimal.valueOf(999999.999999, null, null, null, null);
        assertNotNull(result);
        assertEquals(999999.999999, result.doubleValue(), 0.000001);

        // Test with params (params are not used in this converter, but should still work)
        result = double2BigDecimal.valueOf(100.50, null, null, null, null, "param1", "param2");
        assertNotNull(result);
        assertEquals(new BigDecimal("100.5"), result);
        assertEquals(100.50, result.doubleValue());

        // Test integer-like double value
        result = double2BigDecimal.valueOf(100.0, null, null, null, null);
        assertNotNull(result);
        assertEquals(new BigDecimal("100.0"), result);
        assertEquals(100.0, result.doubleValue());
    }
}
