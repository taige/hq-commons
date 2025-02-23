package com.umpay.commons.util;


import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.text.DecimalFormatSymbols;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Created with IntelliJ IDEA for hello-app
 * User: taige
 * Date: 2018/4/19
 * Time: 下午2:59
 */
public class AmountUtilLocaleTest {
    private static final Logger LOGGER = new Logger();

    @Test
    public void test_cent2Dollar() throws Exception {
        Set<Character> set1 = new HashSet<Character>();
        Set<Character> set2 = new HashSet<Character>();
        for (Locale locale: Locale.getAvailableLocales()) {
            DecimalFormatSymbols dfs = DecimalFormatSymbols.getInstance(locale);
            LOGGER.debug("%2s-%2s: '%s' '%s'", locale.getLanguage(), locale.getCountry(), dfs.getGroupingSeparator(), dfs.getDecimalSeparator());
            set1.add(dfs.getGroupingSeparator());
            set2.add(dfs.getDecimalSeparator());
        }
        LOGGER.debug(set1);
        LOGGER.debug(set2);
        test_cent2Dollar(AmountUtilLocale.getInstance());
        test_cent2Dollar(AmountUtilLocale.getInstance("us"));
        test_cent2Dollar(AmountUtilLocale.getInstance("zh"));
        test_cent2Dollar(AmountUtilLocale.getInstance("zh", "CN"));
        test_cent2Dollar_de(AmountUtilLocale.getInstance("de"));
        test_cent2Dollar_fr(AmountUtilLocale.getInstance("fr"));
        test_cent2Dollar_ch(AmountUtilLocale.getInstance("de", "CH"));
    }

    public void test_cent2Dollar(AmountUtilLocale moneyUtil) throws Exception {
        assertEquals("123.95", moneyUtil.cent2Dollar(12395));

        assertEquals("1", moneyUtil.cent2Dollar(1, -1));
        assertEquals("1", moneyUtil.cent2Dollar(1, 0));
        assertEquals("11", moneyUtil.cent2Dollar(11, 0));

        assertEquals("0.0", moneyUtil.cent2Dollar(0, 1));
        assertEquals("0.1", moneyUtil.cent2Dollar(1, 1));
        assertEquals("-0.1", moneyUtil.cent2Dollar(-1, 1));
        assertEquals("1.2", moneyUtil.cent2Dollar(12, 1));
        assertEquals("12.3", moneyUtil.cent2Dollar(123, 1));
        assertEquals("123.4", moneyUtil.cent2Dollar(1234, 1));

        assertEquals("0.00", moneyUtil.cent2Dollar(0, 2));
        assertEquals("0.01", moneyUtil.cent2Dollar(1, 2));
        assertEquals("0.12", moneyUtil.cent2Dollar(12, 2));
        assertEquals("-0.12", moneyUtil.cent2Dollar(-12, 2));
        assertEquals("1.23", moneyUtil.cent2Dollar(123, 2));
        assertEquals("12.34", moneyUtil.cent2Dollar(1234, 2));
        assertEquals("123.45", moneyUtil.cent2Dollar(12345, 2));

        assertEquals("123.45", moneyUtil.cent2Dollar(12345));
        assertEquals("123.45", moneyUtil.cent2Dollar(12345, true));
        assertEquals("1,234.56", moneyUtil.cent2Dollar(123456, true));
        assertEquals("12345678.90", moneyUtil.cent2Dollar(1234567890));
        assertEquals("12,345,678.90", moneyUtil.cent2Dollar(1234567890, true));
        assertEquals("12345678901.23", moneyUtil.cent2Dollar(1234567890123l));
        assertEquals("12,345,678,901.23", moneyUtil.cent2Dollar(1234567890123l, true));
        assertEquals("12,345,678", moneyUtil.cent2Dollar(12345678, 0, true));
        assertEquals("678", moneyUtil.cent2Dollar(678, 0, true));
        assertEquals("12345678", moneyUtil.cent2Dollar(12345678, 0, false));
        assertEquals("678", moneyUtil.cent2Dollar(678, 0, false));

        assertEquals("0.000", moneyUtil.cent2Dollar(0, 3));
        assertEquals("0.001", moneyUtil.cent2Dollar(1, 3));
        assertEquals("0.012", moneyUtil.cent2Dollar(12, 3));
        assertEquals("0.123", moneyUtil.cent2Dollar(123, 3));
        assertEquals("1.234", moneyUtil.cent2Dollar(1234, 3));
        assertEquals("-1.234", moneyUtil.cent2Dollar(-1234, 3));
        assertEquals("12.345", moneyUtil.cent2Dollar(12345, 3));
        assertEquals("123.456", moneyUtil.cent2Dollar(123456, 3));
        assertEquals("-123.456", moneyUtil.cent2Dollar(-123456, 3));

        assertEquals("0.0001", moneyUtil.cent2Dollar(1, 4));
        assertEquals("0.00001", moneyUtil.cent2Dollar(1, 5));
        assertEquals("0.000001", moneyUtil.cent2Dollar(1, 6));
        assertEquals("0.0000001", moneyUtil.cent2Dollar(1, 7));
        assertEquals("0.00000001", moneyUtil.cent2Dollar(1, 8));
        assertEquals("0.000000001", moneyUtil.cent2Dollar(1, 9));

        assertThrows(IllegalArgumentException.class, () ->
                moneyUtil.cent2Dollar(1, 10)
        );

    }

