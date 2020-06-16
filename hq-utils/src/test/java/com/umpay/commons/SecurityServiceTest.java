package com.umpay.commons;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.GeneralSecurityException;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Created with IntelliJ IDEA for unpay-common
 * User: taige
 * Date: 2018/6/1
 * Time: 上午10:54
 */
public class SecurityServiceTest {

    private static SecurityServiceLocalImpl securityService;
    private String aesKey;
    private String privateKey;
    private String publicKey;

    //mode: ECB|CBC|CFB|OFB

    @BeforeAll
    public static void before() throws GeneralSecurityException {
        securityService = new SecurityServiceLocalImpl();
//        initKeys();
    }

    private void initKeys(int keySize) throws GeneralSecurityException {
        if (aesKey == null) {
            aesKey = securityService.generateKey("AES", keySize);
            SecurityService.B64KeyPair keyPair = securityService.generateKeyPair("RSA", 2048);
            privateKey = keyPair.getPrivateKey();
            publicKey = keyPair.getPublicKey();
        }
    }

    @Test
    public void test_genKey() throws Exception {
        String key = securityService.generateKey("AES", 0);
        System.out.println(Hex.encodeHexString(Base64.decodeBase64(key)));

        String key128 = securityService.generateKey("AES", 128);
        System.out.println(Hex.encodeHexString(Base64.decodeBase64(key128)));

        String key256 = securityService.generateKey("AES", 256);
        System.out.println(Hex.encodeHexString(Base64.decodeBase64(key256)));

        String keyMd5 = securityService.generateKey("HmacMD5", 0);
        System.out.println(Hex.encodeHexString(Base64.decodeBase64(keyMd5)));

        String keySha1 = securityService.generateKey("HmacSHA1", 0);
        System.out.println(Hex.encodeHexString(Base64.decodeBase64(keySha1)));

        String keySha256 = securityService.generateKey("HmacSHA256", 0);
        System.out.println(Hex.encodeHexString(Base64.decodeBase64(keySha256)));
    }

    @Test
    public void test_encryptByAES() throws Exception {
        for (Integer keySize : Arrays.asList(128, 192, 256)) {
            initKeys(keySize);
            String plain = "hello world";
            for (int i = 0; i < 3; i++) {
                String enc = securityService.encryptByAES(plain.getBytes(), aesKey, "ECB");
                System.out.println(enc);
                byte[] dec = securityService.decryptByAES(enc, aesKey, "ECB");
                assertArrayEquals(plain.getBytes(), dec);
            }
            for (int i = 0; i < 3; i++) {
                String enc = securityService.encryptByAES(plain.getBytes(), aesKey, "CBC");
                System.out.println(enc);
                byte[] dec = securityService.decryptByAES(enc, aesKey, "CBC");
                assertArrayEquals(plain.getBytes(), dec);
            }
            {
                String enc = securityService.encryptByAES(plain.getBytes(), aesKey, "PCBC");
                byte[] dec = securityService.decryptByAES(enc, aesKey, "PCBC");
                assertArrayEquals(plain.getBytes(), dec);
            }
            {
                String enc = securityService.encryptByAES(plain.getBytes(), aesKey, "CFB");
                byte[] dec = securityService.decryptByAES(enc, aesKey, "CFB");
                assertArrayEquals(plain.getBytes(), dec);
            }
            {
                String enc = securityService.encryptByAES(plain.getBytes(), aesKey, "OFB");
                byte[] dec = securityService.decryptByAES(enc, aesKey, "OFB");
                assertArrayEquals(plain.getBytes(), dec);
            }
            {
                String enc = securityService.encryptByAES(plain.getBytes(), aesKey, "CTR");
                byte[] dec = securityService.decryptByAES(enc, aesKey, "CTR");
                assertArrayEquals(plain.getBytes(), dec);
            }
            {
                String enc = securityService.encryptByAES(plain.getBytes(), aesKey, "GCM");
                byte[] dec = securityService.decryptByAES(enc, aesKey, "GCM");
                assertArrayEquals(plain.getBytes(), dec);
            }
        }
    }

    @Test
    public void test_genKeyPairAndEncrypt() throws Exception {
        SecurityService.B64KeyPair keyPair = securityService.generateKeyPair("RSA", 2048);
        System.out.println(keyPair.getPublicKey());
        System.out.println(keyPair.getPrivateKey());
        String plain = "hello world";
        String enc = securityService.encryptByPublicKey(plain.getBytes(), keyPair.getPublicKey());
        byte[] dec = securityService.decryptByPrivateKey(enc, keyPair.getPrivateKey());
        assertArrayEquals(plain.getBytes(), dec);
    }

    @Test
    public void test_sign() throws Exception {
        initKeys(128);
        String plain = "hello world";
        String sign = securityService.sign(plain.getBytes(), privateKey, null);
        assertTrue(securityService.verify(plain.getBytes(), publicKey, sign, null));
    }

    @Test
    public void test_hmac() throws Exception {
        //expect = `echo -n "Hi There" | openssl dgst -hmac hello -sha256`
        test_hmac("HmacSHA256", "298f9e96028956e51a3598e84714c2cc49d91d1e319bd90aa17f237ed089b945");
        test_hmac("HmacSHA1", "ebd1357c0fafe0511bfe3275029f57c102d6488d");
        test_hmac("HmacMD5", "c7d5a054ba9d0932208f6feb4250d96b");
    }

    private void test_hmac(String alg, String expect) throws Exception {
        byte[] data = "Hi There".getBytes();

        byte[] bkey = "hello".getBytes();
        SecretKey sk = new SecretKeySpec(bkey, alg);

        Mac mac = Mac.getInstance(alg);
        mac.init(sk);
        byte[] result0 = mac.doFinal(data);

        String hmac0 = Base64.encodeBase64String(result0);

        System.out.println(hmac0);

        assertEquals(expect, Hex.encodeHexString(result0));

        String b64key = securityService.encryptKey(Base64.encodeBase64String(bkey));
        String hmac = securityService.hmac(data, b64key, alg);

        assertEquals(hmac, hmac0);
    }

    @Test
    public void test_getKeyManageService() throws Exception {
        SecurityServiceLocalImpl ss = new SecurityServiceLocalImpl();
        KeyManageService kms = ss.getKeyManageService();
        assertTrue(kms instanceof KeyManageServiceLocalImpl);
    }
}
