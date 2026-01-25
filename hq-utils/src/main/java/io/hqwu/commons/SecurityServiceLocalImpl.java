package io.hqwu.commons;

import io.hqwu.commons.util.Logger;
import io.hqwu.commons.util.RSACoderUtil;
import io.hqwu.commons.util.SecurityUtil;
import io.hqwu.commons.util.StringUtil;
import org.apache.commons.codec.binary.Base64;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 安全服务本地实现类。
 *
 * <p>该类实现了 {@link SecurityService} 接口，集成了多种安全算法与工具类（如 {@link SecurityUtil} 和 {@link RSACoderUtil}），
 * 为系统提供统一的安全服务能力。主要功能包括：
 * <ul>
 *   <li>对称加密：提供 AES 算法的加解密功能。</li>
 *   <li>非对称加密：提供 RSA 算法的公钥加密与私钥解密。</li>
 *   <li>数字签名：支持 SHA256withRSA 等算法的签名生成与验签。</li>
 *   <li>消息摘要：支持 HMAC 算法的消息认证码计算。</li>
 *   <li>密钥管理：支持 AES 密钥及 RSA 密钥对的生成，并配合 {@link KeyManageService} 完成密钥的解密与本地缓存。</li>
 * </ul>
 *
 * @author taige
 * @since 2018/5/30
 */
public class SecurityServiceLocalImpl implements SecurityService, ApplicationContextAware {
    private static final Logger LOGGER = new Logger();

    private ApplicationContext applicationContext;
    private KeyManageService keyManageService;

    private boolean cacheKey;

    /**
     * 密钥缓存
     */
    private ConcurrentHashMap<String, byte[]> cachedKeys = new ConcurrentHashMap<String, byte[]>();

    public SecurityServiceLocalImpl() {
        cacheKey = true;
    }

    public SecurityServiceLocalImpl(boolean cacheKey) {
        this.cacheKey = cacheKey;
    }

    private String[] toModeAndPadding(String alg) {
        String[] modeAndPadding = new String[] {null, null};
        if (! StringUtil.isBlank(alg)) {
            String[] a = alg.split("/");
            modeAndPadding[0] = a[0];
            if (a.length > 1) {
                modeAndPadding[1] = a[1];
            }
        }
        return modeAndPadding;
    }

    private byte[] decodeKey(String key) throws GeneralSecurityException {
        if (cacheKey && cachedKeys.containsKey(key)) {
            return cachedKeys.get(key);
        }
        byte[] bkey = getKeyManageService().decrypt(key);
        cachedKeys.put(key, bkey);
        return bkey;
    }

    private String encodeKey(byte[] key) throws GeneralSecurityException {
        String b64key = getKeyManageService().encrypt(key);
        cachedKeys.put(b64key, key);
        return b64key;
    }

    public void setKeyManageService(KeyManageService keyManageService) {
        this.keyManageService = keyManageService;
    }

    protected KeyManageService getKeyManageService() {
        if (this.keyManageService == null) {
            if (this.applicationContext != null) {
                try {
                    Map<String, KeyManageService> map = this.applicationContext.getBeansOfType(KeyManageService.class);
                    for (Map.Entry<String, KeyManageService> entry: map.entrySet()) {
                        LOGGER.debug("getBean(KeyManageService.class): [", entry.getKey(), "]=", entry.getValue().getClass().getName());
                        if (entry.getValue() instanceof KeyManageServiceLocalImpl) {
                            this.keyManageService = entry.getValue();
                        } else {
                            //尽量选远程实现
                            this.keyManageService = entry.getValue();
                            break;
                        }
                    }
                } catch (BeansException e) {
                    LOGGER.warn("getBean(KeyManageService.class) error: ", e);
                    this.keyManageService = new KeyManageServiceLocalImpl();
                }
            }
            if (this.keyManageService == null) {
                this.keyManageService = new KeyManageServiceLocalImpl();
            }
        }
        return this.keyManageService;
    }

    @Override
    public String encryptByAES(byte[] data, String keyAlias, String alg) throws GeneralSecurityException, IllegalArgumentException {
        String[] modeAndPadding = toModeAndPadding(alg);
        byte[] cipher = SecurityUtil.aesEncrypt(data, decodeKey(keyAlias), modeAndPadding[0], modeAndPadding[1]);
        return Base64.encodeBase64String(cipher);
    }