    /**
     * 德语 千分位：. 小数点：,
     * @param moneyUtil
     * @throws Exception
     */
    public void test_cent2Dollar_de(AmountUtilLocale moneyUtil) throws Exception {
        assertEquals("123,95", moneyUtil.cent2Dollar(12395));

        assertEquals("1", moneyUtil.cent2Dollar(1, -1));
        assertEquals("1", moneyUtil.cent2Dollar(1, 0));
        assertEquals("11", moneyUtil.cent2Dollar(11, 0));

        assertEquals("0,0", moneyUtil.cent2Dollar(0, 1));
        assertEquals("0,1", moneyUtil.cent2Dollar(1, 1));
        assertEquals("-0,1", moneyUtil.cent2Dollar(-1, 1));
        assertEquals("1,2", moneyUtil.cent2Dollar(12, 1));
        assertEquals("12,3", moneyUtil.cent2Dollar(123, 1));
        assertEquals("123,4", moneyUtil.cent2Dollar(1234, 1));

        assertEquals("0,00", moneyUtil.cent2Dollar(0, 2));
        assertEquals("0,01", moneyUtil.cent2Dollar(1, 2));
        assertEquals("0,12", moneyUtil.cent2Dollar(12, 2));
        assertEquals("-0,12", moneyUtil.cent2Dollar(-12, 2));
        assertEquals("1,23", moneyUtil.cent2Dollar(123, 2));
        assertEquals("12,34", moneyUtil.cent2Dollar(1234, 2));
        assertEquals("123,45", moneyUtil.cent2Dollar(12345, 2));

        assertEquals("123,45", moneyUtil.cent2Dollar(12345));
        assertEquals("123,45", moneyUtil.cent2Dollar(12345, true));
        assertEquals("1.234,56", moneyUtil.cent2Dollar(123456, true));
        assertEquals("12345678,90", moneyUtil.cent2Dollar(1234567890));
        assertEquals("12.345.678,90", moneyUtil.cent2Dollar(1234567890, true));
        assertEquals("12345678901,23", moneyUtil.cent2Dollar(1234567890123l));
        assertEquals("12.345.678.901,23", moneyUtil.cent2Dollar(1234567890123l, true));
        assertEquals("12.345.678", moneyUtil.cent2Dollar(12345678, 0, true));
        assertEquals("678", moneyUtil.cent2Dollar(678, 0, true));
        assertEquals("12345678", moneyUtil.cent2Dollar(12345678, 0, false));
        assertEquals("678", moneyUtil.cent2Dollar(678, 0, false));

        assertEquals("0,000", moneyUtil.cent2Dollar(0, 3));
        assertEquals("0,001", moneyUtil.cent2Dollar(1, 3));
        assertEquals("0,012", moneyUtil.cent2Dollar(12, 3));
        assertEquals("0,123", moneyUtil.cent2Dollar(123, 3));
        assertEquals("1,234", moneyUtil.cent2Dollar(1234, 3));
        assertEquals("-1,234", moneyUtil.cent2Dollar(-1234, 3));
        assertEquals("12,345", moneyUtil.cent2Dollar(12345, 3));
        assertEquals("123,456", moneyUtil.cent2Dollar(123456, 3));
        assertEquals("-123,456", moneyUtil.cent2Dollar(-123456, 3));

        assertEquals("0,0001", moneyUtil.cent2Dollar(1, 4));
        assertEquals("0,00001", moneyUtil.cent2Dollar(1, 5));
        assertEquals("0,000001", moneyUtil.cent2Dollar(1, 6));
        assertEquals("0,0000001", moneyUtil.cent2Dollar(1, 7));
        assertEquals("0,00000001", moneyUtil.cent2Dollar(1, 8));
        assertEquals("0,000000001", moneyUtil.cent2Dollar(1, 9));

        assertThrows(IllegalArgumentException.class, () ->
                moneyUtil.cent2Dollar(1, 10)
        );

    }

