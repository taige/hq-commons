package io.hqwu.commons;

import java.security.GeneralSecurityException;

/**
 * 安全服务接口，提供统一的加密、解密、签名验证及密钥管理功能。
 *
 * <p>该接口封装了常用的安全算法操作，主要包括：
 * <ul>
 *   <li><b>密钥管理：</b> 支持生成对称密钥（如 AES）和非对称密钥对（如 RSA）。生成的私钥或对称密钥通常以加密后的“别名”形式存在。</li>
 *   <li><b>对称加密：</b> 提供 AES 算法的加解密，支持指定加密模式（ECB/CBC/CFB/OFB）和填充方式（PKCS5Padding 等）。</li>
 *   <li><b>非对称加密：</b> 提供 RSA 公钥加密和私钥解密功能。</li>
 *   <li><b>数字签名：</b> 提供基于 RSA 的签名生成与验证，支持 SHA1withRSA、SHA256withRSA 等算法。</li>
 *   <li><b>消息认证：</b> 支持 HMAC 算法（如 HmacSHA256）计算。</li>
 * </ul>
 *
 * <p>所有输入输出的密钥、密文及签名通常采用 Base64 编码。
 *
 * @author taige
 * @see B64KeyPair
 * @since 2018/5/30
 */
public interface SecurityService {

    /**
     * Base64编码的公私钥对
     */
    public interface B64KeyPair {
        /**
         * 获取Base64编码的私钥
         * @return
         */
        public String getPrivateKey();

        /**
         * 获取Base64编码的公钥
         * @return
         */
        public String getPublicKey();
    }

    /**
     * 生成对称密钥别名（用主密钥加密的密钥）
     * @param keyType 密钥类型(AES、HmaSHA256...)
     * @param keySize 密钥长度(128/256/...)，输入0时取默认长度
     * @return 密钥别名，Base64编码
     * @throws GeneralSecurityException
     */
    public String generateKey(String keyType, int keySize) throws GeneralSecurityException;

    /**
     * 生成非对称密钥对（私钥用主密钥加密）
     * @param keyType 密钥类型(RSA/DSA...)
     * @param keySize 密钥长度(1024/2048/4096...)
     * @return 密钥对别名，Base64编码
     * @throws GeneralSecurityException
     */
    public B64KeyPair generateKeyPair(String keyType, int keySize) throws GeneralSecurityException;

    /**
     * AES加密
     * @param data 数据明文
     * @param keyAlias AES密钥别名（通过generateKey()或者encryptKey()方法返回的）
     * @param alg 加密算法，格式：加密模式/填充算法，输入 null 时，自动取默认算法(ECB/PKCS5Padding)
     *            加解密模式(ECB|CBC|CFB|OFB)
     *            填充算法(PKCS1Padding|PKCS5Padding|PKCS7Padding...)
     * @return 数据密文 base64编码
     * @throws GeneralSecurityException
     * @throws IllegalArgumentException
     */
    public String encryptByAES(byte[] data, String keyAlias, String alg)
            throws GeneralSecurityException, IllegalArgumentException;

    /**
     * AES解密
     * @param cipher 数据密文
     * @param keyAlias AES密钥别名（通过generateKey()或者encryptKey()方法返回的）
     * @param alg 加密算法，格式：加密模式/填充算法，输入 null 时，自动取默认算法(ECB/PKCS5Padding)
     *            加解密模式(ECB|CBC|CFB|OFB)
     *            填充算法(PKCS1Padding|PKCS5Padding|PKCS7Padding...)
     * @return 数据明文
     * @throws GeneralSecurityException
     * @throws IllegalArgumentException
     */
    public byte[] decryptByAES(byte[] cipher, String keyAlias, String alg)
            throws GeneralSecurityException, IllegalArgumentException;

    /**
     * AES解密base64编码的密文
     * @param cipher 数据密文 base64编码
     * @param keyAlias AES密钥别名（通过generateKey()或者encryptKey()方法返回的）
     * @param alg 加密算法，格式：加密模式/填充算法，输入 null 时，自动取默认算法(ECB/PKCS5Padding)
     *            加解密模式(ECB|CBC|CFB|OFB)
     *            填充算法(PKCS1Padding|PKCS5Padding|PKCS7Padding...)
     * @return 数据明文
     * @throws GeneralSecurityException
     * @throws IllegalArgumentException
     */
    public byte[] decryptByAES(String cipher, String keyAlias, String alg)
            throws GeneralSecurityException, IllegalArgumentException;

    /**
     * RSA公钥加密
     * @param data 明文
     * @param publicKey base64编码的公钥
     * @return 密文
     * @throws GeneralSecurityException
     * @throws IllegalArgumentException
     */
    public String encryptByPublicKey(byte[] data, String publicKey)
            throws GeneralSecurityException, IllegalArgumentException;

    /**
     * RSA私钥解密
     * @param cipher 密文
     * @param privateKeyAlias base64编码的私钥别名（通过generateKeyPair()获取或者encryptKey()返回的）
     * @return 明文
     * @throws GeneralSecurityException
     * @throws IllegalArgumentException
     */
    public byte[] decryptByPrivateKey(byte[] cipher, String privateKeyAlias)
            throws GeneralSecurityException, IllegalArgumentException;

    /**
     * RSA私钥解密base64编码的密文
     * @param cipher 密文
     * @param privateKeyAlias base64编码的私钥别名（通过generateKeyPair()获取或者encryptKey()返回的）
     * @return 明文
     * @throws GeneralSecurityException
     * @throws IllegalArgumentException
     */
    public byte[] decryptByPrivateKey(String cipher, String privateKeyAlias)
            throws GeneralSecurityException, IllegalArgumentException;

    /**
     * 用RSA私钥签名
     * @param data 待签名数据
     * @param privateKeyAlias base64编码的私钥别名（通过generateKeyPair()获取或者encryptKey()返回的）
     * @param signType 签名算法：SHA1withRSA、SHA256withRSA(null时默认)
     * @return base64编码的签名
     * @throws Exception
     */
    public String sign(byte[] data, String privateKeyAlias, String signType)
            throws Exception;

    /**
     * 用RSA公钥验签
     * @param data 待验签数据
     * @param publicKey base64编码的公钥
     * @param sign base64编码的签名
     * @param signType 签名算法：SHA1withRSA、SHA256withRSA(null时默认)
     * @return true: 验签成功; false: 验签失败
     * @throws Exception
     */
    public boolean verify(byte[] data, String publicKey, String sign, String signType)
            throws Exception;

    /**
     * 加密密钥
     * @param key 待加密的密钥
     * @return base64格式的加密密钥
     * @throws GeneralSecurityException
     */
    public String encryptKey(String key)
            throws GeneralSecurityException;

    /**
     * 计算HMAC
     * @param data
     * @param keyAlias base64编码的密钥别名
     * @param alg  mac算法：HmacMD5、HmacSHA1、HmacSHA256 ...
     * @return
     */
    public String hmac(byte[] data, String keyAlias, String alg)
            throws GeneralSecurityException;
}
