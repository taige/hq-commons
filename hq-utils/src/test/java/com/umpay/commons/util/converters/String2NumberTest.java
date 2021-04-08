package com.umpay.commons.util.converters;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Created with IntelliJ IDEA for pp-gopay-fa
 * User: taige
 * Date: 2020/5/9
 * Time: 19:31
 */
class String2NumberTest {

    @Test
    void test_valueOf() {
        
        String2Number string2Number = new String2Number();

        Number n = string2Number.valueOf("121", null, null, null, null);
        assertEquals(Long.class, n.getClass());
        assertEquals(121L, n);
        assertEquals(121, n.intValue());

        n = string2Number.valueOf("1,121", null, null, null, null, "#,###");
        assertEquals(Long.class, n.getClass());
        assertEquals(1121L, n);
        assertEquals(1121, n.intValue());

        n = string2Number.valueOf("12.1", null, null, null, null);
        assertEquals(Double.class, n.getClass());
        assertEquals(12.1D, n);
        assertEquals(12.1F, n.floatValue());

        n = string2Number.valueOf("12,34,12.11", null, null, null, null, "#,##");
        assertEquals(Double.class, n.getClass());
        assertEquals(123412.11D, n);
        assertEquals(123412.11F, n.floatValue());

        assertNull(string2Number.valueOf("ab121d", null, null, null, null, "##.##"));
    }

}