    /**
     * 法语 千分位：0xA0 小数点：,
     * @param moneyUtil
     * @throws Exception
     */
    public void test_cent2Dollar_fr(AmountUtilLocale moneyUtil) throws Exception {
        assertEquals("1 234,56", moneyUtil.cent2Dollar(123456, true));
        assertEquals("12345678,90", moneyUtil.cent2Dollar(1234567890));
        assertEquals("12 345 678,90", moneyUtil.cent2Dollar(1234567890, true));
        assertEquals("12345678901,23", moneyUtil.cent2Dollar(1234567890123l));
        assertEquals("12 345 678 901,23", moneyUtil.cent2Dollar(1234567890123l, true));
        assertEquals("12 345 678", moneyUtil.cent2Dollar(12345678, 0, true));
    }

    /**
     * 瑞士（德语、法语、意大利语） 千分位：' 小数点：.
     * @param moneyUtil
     * @throws Exception
     */
    public void test_cent2Dollar_ch(AmountUtilLocale moneyUtil) throws Exception {
        assertEquals("123.95", moneyUtil.cent2Dollar(12395));

        assertEquals("1", moneyUtil.cent2Dollar(1, -1));
        assertEquals("1", moneyUtil.cent2Dollar(1, 0));
        assertEquals("11", moneyUtil.cent2Dollar(11, 0));

        assertEquals("0.0", moneyUtil.cent2Dollar(0, 1));
        assertEquals("0.1", moneyUtil.cent2Dollar(1, 1));
        assertEquals("-0.1", moneyUtil.cent2Dollar(-1, 1));
        assertEquals("1.2", moneyUtil.cent2Dollar(12, 1));
        assertEquals("12.3", moneyUtil.cent2Dollar(123, 1));
        assertEquals("123.4", moneyUtil.cent2Dollar(1234, 1));

        assertEquals("0.00", moneyUtil.cent2Dollar(0, 2));
        assertEquals("0.01", moneyUtil.cent2Dollar(1, 2));
        assertEquals("0.12", moneyUtil.cent2Dollar(12, 2));
        assertEquals("-0.12", moneyUtil.cent2Dollar(-12, 2));
        assertEquals("1.23", moneyUtil.cent2Dollar(123, 2));
        assertEquals("12.34", moneyUtil.cent2Dollar(1234, 2));
        assertEquals("123.45", moneyUtil.cent2Dollar(12345, 2));

        assertEquals("123.45", moneyUtil.cent2Dollar(12345));
        assertEquals("123.45", moneyUtil.cent2Dollar(12345, true));
        assertEquals("1'234.56", moneyUtil.cent2Dollar(123456, true));
        assertEquals("12345678.90", moneyUtil.cent2Dollar(1234567890));
        assertEquals("12'345'678.90", moneyUtil.cent2Dollar(1234567890, true));
        assertEquals("12345678901.23", moneyUtil.cent2Dollar(1234567890123l));
        assertEquals("12'345'678'901.23", moneyUtil.cent2Dollar(1234567890123l, true));
        assertEquals("12'345'678", moneyUtil.cent2Dollar(12345678, 0, true));
        assertEquals("678", moneyUtil.cent2Dollar(678, 0, true));
        assertEquals("12345678", moneyUtil.cent2Dollar(12345678, 0, false));
        assertEquals("678", moneyUtil.cent2Dollar(678, 0, false));

        assertEquals("0.000", moneyUtil.cent2Dollar(0, 3));
        assertEquals("0.001", moneyUtil.cent2Dollar(1, 3));
        assertEquals("0.012", moneyUtil.cent2Dollar(12, 3));
        assertEquals("0.123", moneyUtil.cent2Dollar(123, 3));
        assertEquals("1.234", moneyUtil.cent2Dollar(1234, 3));
        assertEquals("-1.234", moneyUtil.cent2Dollar(-1234, 3));
        assertEquals("12.345", moneyUtil.cent2Dollar(12345, 3));
        assertEquals("123.456", moneyUtil.cent2Dollar(123456, 3));
        assertEquals("-123.456", moneyUtil.cent2Dollar(-123456, 3));

        assertEquals("0.0001", moneyUtil.cent2Dollar(1, 4));
        assertEquals("0.00001", moneyUtil.cent2Dollar(1, 5));
        assertEquals("0.000001", moneyUtil.cent2Dollar(1, 6));
        assertEquals("0.0000001", moneyUtil.cent2Dollar(1, 7));
        assertEquals("0.00000001", moneyUtil.cent2Dollar(1, 8));
        assertEquals("0.000000001", moneyUtil.cent2Dollar(1, 9));

        assertThrows(IllegalArgumentException.class, () ->
                moneyUtil.cent2Dollar(1, 10)
        );
    }

