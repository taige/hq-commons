package com.umpay.commons.util.converters;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class String2MaskStringTest {

    @Test
    void test_valueOf() {
        String2MaskString string2MaskString = new String2MaskString();

        String masked = string2MaskString.valueOf("123456", null, null, null, null);
        assertEquals("**3456", masked);

        masked = string2MaskString.valueOf("1234", null, null, null, null);
        assertEquals("1234", masked);
    }
}
