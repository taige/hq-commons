package io.hqwu.commons.bean.converters;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Created with IntelliJ IDEA for hq-commons
 * User: taige
 * Date: 2026/1/26
 * Time: 13:35
 */
class String2LocalDateTimeTest {

    @Test
    void test_valueOf() {
        String2LocalDateTime string2LocalDateTime = new String2LocalDateTime();

        // Test with default format (yyyyMMddHHmmss)
        LocalDateTime result = string2LocalDateTime.valueOf("20210103094730", null, null, null, null);
        assertNotNull(result);
        assertEquals(2021, result.getYear());
        assertEquals(1, result.getMonthValue());
        assertEquals(3, result.getDayOfMonth());
        assertEquals(9, result.getHour());
        assertEquals(47, result.getMinute());
        assertEquals(30, result.getSecond());

        // Test with custom format (yyyy-MM-dd HH:mm:ss)
        result = string2LocalDateTime.valueOf("2021-10-03 14:30:45", null, null, null, null, "yyyy-MM-dd HH:mm:ss");
        assertNotNull(result);
        assertEquals(2021, result.getYear());
        assertEquals(10, result.getMonthValue());
        assertEquals(3, result.getDayOfMonth());
        assertEquals(14, result.getHour());
        assertEquals(30, result.getMinute());
        assertEquals(45, result.getSecond());

        // Test with another custom format (yyyy/MM/dd HH:mm)
        result = string2LocalDateTime.valueOf("2022/12/25 23:59", null, null, null, null, "yyyy/MM/dd HH:mm");
        assertNotNull(result);
        assertEquals(2022, result.getYear());
        assertEquals(12, result.getMonthValue());
        assertEquals(25, result.getDayOfMonth());
        assertEquals(23, result.getHour());
        assertEquals(59, result.getMinute());

        // Test with ISO format
        result = string2LocalDateTime.valueOf("2023-05-15T10:30:00", null, null, null, null, "yyyy-MM-dd'T'HH:mm:ss");
        assertNotNull(result);
        assertEquals(2023, result.getYear());
        assertEquals(5, result.getMonthValue());
        assertEquals(15, result.getDayOfMonth());
        assertEquals(10, result.getHour());
        assertEquals(30, result.getMinute());
        assertEquals(0, result.getSecond());

        // Test with blank string
        result = string2LocalDateTime.valueOf("", null, null, null, null);
        assertNull(result);

        // Test with null string
        result = string2LocalDateTime.valueOf(null, null, null, null, null);
        assertNull(result);

        // Test with whitespace string
        result = string2LocalDateTime.valueOf("   ", null, null, null, null);
        assertNull(result);

        // Test with compact format (yyyyMMddHHmm)
        result = string2LocalDateTime.valueOf("202401011530", null, null, null, null, "yyyyMMddHHmm");
        assertNotNull(result);
        assertEquals(2024, result.getYear());
        assertEquals(1, result.getMonthValue());
        assertEquals(1, result.getDayOfMonth());
        assertEquals(15, result.getHour());
        assertEquals(30, result.getMinute());
        assertEquals(0, result.getSecond());
    }
}