    @Test
    public void test_dollar2Cent() throws Exception {
        test_dollar2Cent(AmountUtilLocale.getInstance());
        test_dollar2Cent_de(AmountUtilLocale.getInstance("de"));
        test_dollar2Cent_fr(AmountUtilLocale.getInstance("fr"));
        test_dollar2Cent_ch(AmountUtilLocale.getInstance("fr", "CH"));
    }

    private void test_dollar2Cent(AmountUtilLocale moneyUtil) throws Exception {
        assertEquals(1234567, moneyUtil.dollar2Cent("1,234,567.8", -1));
        assertEquals(1234567, moneyUtil.dollar2Cent("1234567.8", 0));

        assertEquals(1, moneyUtil.dollar2Cent(".01", 2));
        assertEquals(0, moneyUtil.dollar2Cent("", 2));
        assertEquals(0, moneyUtil.dollar2Cent(null, 2));

        assertEquals(0, moneyUtil.dollar2Cent("0.01", 1));
        assertEquals(0, moneyUtil.dollar2Cent("0.001", 2));
        assertEquals(1, moneyUtil.dollar2Cent("0.01", 2));
        assertEquals(10, moneyUtil.dollar2Cent("0.1", 2));
        assertEquals(12, moneyUtil.dollar2Cent("0.12", 2));
        assertEquals(101, moneyUtil.dollar2Cent("01.01", 2));

        assertEquals(1010, moneyUtil.dollar2Cent("1.01", 3));
        assertEquals(1012, moneyUtil.dollar2Cent("1.012", 3));
        assertEquals(1012, moneyUtil.dollar2Cent("1.0124", 3));
        assertEquals(1024, moneyUtil.dollar2Cent("0.1024", 4));

        assertEquals(100, moneyUtil.dollar2Cent("1", 2));
        assertEquals(12340, moneyUtil.dollar2Cent("123.4", 2));
        assertEquals(12340, moneyUtil.dollar2Cent("123.4", 2));
        assertEquals(-12340, moneyUtil.dollar2Cent("-123.4", 2));

        assertEquals(123456781, moneyUtil.dollar2Cent("1234567.81"));

        assertEquals(123456780, moneyUtil.dollar2Cent("1234567.8", 2));
        assertEquals(123456780, moneyUtil.dollar2Cent("1,234,567.8", 2));

        assertEquals(123456780, moneyUtil.dollar2Cent("+1,234,567.8", 2));

    }