    @Override
    public byte[] decryptByAES(byte[] cipher, String keyAlias, String alg) throws GeneralSecurityException, IllegalArgumentException {
        String[] modeAndPadding = toModeAndPadding(alg);
        return SecurityUtil.aesDecrypt(cipher, decodeKey(keyAlias), modeAndPadding[0], modeAndPadding[1]);
    }

    @Override
    public byte[] decryptByAES(String cipher, String keyAlias, String alg) throws GeneralSecurityException, IllegalArgumentException {
        byte[] data;
        if (Base64.isBase64(cipher)) {
            data = Base64.decodeBase64(cipher);
        } else {
            data = cipher.getBytes();
        }
        return decryptByAES(data, keyAlias, alg);
    }

    @Override
    public String encryptByPublicKey(byte[] data, String publicKey) throws GeneralSecurityException, IllegalArgumentException {
        byte[] cipher = RSACoderUtil.encryptByPublicKey(data, Base64.isBase64(publicKey) ? Base64.decodeBase64(publicKey) : publicKey.getBytes(), null);
        return Base64.encodeBase64String(cipher);
    }

    @Override
    public byte[] decryptByPrivateKey(byte[] cipher, String privateKeyAlias) throws GeneralSecurityException, IllegalArgumentException {
        return RSACoderUtil.decryptByPrivateKey(cipher, decodeKey(privateKeyAlias), null);
    }

    @Override
    public byte[] decryptByPrivateKey(String cipher, String privateKeyAlias) throws GeneralSecurityException, IllegalArgumentException {
        byte[] data;
        if (Base64.isBase64(cipher)) {
            data = Base64.decodeBase64(cipher);
        } else {
            data = cipher.getBytes();
        }
        return decryptByPrivateKey(data, privateKeyAlias);
    }

    @Override
    public String sign(byte[] data, String privateKeyAlias, String signType) throws Exception {
        return RSACoderUtil.sign(signType == null ? "SHA256withRSA" : signType, data, decodeKey(privateKeyAlias));
    }

    @Override
    public boolean verify(byte[] data, String publicKey, String sign, String signType) throws Exception {
        return RSACoderUtil.verify(signType == null ? "SHA256withRSA" : signType, data, Base64.isBase64(publicKey) ? Base64.decodeBase64(publicKey) : publicKey.getBytes(), sign);
    }

    @Override
    public String encryptKey(String key) throws GeneralSecurityException {
        byte[] bkey;
        if (Base64.isBase64(key)) {
            bkey = Base64.decodeBase64(key);
        } else {
            bkey = key.getBytes();
        }
        return encodeKey(bkey);
    }

    @Override
    public String hmac(byte[] data, String keyAlias, String alg) throws GeneralSecurityException {
        SecretKey key = new SecretKeySpec(decodeKey(keyAlias), alg);
        Mac mac = Mac.getInstance(alg);
        mac.init(key);
        byte[] _hmac = mac.doFinal(data);
        return Base64.encodeBase64String(_hmac);
    }

    @Override
    public String generateKey(String keyType, int keySize) throws GeneralSecurityException {
        KeyGenerator keyGen = KeyGenerator.getInstance(keyType);
        if (keySize <= 0) {
            keyGen.init(new SecureRandom());
        } else {
            keyGen.init(keySize, new SecureRandom());
        }
        SecretKey key = keyGen.generateKey();
        return encodeKey(key.getEncoded());
    }

    @Override
    public B64KeyPair generateKeyPair(String keyType, int keySize) throws GeneralSecurityException {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance(keyType);
        if (keySize <= 0) {
            if (keyType.toUpperCase().equals("EC")) {
                keySize = 571;
            } else {
                keySize = 2048; // 缺省
            }
        }
        keyGen.initialize(keySize, new SecureRandom());
        KeyPair keyPair = keyGen.generateKeyPair();

        PrivateKey privateKey = keyPair.getPrivate();
        PublicKey publicKey = keyPair.getPublic();
        final String b64PrivateKey = encodeKey(privateKey.getEncoded());
        final String b64PublicKey = Base64.encodeBase64String(publicKey.getEncoded());

        return new B64KeyPair() {
            @Override
            public String getPrivateKey() {
                return b64PrivateKey;
            }

            @Override
            public String getPublicKey() {
                return b64PublicKey;
            }
        };
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
