package io.hqwu.commons.util;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Description:
 * 
 */
public class StringUtilTest {

    @Test
    public void testCentToDollar() {
        assertEquals("0", StringUtil.centToDollar(null));
        assertEquals("0", StringUtil.centToDollar(""));
        assertEquals("0.00", StringUtil.centToDollar("0"));
        assertEquals("-0.01", StringUtil.centToDollar("-1"));
        assertEquals("1.00", StringUtil.centToDollar("100"));
        assertEquals("1000000000.00", StringUtil.centToDollar("100000000000"), "大数处理");
        assertEquals("-1.00", StringUtil.centToDollar("-100"));
        assertEquals("1.00", StringUtil.centToDollar("100.4"));
        assertEquals("1.01", StringUtil.centToDollar("100.5"));
        assertEquals("1.01", StringUtil.centToDollar("100.6"));
    }

    @Test
    public void testDollarToString() {
        assertEquals("0.00", StringUtil.dollarToString(0));
        assertEquals("0.01", StringUtil.dollarToString(0.01));
        assertEquals("-0.01", StringUtil.dollarToString(-0.01));
        assertEquals("1.00", StringUtil.dollarToString(1));
        assertEquals("100000000000.01", StringUtil.dollarToString(100000000000.01), "大数处理");
        assertEquals("100.40", StringUtil.dollarToString(100.4));
        assertEquals("100.04", StringUtil.dollarToString(100.04));
        assertEquals("100.00", StringUtil.dollarToString(100.004));
        assertEquals("100.00", StringUtil.dollarToString(100.0045));
        assertEquals("100.01", StringUtil.dollarToString(100.005));
        assertEquals("100.01", StringUtil.dollarToString(100.006));
    }

    @Test
    public void testCentToDollarShort() {
        assertEquals("0", StringUtil.centToDollarShort(null));
        assertEquals("0", StringUtil.centToDollarShort(""));
        assertEquals("0", StringUtil.centToDollarShort("0"));
        assertEquals("-0", StringUtil.centToDollarShort("-1"));
        assertEquals("1", StringUtil.centToDollarShort("100"));
        assertEquals("1000000000", StringUtil.centToDollarShort("100000000000"), "大数处理");
        assertEquals("-1", StringUtil.centToDollarShort("-100"));
        assertEquals("1", StringUtil.centToDollarShort("100.4"));
        assertEquals("1", StringUtil.centToDollarShort("100.5"));
        assertEquals("1", StringUtil.centToDollarShort("100.6"));
    }

    @Test
    public void testDollarToCent() {
        assertEquals("0", StringUtil.dollarToCent(null));
        assertEquals("0", StringUtil.dollarToCent(""));
        assertEquals("100", StringUtil.dollarToCent("1"));
        assertEquals("-100", StringUtil.dollarToCent("-1"));
        assertEquals( "10000000000000", StringUtil.dollarToCent("100000000000"), "大数处理");
        assertEquals("123", StringUtil.dollarToCent("1.23"));
        assertEquals("1023", StringUtil.dollarToCent("10.23"));
    }

    @Test
    public void testProtect() {
        assertEquals("888117******1367=*********", StringUtil.protect("8881170010011367=020128375"));
        assertEquals("888117******1367D*********", StringUtil.protect("8881170010011367D020128375"));
        assertEquals("998881********1367^David^**********", StringUtil.protect("998881170010011367^David^1609123000"));
        assertEquals("***", StringUtil.protect("123"));
    }

    @Test
    public void testProtectNoSpecialCharacters() {
        // Line 151 coverage: string with length > 6 but no '=', '^', or 'D' characters
        // This will trigger lastFourIndex = len - 4
        // For 10 chars: shows first 6, then last 4 = all 10 chars visible
        assertEquals("1234567890", StringUtil.protect("1234567890"));
        // For 14 chars: shows first 6, chars 6-9 masked (4 asterisks), then last 4 visible
        assertEquals("123456****abcd", StringUtil.protect("1234567890abcd"));
        // For 16 chars: shows first 6, chars 6-11 masked (6 asterisks), then last 4 visible
        assertEquals("testca******wxyz", StringUtil.protect("testcase1234wxyz"));
    }

    @Test
    public void test_areNotEmpty() {
        assertTrue(StringUtil.areNotEmpty("bcd", "abc"));
        assertFalse(StringUtil.areNotEmpty("  ", "abc"));
        assertFalse(StringUtil.areNotEmpty("", ""));
        assertFalse(StringUtil.areNotEmpty("", "  "));
    }

    @Test
    public void testAreNotEmptyWithNullArray() {
        // Line 201 coverage: values == null
        assertFalse(StringUtil.areNotEmpty((String[]) null));
    }

    @Test
    public void testAreNotEmptyWithEmptyArray() {
        // Line 201 coverage: values.length == 0
        assertFalse(StringUtil.areNotEmpty(new String[0]));
    }

    @Test
    public void test_getSortDataByMap() {
        Map<String, String> m = new HashMap<String, String>();
        m.put("cd", "122");
        m.put("cb", "123");
        m.put("bd", "124");
        m.put("ab", "125");
        m.put("ACB", "");
        m.put("ef", "126");
        String r = StringUtil.getSortDataByMap(m);
        assertEquals("ab=125&bd=124&cb=123&cd=122&ef=126", r);
    }

    // ==================== Test cases for padleft method ====================

    @Test
    public void testPadleftBasic() {
        // Basic padding with zeros
        String result = StringUtil.padleft("123", 6, '0');
        assertEquals("000123", result, "Should pad left with zeros");
    }

