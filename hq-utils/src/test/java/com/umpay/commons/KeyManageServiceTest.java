package com.umpay.commons;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.GeneralSecurityException;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * Created with IntelliJ IDEA for unpay-common
 * User: taige
 * Date: 2018/6/8
 * Time: 下午1:22
 */
public class KeyManageServiceTest {

    private KeyManageService keyManageService;

    @BeforeEach
    public void before() throws GeneralSecurityException {
        if (keyManageService == null) {
            keyManageService = new KeyManageServiceLocalImpl();
        }
    }

    @Test
    public void test_encryptByMasterKey() throws Exception {
        String key = keyManageService.encrypt("helloIt'sASecret".getBytes());
        assertEquals("YkQ65svm2x6Dpx7mNigaSvMGct4j0syHAdWEB3luYCk=", key);
        assertArrayEquals(keyManageService.decrypt(key), "helloIt'sASecret".getBytes());
    }

}