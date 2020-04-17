package com.umpay.commons.util;

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
    public void test_areNotEmpty() {
        assertTrue(StringUtil.areNotEmpty("bcd", "abc"));
        assertFalse(StringUtil.areNotEmpty("  ", "abc"));
        assertFalse(StringUtil.areNotEmpty("", ""));
        assertFalse(StringUtil.areNotEmpty("", "  "));
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
}