    @Test
    public void testPadleftWithSpaces() {
        // Padding with spaces
        String result = StringUtil.padleft("abc", 8, ' ');
        assertEquals("     abc", result, "Should pad left with spaces");
    }

    @Test
    public void testPadleftWithCustomCharacter() {
        // Padding with custom character
        String result = StringUtil.padleft("test", 10, '*');
        assertEquals("******test", result, "Should pad left with asterisks");
    }

    @Test
    public void testPadleftNoNeedToPad() {
        // String length equals target length - no padding needed
        String result = StringUtil.padleft("hello", 5, '0');
        assertEquals("hello", result, "Should not pad when length matches");
    }

    @Test
    public void testPadleftSingleCharacter() {
        // Padding single character
        String result = StringUtil.padleft("5", 4, '0');
        assertEquals("0005", result, "Should pad single character");
    }

    @Test
    public void testPadleftEmptyString() {
        // Padding empty string
        String result = StringUtil.padleft("", 5, 'x');
        assertEquals("xxxxx", result, "Should pad empty string completely");
    }

    @Test
    public void testPadleftWithLeadingSpaces() {
        // String with leading spaces (will be trimmed)
        String result = StringUtil.padleft("  test", 8, '0');
        assertEquals("0000test", result, "Should trim leading spaces before padding");
    }

    @Test
    public void testPadleftWithTrailingSpaces() {
        // String with trailing spaces (will be trimmed)
        String result = StringUtil.padleft("test  ", 8, '0');
        assertEquals("0000test", result, "Should trim trailing spaces before padding");
    }

    @Test
    public void testPadleftWithBothSideSpaces() {
        // String with both leading and trailing spaces
        String result = StringUtil.padleft("  test  ", 10, '-');
        assertEquals("------test", result, "Should trim both sides before padding");
    }

    @Test
    public void testPadleftLengthExceedsAfterTrim() {
        // String length exceeds target length after trim - should throw exception
        assertThrows(IllegalArgumentException.class, () -> {
            StringUtil.padleft("toolongstring", 5, '0');
        }, "Should throw IllegalArgumentException when trimmed string is too long");
    }

    @Test
    public void testPadleftExactLengthAfterTrim() {
        // String with spaces that becomes exact length after trim
        String result = StringUtil.padleft("  hello  ", 5, '0');
        assertEquals("hello", result, "Should match length exactly after trim");
    }

    @Test
    public void testPadleftLengthExceedsBeforeTrim() {
        // String with spaces, but after trim still exceeds - should throw exception
        assertThrows(IllegalArgumentException.class, () -> {
            StringUtil.padleft("  verylongstring  ", 10, '0');
        }, "Should throw when trimmed string exceeds target length");
    }

    @Test
    public void testPadleftNumericString() {
        // Padding numeric string (common use case)
        String result = StringUtil.padleft("42", 8, '0');
        assertEquals("00000042", result, "Should pad numeric string with zeros");
    }

    @Test
    public void testPadleftAlphanumeric() {
        // Padding alphanumeric string
        String result = StringUtil.padleft("abc123", 12, '_');
        assertEquals("______abc123", result, "Should pad alphanumeric string");
    }

    @Test
    public void testPadleftMinimumLength() {
        // Minimum padding - length 1
        String result = StringUtil.padleft("a", 2, 'X');
        assertEquals("Xa", result, "Should pad to length 2");
    }

    @Test
    public void testPadleftLargeNumber() {
        // Padding to create a larger string
        String result = StringUtil.padleft("1", 20, '0');
        assertEquals("00000000000000000001", result, "Should pad to 20 characters");
        assertEquals(20, result.length(), "Length should be exactly 20");
    }

    @Test
    public void testPadleftSpecialCharacters() {
        // String containing special characters
        String result = StringUtil.padleft("@#$", 8, '0');
        assertEquals("00000@#$", result, "Should handle special characters");
    }

    @Test
    public void testPadleftWithDigitPadChar() {
        // Using digit as padding character
        String result = StringUtil.padleft("end", 10, '9');
        assertEquals("9999999end", result, "Should pad with digit character");
    }

    @Test
    public void testPadleftOnlySpaces() {
        // String with only spaces (becomes empty after trim)
        String result = StringUtil.padleft("     ", 7, '#');
        assertEquals("#######", result, "Should treat space-only string as empty");
    }

    @Test
    public void testPadleftChinese() {
        // String with Chinese characters
        String result = StringUtil.padleft("中文", 6, '0');
        assertEquals("0000中文", result, "Should handle Chinese characters");
    }

    @Test
    public void testPadleftMultipleCalls() {
        // Test consistency across multiple calls
        String result1 = StringUtil.padleft("123", 8, '0');
        String result2 = StringUtil.padleft("123", 8, '0');
        assertEquals(result1, result2, "Should produce consistent results");
        assertEquals("00000123", result1);
    }

    @Test
    public void testPadleftEdgeCaseZeroFill() {
        // Common use case: zero-filling for IDs or codes
        String id = "5";
        String result = StringUtil.padleft(id, 10, '0');
        assertEquals("0000000005", result, "Should zero-fill ID to 10 digits");
    }

    @Test
    public void testPadleftExceptionMessage() {
        // Verify exception message contains useful information
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            StringUtil.padleft("verylongtext", 5, '0');
        });

        String message = exception.getMessage();
        assertTrue(message.contains("invalid len"), "Exception message should mention 'invalid len'");
        assertTrue(message.contains("12"), "Exception message should contain actual length");
        assertTrue(message.contains("5"), "Exception message should contain target length");
    }
}


