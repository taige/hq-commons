package io.hqwu.commons.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Created with IntelliJ IDEA for hello-app
 * User: taige
 * Date: 2018/4/19
 * Time: 下午2:59
 */
public class AmountUtilTest {

    @Test
    public void test_cent2Dollar() throws Exception {
        assertEquals("123.95", AmountUtil.cent2Dollar(12395));

        assertEquals("1", AmountUtil.cent2Dollar(1, -1));
        assertEquals("1", AmountUtil.cent2Dollar(1, 0));
        assertEquals("11", AmountUtil.cent2Dollar(11, 0));

        assertEquals("0.0", AmountUtil.cent2Dollar(0, 1));
        assertEquals("0.1", AmountUtil.cent2Dollar(1, 1));
        assertEquals("-0.1", AmountUtil.cent2Dollar(-1, 1));
        assertEquals("1.2", AmountUtil.cent2Dollar(12, 1));
        assertEquals("12.3", AmountUtil.cent2Dollar(123, 1));
        assertEquals("123.4", AmountUtil.cent2Dollar(1234, 1));

        assertEquals("0.00", AmountUtil.cent2Dollar(0, 2));
        assertEquals("0.01", AmountUtil.cent2Dollar(1, 2));
        assertEquals("0.12", AmountUtil.cent2Dollar(12, 2));
        assertEquals("-0.12", AmountUtil.cent2Dollar(-12, 2));
        assertEquals("1.23", AmountUtil.cent2Dollar(123, 2));
        assertEquals("12.34", AmountUtil.cent2Dollar(1234, 2));
        assertEquals("123.45", AmountUtil.cent2Dollar(12345, 2));

        assertEquals("123.45", AmountUtil.cent2Dollar(12345));
        assertEquals("123.45", AmountUtil.cent2Dollar(12345, true));
        assertEquals("1,234.56", AmountUtil.cent2Dollar(123456, true));
        assertEquals("12345678.90", AmountUtil.cent2Dollar(1234567890));
        assertEquals("12,345,678.90", AmountUtil.cent2Dollar(1234567890, true));
        assertEquals("12345678901.23", AmountUtil.cent2Dollar(1234567890123l));
        assertEquals("12,345,678,901.23", AmountUtil.cent2Dollar(1234567890123l, true));
        assertEquals("12,345,678", AmountUtil.cent2Dollar(12345678, 0, true));
        assertEquals("678", AmountUtil.cent2Dollar(678, 0, true));
        assertEquals("12345678", AmountUtil.cent2Dollar(12345678, 0, false));
        assertEquals("678", AmountUtil.cent2Dollar(678, 0, false));

        assertEquals("0.000", AmountUtil.cent2Dollar(0, 3));
        assertEquals("0.001", AmountUtil.cent2Dollar(1, 3));
        assertEquals("0.012", AmountUtil.cent2Dollar(12, 3));
        assertEquals("0.123", AmountUtil.cent2Dollar(123, 3));
        assertEquals("1.234", AmountUtil.cent2Dollar(1234, 3));
        assertEquals("-1.234", AmountUtil.cent2Dollar(-1234, 3));
        assertEquals("12.345", AmountUtil.cent2Dollar(12345, 3));
        assertEquals("123.456", AmountUtil.cent2Dollar(123456, 3));
        assertEquals("-123.456", AmountUtil.cent2Dollar(-123456, 3));

        assertEquals("0.0001", AmountUtil.cent2Dollar(1, 4));
        assertEquals("0.00001", AmountUtil.cent2Dollar(1, 5));
        assertEquals("0.000001", AmountUtil.cent2Dollar(1, 6));
        assertEquals("0.0000001", AmountUtil.cent2Dollar(1, 7));
        assertEquals("0.00000001", AmountUtil.cent2Dollar(1, 8));
        assertEquals("0.000000001", AmountUtil.cent2Dollar(1, 9));

        assertThrows(IllegalArgumentException.class, () -> AmountUtil.cent2Dollar(1, 10));

    }

    @Test
    void test_formatThousands() {
        assertEquals(AmountUtil.formatThousands(0), "0");
        assertEquals(AmountUtil.formatThousands(1), "1");
        assertEquals(AmountUtil.formatThousands(12), "12");
        assertEquals(AmountUtil.formatThousands(123), "123");
        assertEquals(AmountUtil.formatThousands(1234), "1,234");
        assertEquals(AmountUtil.formatThousands(12345), "12,345");
        assertEquals(AmountUtil.formatThousands(123456), "123,456");
        assertEquals(AmountUtil.formatThousands(1234567), "1,234,567");
    }

    @Test
    public void test_dollar2Cent() throws Exception {
        assertEquals(100, AmountUtil.dollar2Cent("100.0", 0));
        assertEquals(1234567, AmountUtil.dollar2Cent("1,234,567.08", -1));
        assertEquals(1234567, AmountUtil.dollar2Cent("1234567.08", 0));

        assertEquals(123456700, AmountUtil.dollar2Cent("1234567.001", 2));

        assertEquals(1234567, AmountUtil.dollar2Cent("1,234,567.8", -1));
        assertEquals(1234567, AmountUtil.dollar2Cent("1234567.8", 0));

        assertEquals(1, AmountUtil.dollar2Cent(".01", 2));
        assertEquals(0, AmountUtil.dollar2Cent("", 2));
        assertEquals(0, AmountUtil.dollar2Cent(null, 2));

        assertEquals(0, AmountUtil.dollar2Cent("0.01", 1));
        assertEquals(0, AmountUtil.dollar2Cent("0.001", 2));
        assertEquals(1, AmountUtil.dollar2Cent("0.01", 2));
        assertEquals(10, AmountUtil.dollar2Cent("0.1", 2));
        assertEquals(12, AmountUtil.dollar2Cent("0.12", 2));
        assertEquals(101, AmountUtil.dollar2Cent("01.01", 2));

        assertEquals(1010, AmountUtil.dollar2Cent("1.01", 3));
        assertEquals(1012, AmountUtil.dollar2Cent("1.012", 3));
        assertEquals(1012, AmountUtil.dollar2Cent("1.0124", 3));
        assertEquals(1024, AmountUtil.dollar2Cent("0.1024", 4));

        assertEquals(100, AmountUtil.dollar2Cent("1", 2));
        assertEquals(12340, AmountUtil.dollar2Cent("123.4", 2));
        assertEquals(12340, AmountUtil.dollar2Cent("123.4", 2));
        assertEquals(-12340, AmountUtil.dollar2Cent("-123.4", 2));

        assertEquals(123456781, AmountUtil.dollar2Cent("1234567.81"));

        assertEquals(123456780, AmountUtil.dollar2Cent("1234567.8", 2));
        assertEquals(123456780, AmountUtil.dollar2Cent("1,234,567.8", 2));

        assertEquals(123456780, AmountUtil.dollar2Cent("+1,234,567.8", 2));

        assertThrows(IllegalArgumentException.class, () -> AmountUtil.dollar2Cent("12345.67.8", 2));

        assertThrows(IllegalArgumentException.class, () -> AmountUtil.dollar2Cent("123s4567.8", 2));

        assertThrows(IllegalArgumentException.class, () -> AmountUtil.dollar2Cent("1234567.8", 10));

        assertThrows(IllegalArgumentException.class, () -> AmountUtil.dollar2Cent("1+234567.8"));

        assertThrows(IllegalArgumentException.class, () -> AmountUtil.dollar2Cent("1-234567.8"));

    }
}
