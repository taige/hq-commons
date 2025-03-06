package io.hqwu.commons.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Created with IntelliJ IDEA
 * User: taige
 * Date: 14-4-2
 * Time: 下午1:37
 */
public class FormatterTest {

    @Test
    public void testFormatNS() throws Exception {
        
        assertEquals(Formatter.formatNS(0), "0");
        assertEquals(Formatter.formatNS(00), "0");
        assertEquals(Formatter.formatNS(12), "12");
        assertEquals(Formatter.formatNS(123), "123");
        assertEquals(Formatter.formatNS(1234), "1,234");
        assertEquals(Formatter.formatNS(12345), "12,345");
        assertEquals(Formatter.formatNS(123456), "123,456");
        assertEquals(Formatter.formatNS(1234567), "1,234,567");
        assertEquals(Formatter.formatNS(12345678), "12,345,678");
        assertEquals(Formatter.formatNS(123456789), "123,456,789");
        assertEquals(Formatter.formatNS(1234567890), "1,234,567,890");
        assertEquals(Formatter.formatNS(12345678901L), "12,345,678,901");
        assertEquals(Formatter.formatNS(123456789012L), "123,456,789,012");
        assertEquals(Formatter.formatNS(1234567890123L), "1,234,567,890,123");
        assertEquals(Formatter.formatNS(12345678901234L), "12,345,678,901,234");
        assertEquals(Formatter.formatNS(123456789012345L), "123,456,789,012,345");
        assertEquals(Formatter.formatNS(1234567890123456L), "1,234,567,890,123,456");
        assertEquals(Formatter.formatNS(12345678901234567L), "12,345,678,901,234,567");
        assertEquals(Formatter.formatNS(123456789012345678L), "123,456,789,012,345,678");
        assertEquals(Formatter.formatNS(1234567890123456789L), "1,234,567,890,123,456,789");
    }

    @Test
    public void test_1() {
        assertEquals("34567890", Formatter.fmtNumR(1234567890).toString());
        assertEquals("12345678", Formatter.fmtNumL(1234567890).toString());
        assertEquals("     123", Formatter.fmtNumR(123).toString());
        assertEquals("123     ", Formatter.fmtNumL(123).toString());
    }

    @Test
    public void test_2() {
        assertEquals(String.valueOf("   abcdefg"), Formatter.fmtStrR("abcdefg", 10).toString());
        assertEquals(String.valueOf("abcdefg   "), Formatter.fmtStrL("abcdefg", 10).toString());

        assertEquals(String.valueOf("abcdefg"), Formatter.fmtStrR("abcdefg", 0).toString());
        assertEquals(String.valueOf("abcdefg"), Formatter.fmtStrL("abcdefg", 0).toString());

        assertEquals("          ", Formatter.fmtStrR("", 10).toString());
        assertEquals("          ", Formatter.fmtStrL("", 10).toString());

//        assertEquals(null, Formatter.fmtStrR(null, 10).toString());
//        assertEquals(null, Formatter.fmtStrL(null, 10).toString());
    }

    @Test
    public void test_3() {
        assertEquals(String.valueOf("abcdefg"), Formatter.ltrim("abcdefg", 20).toString());
        assertEquals(String.valueOf("abcdefg"), Formatter.rtrim("abcdefg", 20).toString());

        assertEquals(String.valueOf("fg"), Formatter.ltrim("abcdefg", 2).toString());
        assertEquals(String.valueOf("ab"), Formatter.rtrim("abcdefg", 2).toString());

//        assertEquals(String.valueOf(null), Formatter.ltrim(null, 2).toString());
//        assertEquals(String.valueOf(null), Formatter.rtrim(null, 2).toString());
    }

    @Test
    public void test_4() {
        assertEquals(String.valueOf("000abcdefg"), Formatter.ralign("abcdefg", 10).toString());
        assertEquals(String.valueOf("abcdefg   "), Formatter.lalign("abcdefg", 10).toString());

        assertEquals(String.valueOf("efg"), Formatter.ralign("abcdefg", 3).toString());
        assertEquals(String.valueOf("abc"), Formatter.lalign("abcdefg", 3).toString());

        assertEquals("000", Formatter.ralign(null, 3).toString());
        assertEquals("   ", Formatter.lalign(null, 3).toString());
    }

    @Test
    public void test_5() {
        assertEquals("a b c", Formatter.trim("  a b c  "));
    }
}
