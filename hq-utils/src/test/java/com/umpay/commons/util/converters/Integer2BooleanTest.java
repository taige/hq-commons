package com.umpay.commons.util.converters;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Created with IntelliJ IDEA for pp-gopay-fa
 * User: taige
 * Date: 2020/5/9
 * Time: 19:12
 */
class Integer2BooleanTest {

    @Test
    void test_valueOf() {
        Integer2Boolean integer2Boolean = new Integer2Boolean();
        assertTrue(integer2Boolean.valueOf(1, null, null, null, null));
        assertFalse(integer2Boolean.valueOf(0, null, null, null, null));
        assertFalse(integer2Boolean.valueOf(-1, null, null, null, null));
        assertFalse(integer2Boolean.valueOf(2, null, null, null, null));

        assertFalse(integer2Boolean.valueOf(1, null, null, null, null, "true", "0", "-1", "2"));
        assertTrue(integer2Boolean.valueOf(0, null, null, null, null, "true", "0", "-1", "2"));
        assertTrue(integer2Boolean.valueOf(-1, null, null, null, null, "true", "0", "-1", "2"));
        assertTrue(integer2Boolean.valueOf(2, null, null, null, null, "true", "0", "-1", "2"));

        assertFalse(integer2Boolean.valueOf(1, null, null, null, null, "false", "1", "-1", "2"));
        assertTrue(integer2Boolean.valueOf(0, null, null, null, null, "false", "1", "-1", "2"));
        assertFalse(integer2Boolean.valueOf(-1, null, null, null, null, "false", "1", "-1", "2"));
        assertFalse(integer2Boolean.valueOf(2, null, null, null, null, "false", "1", "-1", "2"));

        assertTrue(integer2Boolean.valueOf(1, null, null, null, null, "ffalse", "1", "-1", "2"));
        assertFalse(integer2Boolean.valueOf(0, null, null, null, null, "ffalse", "1", "-1", "2"));

        assertFalse(integer2Boolean.valueOf(0, null, null, null, null, "true", "1a", "-1", "2"));
        assertFalse(integer2Boolean.valueOf(-1, null, null, null, null, "false", "1a", "-1", "2"));
    }
}