    @ParameterizedTest
    @CsvSource(value = {"12345.67.8,2",
            "123s4567.8,2",
            "1234567.8,10",
            "1+234567.8,",
            "1-234567.8,"})
    public void test_dollar2Cent_error(String amt, Integer precision) throws Exception {
        AmountUtilLocale moneyUtil = AmountUtilLocale.getInstance();
        assertThrows(IllegalArgumentException.class, () -> {
                    if (precision == null) {
                        moneyUtil.dollar2Cent(amt);
                    } else {
                        moneyUtil.dollar2Cent(amt, precision);
                    }
                }
        );
    }

    private void test_dollar2Cent_de(AmountUtilLocale moneyUtil) throws Exception {
        assertEquals(1234567, moneyUtil.dollar2Cent("1.234.567,8", -1));
        assertEquals(1234567, moneyUtil.dollar2Cent("1234567,8", 0));

        assertEquals(1, moneyUtil.dollar2Cent(",01", 2));
        assertEquals(0, moneyUtil.dollar2Cent("", 2));
        assertEquals(0, moneyUtil.dollar2Cent(null, 2));

        assertEquals(0, moneyUtil.dollar2Cent("0,01", 1));
        assertEquals(0, moneyUtil.dollar2Cent("0,001", 2));
        assertEquals(1, moneyUtil.dollar2Cent("0,01", 2));
        assertEquals(10, moneyUtil.dollar2Cent("0,1", 2));
        assertEquals(12, moneyUtil.dollar2Cent("0,12", 2));
        assertEquals(101, moneyUtil.dollar2Cent("01,01", 2));

        assertEquals(1010, moneyUtil.dollar2Cent("1,01", 3));
        assertEquals(1012, moneyUtil.dollar2Cent("1,012", 3));
        assertEquals(1012, moneyUtil.dollar2Cent("1,0124", 3));
        assertEquals(1024, moneyUtil.dollar2Cent("0,1024", 4));

        assertEquals(100, moneyUtil.dollar2Cent("1", 2));
        assertEquals(12340, moneyUtil.dollar2Cent("123,4", 2));
        assertEquals(12340, moneyUtil.dollar2Cent("123,4", 2));
        assertEquals(-12340, moneyUtil.dollar2Cent("-123,4", 2));

        assertEquals(123456781, moneyUtil.dollar2Cent("1234567,81"));

        assertEquals(123456780, moneyUtil.dollar2Cent("1234567,8", 2));
        assertEquals(123456780, moneyUtil.dollar2Cent("1.234.567,8", 2));

        assertEquals(123456780, moneyUtil.dollar2Cent("+1.234.567,8", 2));

    }

    @ParameterizedTest
    @CsvSource(value = {"12345,67,8#2",
            "123s4567,8#2",
            "1234567,8#10",
            "1+234567,8#",
            "1-234567,8#"}, delimiter = '#')
    public void test_dollar2Cent_de_error(String amt, Integer precision) throws Exception {
        AmountUtilLocale moneyUtil = AmountUtilLocale.getInstance("de");
        assertThrows(IllegalArgumentException.class, () -> {
                    if (precision == null) {
                        moneyUtil.dollar2Cent(amt);
                    } else {
                        moneyUtil.dollar2Cent(amt, precision);
                    }
                }
        );
    }

    private void test_dollar2Cent_fr(AmountUtilLocale moneyUtil) throws Exception {
        assertEquals(1234567, moneyUtil.dollar2Cent("1 234 567,8", -1));
        assertEquals(1234567, moneyUtil.dollar2Cent("1234567,8", 0));

        assertEquals(1, moneyUtil.dollar2Cent(",01", 2));
        assertEquals(0, moneyUtil.dollar2Cent("", 2));
        assertEquals(0, moneyUtil.dollar2Cent(null, 2));

        assertEquals(0, moneyUtil.dollar2Cent("0,01", 1));
        assertEquals(0, moneyUtil.dollar2Cent("0,001", 2));
        assertEquals(1, moneyUtil.dollar2Cent("0,01", 2));
        assertEquals(10, moneyUtil.dollar2Cent("0,1", 2));
        assertEquals(12, moneyUtil.dollar2Cent("0,12", 2));
        assertEquals(101, moneyUtil.dollar2Cent("01,01", 2));

        assertEquals(1010, moneyUtil.dollar2Cent("1,01", 3));
        assertEquals(1012, moneyUtil.dollar2Cent("1,012", 3));
        assertEquals(1012, moneyUtil.dollar2Cent("1,0124", 3));
        assertEquals(1024, moneyUtil.dollar2Cent("0,1024", 4));

        assertEquals(100, moneyUtil.dollar2Cent("1", 2));
        assertEquals(12340, moneyUtil.dollar2Cent("123,4", 2));
        assertEquals(12340, moneyUtil.dollar2Cent("123,4", 2));
        assertEquals(-12340, moneyUtil.dollar2Cent("-123,4", 2));

        assertEquals(123456781, moneyUtil.dollar2Cent("1234567,81"));

        assertEquals(123456780, moneyUtil.dollar2Cent("1234567,8", 2));
        assertEquals(123456780, moneyUtil.dollar2Cent("1 234 567,8", 2));

        assertEquals(123456780, moneyUtil.dollar2Cent("+1 234 567,8", 2));
    }

