/*
 * @(#)SecurityUtilTest.java Created on 2013-8-13
 *
 * Copyright 2012-2013 Chinabank Payments, Inc. All rights reserved.
 * Use is subject to license terms.
 */
package com.umpay.commons.util;

import org.apache.commons.codec.binary.Base64;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.security.*;
import java.security.cert.X509Certificate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Description:
 * 
 * @author: shenjianlin <a href="mailto:ustbsjl@gmail.com">ustbsjl@gmail.com</a> <br>
 *          QQ: 79043549
 * @version: 1.0 2013-8-13
 * @history:
 */

public class SecurityUtilTest {
    private static final Logger LOGGER = new Logger();

    static KeyPair rsaPair;
    static KeyPair dsaPair;

    @BeforeAll
    public static void beforeClass() throws Exception {
        rsaPair = SecurityUtil.genKeyPair("RSA", 1024);
        dsaPair = SecurityUtil.genKeyPair("DSA", 512);
    }

    @Test
    public void testCipherException() throws Exception {
        byte[] data = null;
        byte[] bkey = null;
        String alg = "DES/ECB/PKCS5Padding";
        byte[] actual = null;
        try {
            // 密钥不能为空
            actual = SecurityUtil.symEncrypt(data, bkey, alg);
            assertTrue(false);
        } catch (IllegalArgumentException e) {
            assertNull(actual);
            assertTrue(true);// 参数异常
        }
        // 空数据加密
        bkey = "1234567".getBytes();
        actual = SecurityUtil.symEncrypt(data, bkey, alg);
        assertNull(actual);
        // 密钥长度不对
        bkey = "1234567".getBytes();
        data = "data".getBytes();
        try {
            actual = SecurityUtil.symEncrypt(data, bkey, alg);
            assertTrue(false);
        } catch (GeneralSecurityException e) {
            assertNull(actual);
        }
    }

    @Test
    public void testDesEncryptSuccess() throws Exception {
        byte[] data = null;
        byte[] bkey = null;
        String padding = "PKCS5Padding";
        byte[] actual = null;
        // ----------------ECB-------------------
        // 9字节加密后变成16字节
        bkey = "12345678".getBytes();
        data = "123456789".getBytes();
        actual = SecurityUtil.desEncrypt(data, bkey, padding);
        assertNotNull(actual);
        assertTrue(actual.length == 16);

        byte[] plain = SecurityUtil.desDecrypt(actual, bkey, padding);
        assertArrayEquals(data, plain);
    }

    @Test
    public void testPaddingDesedeKey() throws Exception {
        byte[] bkey = "123456781234567".getBytes();// 15字节密钥，异常
        try {
            SecurityUtil.paddingDesedeKey(bkey);
            assertTrue(false);// 执行到此就报错
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }
        bkey = "1234567812345678".getBytes();// 16字节密钥，拷贝为24字节
        byte[] bkey24 = SecurityUtil.paddingDesedeKey(bkey);
        assertNotNull(bkey24);
        assertEquals(24, bkey24.length);
        assertArrayEquals("123456781234567812345678".getBytes(), bkey24);
    }

    @Test
    public void testDesedeEncryptSuccess() throws Exception {
        byte[] data = "123456789".getBytes();
        byte[] bkey = "1234567812345678".getBytes();
        String padding = "PKCS5Padding";
        byte[] actual = null;
        // 9字节加密后变成16字节
        actual = SecurityUtil.desedeEncrypt(data, bkey, padding);
        assertNotNull(actual);
        assertTrue(actual.length == 16);

        byte[] plain = SecurityUtil.desedeDecrypt(actual, bkey, padding);
        assertArrayEquals(data, plain);
    }

    @Test
    public void testAesEncryptSuccess() throws Exception {
        byte[] data = "123456789".getBytes();
        byte[] bkey = "1234567812345678".getBytes();
        String mode = "ECB";
        // mode = "CBC";
        String padding = "PKCS5Padding";
        byte[] actual = null;

        // 9字节加密后变成16字节
        actual = SecurityUtil.aesEncrypt(data, bkey, mode, padding);
        assertNotNull(actual);
        assertTrue(actual.length == 16);

        byte[] plain = SecurityUtil.aesDecrypt(actual, bkey, mode, padding);
        assertArrayEquals(data, plain);
    }

    @Test
    public void testGenKeyPair() throws Exception {
        KeyPair pair = SecurityUtil.genKeyPair("RSA", 1024);
        assertNotNull(pair.getPrivate());
        assertNotNull(pair.getPublic());
        assertEquals("RSA", pair.getPrivate().getAlgorithm());
        assertEquals("RSA", pair.getPublic().getAlgorithm());

        pair = SecurityUtil.genKeyPair("RSA", 2048);
        assertNotNull(pair.getPrivate());
        assertNotNull(pair.getPublic());
        assertEquals("RSA", pair.getPrivate().getAlgorithm());
        assertEquals("RSA", pair.getPublic().getAlgorithm());
        System.out.println("privateKey:" + Base64.encodeBase64String(pair.getPrivate().getEncoded()));
        System.out.println("publicKey: " + Base64.encodeBase64String(pair.getPublic().getEncoded()));

        pair = SecurityUtil.genKeyPair("DSA", 1024);
        assertNotNull(pair.getPrivate());
        assertNotNull(pair.getPublic());
        assertEquals("DSA", pair.getPrivate().getAlgorithm());
        assertEquals("DSA", pair.getPublic().getAlgorithm());
    }

