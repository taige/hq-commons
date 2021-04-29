package io.hqwu.commons.bean.converters;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Created with IntelliJ IDEA for pp-gopay-fa
 * User: taige
 * Date: 2020/5/9
 * Time: 20:16
 */
class Boolean2IntegerTest {

    @Test
    void test_valueOf() {
        Boolean2Integer boolean2Integer = new Boolean2Integer();
        assertEquals(1, boolean2Integer.valueOf(true, null, null, null, null));
        assertEquals(0, boolean2Integer.valueOf(false, null, null, null, null));

        assertEquals(2, boolean2Integer.valueOf(true, null, null, null, null, "2", "3"));
        assertEquals(3, boolean2Integer.valueOf(false, null, null, null, null, "2", "3"));

        assertEquals(0, boolean2Integer.valueOf(true, null, null, null, null, "0", "1"));
        assertEquals(1, boolean2Integer.valueOf(false, null, null, null, null, "0", "1"));

        assertEquals(0, boolean2Integer.valueOf(true, null, null, null, null, "", "1"));
        assertEquals(1, boolean2Integer.valueOf(true, null, null, null, null, "", "2"));

        assertEquals(1, boolean2Integer.valueOf(false, null, null, null, null, "0", ""));
        assertEquals(0, boolean2Integer.valueOf(false, null, null, null, null, "1", ""));

        assertEquals(0, boolean2Integer.valueOf(true, null, null, null, null, "a", "1"));
        assertEquals(1, boolean2Integer.valueOf(true, null, null, null, null, "b", "2"));

        assertEquals(1, boolean2Integer.valueOf(true, null, null, null, null, "a", "1a"));
        assertEquals(1, boolean2Integer.valueOf(true, null, null, null, null, "b", "2a"));

        assertEquals(1, boolean2Integer.valueOf(false, null, null, null, null, "0", "c"));
        assertEquals(0, boolean2Integer.valueOf(false, null, null, null, null, "1", "d"));

        assertEquals(0, boolean2Integer.valueOf(false, null, null, null, null, "0c", "c"));
        assertEquals(0, boolean2Integer.valueOf(false, null, null, null, null, "1d", "d"));
    }
}