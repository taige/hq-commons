/*
 * @(#)Base64Test.java Created on 2013-8-14
 *
 * Copyright 2012-2013 Chinabank Payments, Inc. All rights reserved.
 * Use is subject to license terms.
 */
package io.hqwu.commons.util;

import org.apache.commons.codec.binary.Base64;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Description:
 * 
 * @author: shenjianlin <a href="mailto:ustbsjl@gmail.com">ustbsjl@gmail.com</a> <br>
 *          QQ: 79043549
 * @version: 1.0 2013-8-14
 * @history:
 */

public class Base64Test {

    @Test
    public void testBase64() throws Exception {
        assertNull(Base64.encodeBase64String(null));
        
        byte[] bin = "1234567".getBytes();
        String encoded = Base64.encodeBase64String(bin);
        assertEquals((bin.length + 2) / 3 * 4, encoded.length());
        byte[] decoded = Base64.decodeBase64(encoded);
        assertArrayEquals(bin, decoded);
        
        bin = "123".getBytes();
        encoded = Base64.encodeBase64String(bin);
        assertEquals((bin.length + 2) / 3 * 4, encoded.length());
        decoded = Base64.decodeBase64(encoded);
        assertArrayEquals(bin, decoded);
        
        bin = "中国".getBytes();// 4个字节
        encoded = Base64.encodeBase64String(bin);
        assertEquals((bin.length + 2) / 3 * 4, encoded.length());
        decoded = Base64.decodeBase64(encoded);
        assertArrayEquals(bin, decoded);
        assertEquals("中国", new String(decoded));
    }
}
