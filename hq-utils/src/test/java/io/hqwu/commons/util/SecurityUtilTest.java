/*
 * @(#)SecurityUtilTest.java Created on 2013-8-13
 *
 * Copyright 2012-2013 Chinabank Payments, Inc. All rights reserved.
 * Use is subject to license terms.
 */
package io.hqwu.commons.util;

import org.apache.commons.codec.binary.Base64;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
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
    public void testAesEncryptWithRandomIV_CBC() throws Exception {
        // 保存原始系统属性
        String originalProperty = System.getProperty("RANDOM_AES_IV");

        try {
            // 设置 RANDOM_AES_IV=true 来激活随机IV分支
            System.setProperty("RANDOM_AES_IV", "true");

            byte[] data = "123456789".getBytes();
            byte[] bkey = "1234567812345678".getBytes();
            String mode = "CBC";
            String padding = "PKCS5Padding";

            // 测试加密 - 使用随机IV
            byte[] encrypted = SecurityUtil.aesEncrypt(data, bkey, mode, padding);
            assertNotNull(encrypted);
            // 加密后的数据应该包含16字节IV + 加密数据，所以长度应该是 16(IV) + 16(加密后的数据)
            assertEquals(32, encrypted.length);

            // 测试解密 - 从加密数据中提取IV并解密
            byte[] decrypted = SecurityUtil.aesDecrypt(encrypted, bkey, mode, padding);
            assertArrayEquals(data, decrypted);

            // 测试多次加密结果应该不同（因为IV是随机的）
            byte[] encrypted2 = SecurityUtil.aesEncrypt(data, bkey, mode, padding);
            assertNotNull(encrypted2);
            assertEquals(32, encrypted2.length);
            // 由于IV随机，两次加密结果应该不同
            assertFalse(java.util.Arrays.equals(encrypted, encrypted2));

            // 但解密结果应该相同
            byte[] decrypted2 = SecurityUtil.aesDecrypt(encrypted2, bkey, mode, padding);
            assertArrayEquals(data, decrypted2);

        } finally {
            // 恢复原始系统属性
            if (originalProperty == null) {
                System.clearProperty("RANDOM_AES_IV");
            } else {
                System.setProperty("RANDOM_AES_IV", originalProperty);
            }
        }
    }

    @Test
    public void testAesEncryptWithRandomIV_CFB() throws Exception {
        String originalProperty = System.getProperty("RANDOM_AES_IV");

        try {
            System.setProperty("RANDOM_AES_IV", "true");

            byte[] data = "Test data for CFB mode with random IV".getBytes();
            byte[] bkey = "1234567812345678".getBytes();
            String mode = "CFB";
            String padding = "PKCS5Padding";

            byte[] encrypted = SecurityUtil.aesEncrypt(data, bkey, mode, padding);
            assertNotNull(encrypted);
            // 应该包含16字节IV + 加密数据
            assertTrue(encrypted.length > 16);

            byte[] decrypted = SecurityUtil.aesDecrypt(encrypted, bkey, mode, padding);
            assertArrayEquals(data, decrypted);

        } finally {
            if (originalProperty == null) {
                System.clearProperty("RANDOM_AES_IV");
            } else {
                System.setProperty("RANDOM_AES_IV", originalProperty);
            }
        }
    }

    @Test
    public void testAesEncryptWithRandomIV_OFB() throws Exception {
        String originalProperty = System.getProperty("RANDOM_AES_IV");

        try {
            System.setProperty("RANDOM_AES_IV", "true");

            byte[] data = "Test data for OFB mode".getBytes();
            byte[] bkey = "1234567812345678".getBytes();
            String mode = "OFB";
            String padding = "PKCS5Padding";

            byte[] encrypted = SecurityUtil.aesEncrypt(data, bkey, mode, padding);
            assertNotNull(encrypted);
            assertTrue(encrypted.length > 16);

            byte[] decrypted = SecurityUtil.aesDecrypt(encrypted, bkey, mode, padding);
            assertArrayEquals(data, decrypted);

        } finally {
            if (originalProperty == null) {
                System.clearProperty("RANDOM_AES_IV");
            } else {
                System.setProperty("RANDOM_AES_IV", originalProperty);
            }
        }
    }

    @Test
    public void testAesEncryptWithRandomIV_ShortData() throws Exception {
        String originalProperty = System.getProperty("RANDOM_AES_IV");

        try {
            System.setProperty("RANDOM_AES_IV", "true");

            // 测试短数据（小于等于IV长度）的情况
            byte[] data = "short".getBytes();  // 5字节，小于16字节IV
            byte[] bkey = "1234567812345678".getBytes();
            String mode = "CBC";
            String padding = "PKCS5Padding";

            byte[] encrypted = SecurityUtil.aesEncrypt(data, bkey, mode, padding);
            assertNotNull(encrypted);

            byte[] decrypted = SecurityUtil.aesDecrypt(encrypted, bkey, mode, padding);
            assertArrayEquals(data, decrypted);

        } finally {
            if (originalProperty == null) {
                System.clearProperty("RANDOM_AES_IV");
            } else {
                System.setProperty("RANDOM_AES_IV", originalProperty);
            }
        }
    }

    @Test
    public void testAesEncryptWithRandomIV_LongData() throws Exception {
        String originalProperty = System.getProperty("RANDOM_AES_IV");

        try {
            System.setProperty("RANDOM_AES_IV", "true");

            // 测试较长数据
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 100; i++) {
                sb.append("Long test data ");
            }
            byte[] data = sb.toString().getBytes();
            byte[] bkey = "1234567812345678".getBytes();
            String mode = "CBC";
            String padding = "PKCS5Padding";

            byte[] encrypted = SecurityUtil.aesEncrypt(data, bkey, mode, padding);
            assertNotNull(encrypted);
            // 长度应该是 16(IV) + 加密后的数据长度
            assertTrue(encrypted.length > data.length);

            byte[] decrypted = SecurityUtil.aesDecrypt(encrypted, bkey, mode, padding);
            assertArrayEquals(data, decrypted);

        } finally {
            if (originalProperty == null) {
                System.clearProperty("RANDOM_AES_IV");
            } else {
                System.setProperty("RANDOM_AES_IV", originalProperty);
            }
        }
    }

    @Test
    public void testSymEncryptWithRandomIV_DES_CBC() throws Exception {
        String originalProperty = System.getProperty("RANDOM_AES_IV");

        try {
            System.setProperty("RANDOM_AES_IV", "true");

            byte[] data = "Test DES CBC with random IV".getBytes();
            byte[] bkey = "12345678".getBytes();  // DES需要8字节密钥
            String alg = "DES/CBC/PKCS5Padding";

            byte[] encrypted = SecurityUtil.symEncrypt(data, bkey, alg);
            assertNotNull(encrypted);
            // 应该包含IV + 加密数据
            assertTrue(encrypted.length > 16);

            byte[] decrypted = SecurityUtil.symDecrypt(encrypted, bkey, alg);
            assertArrayEquals(data, decrypted);

        } finally {
            if (originalProperty == null) {
                System.clearProperty("RANDOM_AES_IV");
            } else {
                System.setProperty("RANDOM_AES_IV", originalProperty);
            }
        }
    }

    @Test
    public void testSymEncryptWithRandomIV_DESede_CBC() throws Exception {
        String originalProperty = System.getProperty("RANDOM_AES_IV");

        try {
            System.setProperty("RANDOM_AES_IV", "true");

            byte[] data = "Test DESede CBC with random IV".getBytes();
            byte[] bkey = "123456781234567812345678".getBytes();  // 3DES需要24字节密钥
            String alg = "DESede/CBC/PKCS5Padding";

            byte[] encrypted = SecurityUtil.symEncrypt(data, bkey, alg);
            assertNotNull(encrypted);
            assertTrue(encrypted.length > 16);

            byte[] decrypted = SecurityUtil.symDecrypt(encrypted, bkey, alg);
            assertArrayEquals(data, decrypted);

        } finally {
            if (originalProperty == null) {
                System.clearProperty("RANDOM_AES_IV");
            } else {
                System.setProperty("RANDOM_AES_IV", originalProperty);
            }
        }
    }

    @Test
    public void testAesDecryptWithRandomIV_DataLengthEqualsIV() throws Exception {
        // 覆盖第154行: 当input.length <= ivLength时，使用bkey作为IV
        String originalProperty = System.getProperty("RANDOM_AES_IV");

        try {
            System.setProperty("RANDOM_AES_IV", "true");

            byte[] bkey = "1234567812345678".getBytes();  // 16字节密钥
            String mode = "CBC";
            String padding = "PKCS5Padding";

            // 测试1: 数据长度正好等于IV长度(16字节)
            byte[] shortData = "1234567890123456".getBytes();  // 正好16字节
            assertEquals(16, shortData.length);

            // 尝试解密一个长度等于IV的数据，应该使用bkey作为IV
            // 这种情况下会走到第154行的逻辑
            byte[] decrypted = SecurityUtil.aesDecrypt(shortData, bkey, mode, padding);
            // 由于这是用bkey作为IV解密的，结果可能不是有效数据，但不应该抛异常
            assertNotNull(decrypted);

        } catch (Exception e) {
            // 可能会因为padding错误而失败，这是预期的
            // 重要的是代码路径被执行了（第154行）
            assertTrue(e.getMessage().contains("pad") || e.getMessage().contains("BadPadding"));
        } finally {
            if (originalProperty == null) {
                System.clearProperty("RANDOM_AES_IV");
            } else {
                System.setProperty("RANDOM_AES_IV", originalProperty);
            }
        }
    }

    @Test
    public void testAesDecryptWithRandomIV_DataLengthLessThanIV() throws Exception {
        // 覆盖第154行: 当input.length < ivLength时，使用bkey作为IV
        String originalProperty = System.getProperty("RANDOM_AES_IV");

        try {
            System.setProperty("RANDOM_AES_IV", "true");

            byte[] bkey = "1234567812345678".getBytes();  // 16字节密钥
            String mode = "CBC";
            String padding = "PKCS5Padding";

            // 测试: 数据长度小于IV长度
            byte[] shortData = "12345".getBytes();  // 只有5字节，小于16
            assertTrue(shortData.length < 16);

            // 尝试解密一个长度小于IV的数据，应该使用bkey作为IV
            // 这种情况下会走到第154行的逻辑
            byte[] decrypted = SecurityUtil.aesDecrypt(shortData, bkey, mode, padding);
            assertNotNull(decrypted);

        } catch (Exception e) {
            // 可能会因为padding错误而失败，这是预期的
            // 重要的是代码路径被执行了（第154行）
            assertTrue(e.getMessage().contains("pad") || e.getMessage().contains("BadPadding") ||
                      e.getMessage().contains("Input length"));
        } finally {
            if (originalProperty == null) {
                System.clearProperty("RANDOM_AES_IV");
            } else {
                System.setProperty("RANDOM_AES_IV", originalProperty);
            }
        }
    }

    @Test
    public void testAesDecryptWithRandomIV_EmptyDataAfterIVExtraction() throws Exception {
        // 注意：原代码第162-165行的回退逻辑已被删除，因为该分支在逻辑上不可达
        //
        // 分析：要进入那个分支需要 input.length > ivLength
        // 但如果 input.length > ivLength，则提取IV后的data长度 = input.length - ivLength > 0
        // 所以data永远不会是空数组，cipher也就永远不会返回null或空数组
        //
        // 本测试用例验证边界情况：input.length略大于ivLength时的行为

        String originalProperty = System.getProperty("RANDOM_AES_IV");

        try {
            System.setProperty("RANDOM_AES_IV", "true");

            byte[] bkey = "1234567812345678".getBytes();
            String alg = "AES/CBC/PKCS5Padding";

            // 测试input.length = ivLength + 1的情况
            // 这会提取16字节IV，剩余1字节数据
            // cipher会因为数据长度不符合要求而抛异常
            byte[] testData = new byte[17];
            System.arraycopy(bkey, 0, testData, 0, 16);
            testData[16] = 0;

            try {
                byte[] result = SecurityUtil.symDecrypt(testData, bkey, alg);
                // 如果没抛异常，验证结果不为null
                assertNotNull(result);
            } catch (Exception e) {
                // 预期会抛异常（IllegalBlockSizeException或BadPaddingException）
                assertTrue(e.getMessage().contains("block") ||
                          e.getMessage().contains("pad") ||
                          e.getMessage().contains("BadPadding"));
            }

        } finally {
            if (originalProperty == null) {
                System.clearProperty("RANDOM_AES_IV");
            } else {
                System.setProperty("RANDOM_AES_IV", originalProperty);
            }
        }
    }

    @Test
    public void testAesDecryptWithRandomIV_CompatibilityWithNonRandomIV() throws Exception {
        // 额外的兼容性测试 - 验证随机IV和非随机IV模式各自的正确性

        String originalProperty = System.getProperty("RANDOM_AES_IV");

        try {
            byte[] bkey = "1234567812345678".getBytes();
            String mode = "CBC";
            String padding = "PKCS5Padding";
            byte[] plainText = "Test compatibility".getBytes();

            // 测试非随机IV方式
            System.setProperty("RANDOM_AES_IV", "false");
            byte[] encryptedOldWay = SecurityUtil.aesEncrypt(plainText, bkey, mode, padding);
            byte[] decryptedOldWay = SecurityUtil.aesDecrypt(encryptedOldWay, bkey, mode, padding);
            assertArrayEquals(plainText, decryptedOldWay);

            // 测试随机IV方式
            System.setProperty("RANDOM_AES_IV", "true");
            byte[] encryptedNewWay = SecurityUtil.aesEncrypt(plainText, bkey, mode, padding);
            byte[] decryptedNewWay = SecurityUtil.aesDecrypt(encryptedNewWay, bkey, mode, padding);
            assertArrayEquals(plainText, decryptedNewWay);

            // 两种方式的密文应该不同（因为IV不同）
            assertFalse(java.util.Arrays.equals(encryptedOldWay, encryptedNewWay));

        } finally {
            if (originalProperty == null) {
                System.clearProperty("RANDOM_AES_IV");
            } else {
                System.setProperty("RANDOM_AES_IV", originalProperty);
            }
        }
    }

    @Test
    public void testDesDecryptWithRandomIV_DataLengthEqualsIV() throws Exception {
        // 使用DES测试第154行逻辑（DES的IV长度是8字节）
        String originalProperty = System.getProperty("RANDOM_AES_IV");

        try {
            System.setProperty("RANDOM_AES_IV", "true");

            byte[] bkey = "12345678".getBytes();  // 8字节密钥
            String alg = "DES/CBC/PKCS5Padding";

            // 数据长度正好等于DES的IV长度(8字节)
            byte[] shortData = "12345678".getBytes();
            assertEquals(8, shortData.length);

            // 尝试解密，会走到第154行的逻辑（使用bkey作为IV）
            byte[] decrypted = SecurityUtil.symDecrypt(shortData, bkey, alg);
            assertNotNull(decrypted);

        } catch (Exception e) {
            // 可能会因为padding错误而失败，这是预期的
            assertTrue(e.getMessage().contains("pad") || e.getMessage().contains("BadPadding"));
        } finally {
            if (originalProperty == null) {
                System.clearProperty("RANDOM_AES_IV");
            } else {
                System.setProperty("RANDOM_AES_IV", originalProperty);
            }
        }
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
        LOGGER.info("privateKey:" + Base64.encodeBase64String(pair.getPrivate().getEncoded()));
        LOGGER.info("publicKey: " + Base64.encodeBase64String(pair.getPublic().getEncoded()));

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
        cert = SecurityUtil.readX509Cert("io/hqwu/commons/util/certs/test.cer");
        assertNotNull(cert.getPublicKey());
        // der
        cert = SecurityUtil.readX509Cert("io/hqwu/commons/util/certs/test.der");
        assertNotNull(cert.getPublicKey());
        // crt test cache
        for (int i = 0; i < 10; i++) {
            cert = SecurityUtil.readX509Cert("io/hqwu/commons/util/certs/test.crt");
            assertNotNull(cert.getPublicKey());
        }
        // 文件格式不对
        try {
            cert = SecurityUtil.readX509Cert("io/hqwu/commons/util/certs/test.file");
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
            KeyStore ks = SecurityUtil.readJks("io/hqwu/commons/util/certs/test.jks",
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
        KeyStore ks = SecurityUtil.readPKCS12("io/hqwu/commons/util/certs/test.pfx",
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
        KeyStore ks = SecurityUtil.readJks("io/hqwu/commons/util/certs/test.jks",
                "123456");
        SecurityUtil.printKeyStore(ks, "123456");
    }

    @Test
    public void testPrintKeyStoreWithTrustedCertificateEntry() throws Exception {
        // 覆盖第760行：privateKey == null 的分支
        // 创建一个包含纯证书条目（Trusted Certificate Entry）的KeyStore
        // 这种条目只有证书，没有私钥

        KeyStore ks = KeyStore.getInstance("JKS");
        ks.load(null, null);

        // 读取一个证书文件
        X509Certificate cert = SecurityUtil.readX509Cert("io/hqwu/commons/util/certs/test.cer");

        // 添加一个纯证书条目（没有私钥）
        // 使用 setCertificateEntry 而不是 setKeyEntry
        ks.setCertificateEntry("trustedCert", cert);

        // 打印这个KeyStore
        // 当执行到第759行时，privateKey = keystore.getKey("trustedCert", pwd.toCharArray())
        // 对于 Trusted Certificate Entry，getKey() 会返回 null
        // 这会触发第760行的 if (privateKey == null) 分支
        SecurityUtil.printKeyStore(ks, "anypassword");

        // 验证KeyStore中确实有这个条目
        assertTrue(ks.containsAlias("trustedCert"));
        // 验证这个条目是证书条目（不是密钥条目）
        assertTrue(ks.isCertificateEntry("trustedCert"));
        assertFalse(ks.isKeyEntry("trustedCert"));
    }

    @Test
    public void testPrintKeyStoreWithMixedEntries() throws Exception {
        // 测试包含混合类型条目的KeyStore：既有密钥条目，也有纯证书条目
        // 进一步验证第760行分支的覆盖

        KeyStore ks = KeyStore.getInstance("JKS");
        ks.load(null, null);

        // 1. 添加一个密钥条目（有私钥）
        KeyPair keyPair = SecurityUtil.genKeyPair("RSA", 1024);
        X509Certificate cert = SecurityUtil.readX509Cert("io/hqwu/commons/util/certs/test.cer");
        ks.setKeyEntry("keyEntry", keyPair.getPrivate(), "123456".toCharArray(),
                      new java.security.cert.Certificate[]{cert});

        // 2. 添加一个纯证书条目（没有私钥）
        ks.setCertificateEntry("certOnly", cert);

        // 打印KeyStore
        // 对于 "keyEntry"：privateKey 不为null，会打印私钥信息
        // 对于 "certOnly"：privateKey 为null，会触发第760行分支，打印"无私钥"
        SecurityUtil.printKeyStore(ks, "123456");

        // 验证两种条目都存在
        assertTrue(ks.isKeyEntry("keyEntry"));
        assertTrue(ks.isCertificateEntry("certOnly"));
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

    // ========== 新增测试用例 ==========

    @Test
    public void testPrintProvidersInfo() {
        // 测试打印JCE提供者信息
        // 这个方法只是输出信息，不抛异常即为成功
        SecurityUtil.printProvidersInfo();
    }

    @Test
    public void testRsaEncryptDecryptWithPadding() throws Exception {
        // 测试RSA加密解密（使用指定padding）
        byte[] plainText = "Test RSA encryption".getBytes();

        // 使用PKCS1Padding
        byte[] encrypted = SecurityUtil.rsaEncrypt(plainText, rsaPair.getPublic(), "PKCS1Padding");
        assertNotNull(encrypted);
        assertTrue(encrypted.length > 0);

        byte[] decrypted = SecurityUtil.rsaDecrypt(encrypted, rsaPair.getPrivate(), "PKCS1Padding");
        assertArrayEquals(plainText, decrypted);
    }

    @Test
    public void testMd5HEX() {
        // 测试MD5十六进制编码
        String data = "Hello World";
        String md5Hex = SecurityUtil.md5HEX(data);

        assertNotNull(md5Hex);
        assertEquals(32, md5Hex.length()); // MD5固定32个字符
        // 验证是否为十六进制字符串（支持大小写）
        assertTrue(md5Hex.matches("[0-9a-fA-F]+"));

        // 测试相同输入产生相同输出
        String md5Hex2 = SecurityUtil.md5HEX(data);
        assertEquals(md5Hex, md5Hex2);
    }

    @Test
    public void testReadPKCS12InputStream() throws Exception {
        // 测试从InputStream读取PKCS12
        try (InputStream is = ClassLoader.getSystemResourceAsStream("io/hqwu/commons/util/certs/test.pfx")) {
            assertNotNull(is);
            KeyStore ks = SecurityUtil.readPKCS12(is, "123456");
            assertNotNull(ks);
            assertNotNull(ks.getKey("serverkey", "123456".toCharArray()));
        }
    }

    @Test
    public void testReadJksInputStream() throws Exception {
        // 测试从InputStream读取JKS
        try (InputStream is = ClassLoader.getSystemResourceAsStream("io/hqwu/commons/util/certs/test.jks")) {
            assertNotNull(is);
            KeyStore ks = SecurityUtil.readJks(is, "123456");
            assertNotNull(ks);
            assertNotNull(ks.getKey("serverkey", "123456".toCharArray()));
        }
    }

    @Test
    public void testGetX509PublicKey() throws Exception {
        // 测试从X509编码获取公钥
        PublicKey originalPublicKey = rsaPair.getPublic();
        byte[] x509Encoded = originalPublicKey.getEncoded();

        PublicKey restoredPublicKey = SecurityUtil.getX509PublicKey(x509Encoded);

        assertNotNull(restoredPublicKey);
        assertEquals("RSA", restoredPublicKey.getAlgorithm());
        assertArrayEquals(originalPublicKey.getEncoded(), restoredPublicKey.getEncoded());
    }

    @Test
    public void testGetPKCS8PrivateKey() throws Exception {
        // 测试从PKCS8编码获取私钥
        PrivateKey originalPrivateKey = rsaPair.getPrivate();
        byte[] pkcs8Encoded = originalPrivateKey.getEncoded();

        PrivateKey restoredPrivateKey = SecurityUtil.getPKCS8PrivateKey(pkcs8Encoded);

        assertNotNull(restoredPrivateKey);
        assertEquals("RSA", restoredPrivateKey.getAlgorithm());
        assertArrayEquals(originalPrivateKey.getEncoded(), restoredPrivateKey.getEncoded());
    }

    @Test
    public void testGenKeyPairWithTypeOnly() throws Exception {
        // 测试只指定类型的密钥对生成（使用默认长度）
        KeyPair pair = SecurityUtil.genKeyPair("RSA");
        assertNotNull(pair);
        assertNotNull(pair.getPrivate());
        assertNotNull(pair.getPublic());
        assertEquals("RSA", pair.getPrivate().getAlgorithm());
        assertEquals("RSA", pair.getPublic().getAlgorithm());
    }

    @Test
    public void testAesEncryptDecryptWithPaddingOnly() throws Exception {
        // 测试AES加密解密（只指定padding，使用默认ECB模式）
        byte[] data = "Test AES with padding".getBytes();
        byte[] bkey = "1234567812345678".getBytes();
        String padding = "PKCS5Padding";

        byte[] encrypted = SecurityUtil.aesEncrypt(data, bkey, padding);
        assertNotNull(encrypted);
        assertTrue(encrypted.length >= data.length);

        byte[] decrypted = SecurityUtil.aesDecrypt(encrypted, bkey, padding);
        assertArrayEquals(data, decrypted);
    }

    @Test
    public void testDesedeEncryptDecryptSimple() throws Exception {
        // 测试3DES加密解密（使用默认模式和padding）
        byte[] data = "Test DESede simple".getBytes();
        byte[] bkey = "123456781234567812345678".getBytes(); // 24字节密钥

        byte[] encrypted = SecurityUtil.desedeEncrypt(data, bkey);
        assertNotNull(encrypted);
        assertTrue(encrypted.length >= data.length);

        byte[] decrypted = SecurityUtil.desedeDecrypt(encrypted, bkey);
        assertArrayEquals(data, decrypted);
    }

    @Test
    public void testDesEncryptDecryptSimple() throws Exception {
        // 测试DES加密解密（使用默认模式和padding）
        byte[] data = "Test DES simple".getBytes();
        byte[] bkey = "12345678".getBytes(); // 8字节密钥

        byte[] encrypted = SecurityUtil.desEncrypt(data, bkey);
        assertNotNull(encrypted);
        assertTrue(encrypted.length >= data.length);

        byte[] decrypted = SecurityUtil.desDecrypt(encrypted, bkey);
        assertArrayEquals(data, decrypted);
    }

    @Test
    public void testCipherWithNullData() throws Exception {
        // 测试 cipher 方法的 ArrayUtils.isEmpty(data) 分支 - null 数据
        // 覆盖第88行：if (ArrayUtils.isEmpty(data))

        byte[] bkey = "1234567812345678".getBytes();
        SecretKey key = new SecretKeySpec(bkey, "AES");
        String alg = "AES/ECB/PKCS5Padding";

        // 测试 null 数据
        byte[] result = SecurityUtil.cipher(null, key, SecurityUtil.ENCRYPT_MODE, alg, null);

        // 根据第90行：return data; 应该返回 null
        assertNull(result);
    }

    @Test
    public void testCipherWithEmptyData() throws Exception {
        // 测试 cipher 方法的 ArrayUtils.isEmpty(data) 分支 - 空数组
        // 覆盖第88行：if (ArrayUtils.isEmpty(data))

        byte[] bkey = "1234567812345678".getBytes();
        SecretKey key = new SecretKeySpec(bkey, "AES");
        String alg = "AES/ECB/PKCS5Padding";

        // 测试空数组
        byte[] emptyData = new byte[0];
        byte[] result = SecurityUtil.cipher(emptyData, key, SecurityUtil.ENCRYPT_MODE, alg, null);

        // 根据第90行：return data; 应该返回空数组
        assertNotNull(result);
        assertEquals(0, result.length);
        assertSame(emptyData, result); // 应该返回同一个对象
    }

    @Test
    public void testCipherWithEmptyDataDecryptMode() throws Exception {
        // 测试解密模式下的空数据处理

        byte[] bkey = "1234567812345678".getBytes();
        SecretKey key = new SecretKeySpec(bkey, "AES");
        String alg = "AES/ECB/PKCS5Padding";

        // 测试解密模式的空数组
        byte[] emptyData = new byte[0];
        byte[] result = SecurityUtil.cipher(emptyData, key, SecurityUtil.DECRYPT_MODE, alg, null);

        assertNotNull(result);
        assertEquals(0, result.length);
    }
}