    @Test
    public void testReadObjectKey() throws Exception {
        String priKeyFileName = "prikey.bin";
        FileUtil.deleteQuietly(new File(priKeyFileName));// 消除影响

        // 私钥
        boolean ok = FileUtil.writeObject(priKeyFileName, rsaPair.getPrivate());
        assertTrue(ok);
        Key priKey = SecurityUtil.readObjectKey(priKeyFileName);
        assertNotNull(priKey);
        
        FileUtil.deleteQuietly(new File(priKeyFileName));// 消除影响
    }

    @Test
    public void testSignVerify() throws Exception {
        // RSA
        String[] algsRSA = { "MD5WITHRSA", "SHA1WITHRSA" };
        for (String alg : algsRSA) {
            testSignVerifyTest(rsaPair, alg);
            testSignVerifyPEMTest(rsaPair, alg);
        }
        // DSA
        String[] algsDSA = { "SHA1WITHDSA" };
        for (String alg : algsDSA) {
            testSignVerifyTest(dsaPair, alg);
            testSignVerifyPEMTest(dsaPair, alg);
        }
    }

    void testSignVerifyTest(KeyPair pair, String alg)
            throws GeneralSecurityException {
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            buf.append("1");
        }
        byte[] data = buf.toString().getBytes();
        byte[] bSign = SecurityUtil.sign(data, pair.getPrivate(), alg);
        assertTrue(bSign.length > 0);

        boolean result = SecurityUtil
                .verify(data, pair.getPublic(), alg, bSign);
        assertTrue(result);
    }

    void testSignVerifyPEMTest(KeyPair pair, String alg) throws Exception {
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            buf.append("1");
        }
        byte[] data = buf.toString().getBytes();
        String sign = SecurityUtil.signPEM(data, pair.getPrivate(), alg);
        assertTrue(sign.length() > 0);

        boolean result = SecurityUtil.verifyPEM(data, pair.getPublic(), alg,
                sign);
        assertTrue(result);
    }

    @Test
    public void testReadX509Cert() throws Exception {
        try {
            SecurityUtil.readX509Cert("notExist");
            assertTrue(false);
        } catch (FileNotFoundException e) {
            assertTrue(true);
        }
        X509Certificate cert;
        // cer
        cert = SecurityUtil.readX509Cert("com/umpay/commons/util/certs/test.cer");
        assertNotNull(cert.getPublicKey());
        // der
        cert = SecurityUtil.readX509Cert("com/umpay/commons/util/certs/test.der");
        assertNotNull(cert.getPublicKey());
        // crt test cache
        for (int i = 0; i < 10; i++) {
            cert = SecurityUtil.readX509Cert("com/umpay/commons/util/certs/test.crt");
            assertNotNull(cert.getPublicKey());
        }
        // 文件格式不对
        try {
            cert = SecurityUtil.readX509Cert("com/umpay/commons/util/certs/test.file");
            assertTrue(false);
        } catch (GeneralSecurityException e) {
            assertTrue(true);
        }
    }

    @Test
    public void testReadJKS() throws Exception {
        try {
            SecurityUtil.readJks("notExist", "");
            assertTrue(false);
        } catch (FileNotFoundException e) {
            assertTrue(true);
        }
        for (int i = 0; i < 10; i++) {
            KeyStore ks = SecurityUtil.readJks("com/umpay/commons/util/certs/test.jks",
                    "123456");
            assertNotNull(ks.getKey("serverkey", "123456".toCharArray()));
        }
    }

    @Test
    public void testReadPKCS12() throws Exception {
        try {
            SecurityUtil.readPKCS12("notExist", "");
            assertTrue(false);
        } catch (FileNotFoundException e) {
            assertTrue(true);
        }
        KeyStore ks = SecurityUtil.readPKCS12("com/umpay/commons/util/certs/test.pfx",
                "123456");
        assertNotNull(ks.getKey("serverkey", "123456".toCharArray()));
    }

    @Test
    public void testRsaEncryptDecrypt() throws Exception {
        byte[] bplain = "213456".getBytes();
        byte[] bcipher = SecurityUtil.rsaEncrypt(bplain, rsaPair.getPublic());
        assertEquals(128, bcipher.length);
        byte[] bplain2 = SecurityUtil.rsaDecrypt(bcipher, rsaPair.getPrivate());
        assertArrayEquals(bplain, bplain2);
    }

    @Test
    public void testPrintKeyStore() throws Exception {
        KeyStore ks = SecurityUtil.readJks("com/umpay/commons/util/certs/test.jks",
                "123456");
        SecurityUtil.printKeyStore(ks, "123456");
    }

    @Test
    public void testGenKey() throws NoSuchAlgorithmException {
//        KeyPair keyPair = SecurityUtil.genKeyPair("RSA");
        KeyPair keyPair = SecurityUtil.genKeyPair("RSA", 2048);
        PrivateKey privateKey = keyPair.getPrivate();
        PublicKey publicKey = keyPair.getPublic();
        byte[] priBody = privateKey.getEncoded();
        byte[] pubBody = publicKey.getEncoded();


        LOGGER.info("privateKey:", Base64Util.byteArrayToBase64(priBody));

        LOGGER.info("publicKey:", Base64Util.byteArrayToBase64(pubBody));

    }

//    @Test
//    public void testAesEncrypt() throws Exception {
//        String plainText = "test";
//        String sText = SecurityUtil.aesEncrypt(plainText);
//        LOGGER.info(sText);
//        String tText = SecurityUtil.aesDecrypt(sText);
//        LOGGER.info(tText);
//        Assert.assertEquals(plainText, tText);
//    }
//
//    @Test
//    public void testAesDencrypt() throws Exception {
//
//        String tText = SecurityUtil.aesDecrypt("bOzEZylOjJFoo5urnom7o5+tmRimwo/1vuPVIyxtCDs=");
//        LOGGER.info(tText);
//    }
}