    @ParameterizedTest
    @CsvSource(value = {"12345,67,8#2",
            "123s4567,8#2",
            "1234567,8#10",
            "1+234567,8#",
            "1-234567,8#"}, delimiter = '#')
    public void test_dollar2Cent_fr_error(String amt, Integer precision) throws Exception {
        AmountUtilLocale moneyUtil = AmountUtilLocale.getInstance("fr");
        assertThrows(IllegalArgumentException.class, () -> {
                    if (precision == null) {
                        moneyUtil.dollar2Cent(amt);
                    } else {
                        moneyUtil.dollar2Cent(amt, precision);
                    }
                }
        );

    }

    private void test_dollar2Cent_ch(AmountUtilLocale moneyUtil) throws Exception {
        assertEquals(1234567, moneyUtil.dollar2Cent("1'234'567.8", -1));
        assertEquals(1234567, moneyUtil.dollar2Cent("1234567.8", 0));

        assertEquals(1, moneyUtil.dollar2Cent(".01", 2));
        assertEquals(0, moneyUtil.dollar2Cent("", 2));
        assertEquals(0, moneyUtil.dollar2Cent(null, 2));

        assertEquals(0, moneyUtil.dollar2Cent("0.01", 1));
        assertEquals(0, moneyUtil.dollar2Cent("0.001", 2));
        assertEquals(1, moneyUtil.dollar2Cent("0.01", 2));
        assertEquals(10, moneyUtil.dollar2Cent("0.1", 2));
        assertEquals(12, moneyUtil.dollar2Cent("0.12", 2));
        assertEquals(101, moneyUtil.dollar2Cent("01.01", 2));

        assertEquals(1010, moneyUtil.dollar2Cent("1.01", 3));
        assertEquals(1012, moneyUtil.dollar2Cent("1.012", 3));
        assertEquals(1012, moneyUtil.dollar2Cent("1.0124", 3));
        assertEquals(1024, moneyUtil.dollar2Cent("0.1024", 4));

        assertEquals(100, moneyUtil.dollar2Cent("1", 2));
        assertEquals(12340, moneyUtil.dollar2Cent("123.4", 2));
        assertEquals(12340, moneyUtil.dollar2Cent("123.4", 2));
        assertEquals(-12340, moneyUtil.dollar2Cent("-123.4", 2));

        assertEquals(123456781, moneyUtil.dollar2Cent("1234567.81"));

        assertEquals(123456780, moneyUtil.dollar2Cent("1234567.8", 2));
        assertEquals(123456780, moneyUtil.dollar2Cent("1'234'567.8", 2));

        assertEquals(123456780, moneyUtil.dollar2Cent("+1'234'567.8", 2));
    }

    @ParameterizedTest
    @CsvSource(value = {"12345.67.8,2", "123s4567.8,2", "1234567.8,10", "1+234567.8,", "1-234567.8,"})
    public void test_dollar2Cent_ch_error(String amt, Integer precision) throws Exception {
        AmountUtilLocale moneyUtil = AmountUtilLocale.getInstance("fr", "CH");
        assertThrows(IllegalArgumentException.class, () -> {
                    if (precision == null) {
                        moneyUtil.dollar2Cent(amt);
                    } else {
                        moneyUtil.dollar2Cent(amt, precision);
                    }
                }
        );

    }
}
