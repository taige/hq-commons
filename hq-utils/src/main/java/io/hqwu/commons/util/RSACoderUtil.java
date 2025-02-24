package io.hqwu.commons.util;

import javax.crypto.Cipher;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.security.spec.X509EncodedKeySpec;


/**
 * 加解密工具类.
 *
 * @version $Id: RSACoderUtil.java
 */
public class RSACoderUtil extends CoderUtil {

    /**
     * 加密算法
     */
    public static final String KEY_ALGORTHM = "RSA";

    /**
     * 具体加密算法，包括padding的方式
     */
    public static final String SPECIFIC_KEY_ALGORITHM = "RSA/ECB/PKCS1Padding";

    /**
     * 加签算法
     */
    public static final String SIGNATURE_ALGORITHM = "SHA1WithRSA";

    /**
     * 用RSA算法进行加密
     *
     * @param paramsString 要加密的字符串
     * @param charset      字符集
     * @param publicKey    加密使用的公钥
     * @return 加密结果
     * @throws Exception 异常
     */
    public static String encrypt(String paramsString, String charset, String publicKey)
            throws Exception {
        byte[] encryptedResult = RSACoderUtil.encryptByPublicKey(paramsString.getBytes(charset),
                publicKey, null);

        return Base64Util.byteArrayToBase64(encryptedResult);
    }

    /**
     * 用RSA算法进行加密
     *
     * @param paramsString   要加密的字符串
     * @param charset        字符集
     * @param publicKey      加密使用的公钥
     * @param encryptionType 加密算法类型
     * @return 加密结果
     * @throws Exception 异常
     */
    public static String encrypt(String paramsString, String charset, String publicKey,
                                 EncryptionModeEnum encryptionType) throws Exception {
        byte[] encryptedResult = RSACoderUtil.encryptByPublicKey(paramsString.getBytes(charset),
                publicKey, encryptionType);

        return Base64Util.byteArrayToBase64(encryptedResult);
    }

    /**
     * 把参数通过私钥进行加签.
     *
     * @param data       要加密的字符串
     * @param charset    字符串的编码
     * @param privateKey 商户的私钥
     * @return 加签后的数据
     * @throws Exception 异常
     */
    public static String sign(String data, String charset, String privateKey) throws Exception {
        byte[] dataInBytes = data.getBytes(charset);
        String signParams = RSACoderUtil.sign(dataInBytes, privateKey);//用应用的私钥加签.
        return signParams;
    }

    /**
     * 把参数通过私钥进行加签.
     *
     * @param signType   签名类型
     * @param data       要加密的字符串
     * @param charset    字符串的编码
     * @param privateKey 商户的私钥
     * @return 加签后的数据
     * @throws Exception 异常
     */
    public static String sign(SignTypeEnum signType, String data, String charset, String privateKey)
            throws Exception {
        byte[] dataInBytes = data.getBytes(charset);
        String signParams = RSACoderUtil.sign(signType, dataInBytes, privateKey);//用应用的私钥加签.
        return signParams;
    }

    /**
     * 解密数据
     *
     * @param data    base64格式的加密数据
     * @param key     私钥
     * @param charset 字符集
     * @return 解密后的明文
     * @throws Exception 异常
     */
    public static String decrypt(String data, String key, String charset) throws Exception {
        byte[] byte64 = Base64Util.base64ToByteArray(data);
        byte[] encryptedBytes = decryptByPrivateKey(byte64, key, null);
        return new String(encryptedBytes, charset);
    }

    /**
     * 解密数据
     *
     * @param data           base64格式的加密数据
     * @param key            私钥
     * @param charset        字符集
     * @param encryptionType 加密类型
     * @return 解密后的明文
     * @throws Exception 异常
     */
    public static String decrypt(String data, String key, String charset,
                                 EncryptionModeEnum encryptionType) throws Exception {
        byte[] byte64 = Base64Util.base64ToByteArray(data);
        byte[] encryptedBytes = decryptByPrivateKey(byte64, key, encryptionType);
        return new String(encryptedBytes, charset);
    }

    /**
     * 用私钥解密
     *
     * @param data           加密数据
     * @param key            密钥
     * @param encryptionType 加密类型
     * @return 解密后的byte数组
     * @throws Exception 异常
     */
    public static byte[] decryptByPrivateKey(byte[] data, String key,
                                             EncryptionModeEnum encryptionType) throws GeneralSecurityException {
        byte[] keyBytes = decryptBASE64(key);
        return decryptByPrivateKey(data, keyBytes, encryptionType);
    }

    public static byte[] decryptByPrivateKey(byte[] data, byte[] keyBytes,
                                             EncryptionModeEnum encryptionType) throws GeneralSecurityException {
        byte[] decryptedData = null;

        //对私钥解密 移动到上面的方法中
        //byte[] keyBytes = decryptBASE64(key);

        PKCS8EncodedKeySpec pkcs8EncodedKeySpec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORTHM);
        Key privateKey = keyFactory.generatePrivate(pkcs8EncodedKeySpec);
        //对数据解密
        Cipher cipher = Cipher.getInstance(SPECIFIC_KEY_ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, privateKey);

        // 解密时超过maxDecryptBlockSize字节就报错。为此采用分段解密的办法来解密
        int maxDecryptBlockSize;
        if (encryptionType != null) {
            maxDecryptBlockSize = getMaxDecryptBlockSizeByEncryptionType(encryptionType);
        } else {
            maxDecryptBlockSize = getMaxDecryptBlockSize(keyFactory, privateKey);
        }

        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        try {
            int dataLength = data.length;
            for (int i = 0; i < dataLength; i += maxDecryptBlockSize) {
                int decryptLength = Math.min(dataLength - i, maxDecryptBlockSize);
                byte[] doFinal = cipher.doFinal(data, i, decryptLength);
                bout.write(doFinal);
            }
            decryptedData = bout.toByteArray();
        } catch (IOException e) {
            throw new GeneralSecurityException(e);
        } finally {
            if (bout != null) {
                try {
                    bout.close();
                } catch (IOException e) {
                }
            }
        }

        return decryptedData;
    }

    /**
     * 用公钥加密
     *
     * @param data           加密数据
     * @param key            公钥
     * @param encryptionType 加密类型
     * @return 加密后的字节数组
     * @throws Exception 异常
     */
    public static byte[] encryptByPublicKey(byte[] data, String key,
                                            EncryptionModeEnum encryptionType) throws GeneralSecurityException {
        //对公钥base64解码
        byte[] keyBytes = decryptBASE64(key);
        return encryptByPublicKey(data, keyBytes, encryptionType);
    }

    public static byte[] encryptByPublicKey(byte[] data, byte[] keyBytes,
                                            EncryptionModeEnum encryptionType) throws GeneralSecurityException {
        byte[] encryptedData = null;

        //对公钥解密 移动到上面的方法中
        //byte[] keyBytes = decryptBASE64(key);
        //取公钥
        X509EncodedKeySpec x509EncodedKeySpec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORTHM);
        Key publicKey = keyFactory.generatePublic(x509EncodedKeySpec);

        //对数据解密
        Cipher cipher = Cipher.getInstance(SPECIFIC_KEY_ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);

        // 加密时超过maxEncryptBlockSize字节就报错。为此采用分段加密的办法来加密
        int maxEncryptBlockSize;
        if (encryptionType != null) {
            maxEncryptBlockSize = getMaxEncryptBlockSizeByEncryptionType(encryptionType);
        } else {
            maxEncryptBlockSize = getMaxEncryptBlockSize(keyFactory, publicKey);
        }

        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        try {
            int dataLength = data.length;
            for (int i = 0; i < data.length; i += maxEncryptBlockSize) {
                int encryptLength = Math.min(dataLength - i, maxEncryptBlockSize);
                byte[] doFinal = cipher.doFinal(data, i, encryptLength);
                bout.write(doFinal);
            }
            encryptedData = bout.toByteArray();
        } catch (IOException e) {
            throw new GeneralSecurityException(e);
        } finally {
            try {
                bout.close();
            } catch (IOException e) {
            }
        }
        return encryptedData;
    }

    /**
     * 用私钥对信息生成数字签名 SHA256WITHRSA
     *
     * @param data       加密数据
     * @param privateKey 私钥
     * @return 签名后的base64值
     * @throws Exception 异常
     */
    public static String sign(byte[] data, String privateKey) throws Exception {
        return sign(SignTypeEnum.SHA256WITHRSA, data, privateKey);
    }

    /**
     * 用私钥对信息生成数字签名
     *
     * @param signType   签名类型
     * @param data       加密数据
     * @param privateKey 私钥
     * @return 签名后的base64值
     * @throws Exception 异常
     */
    public static String sign(SignTypeEnum signType, byte[] data, String privateKey)
            throws Exception {
        byte[] keyBytes = decryptBASE64(privateKey);
        return sign(signType.getDesc(), data, keyBytes);
    }

    public static String sign(String signType, byte[] data, byte[] keyBytes)
            throws Exception {
        //解密私钥
        //byte[] keyBytes = decryptBASE64(privateKey);
        //构造PKCS8EncodedKeySpec对象
        PKCS8EncodedKeySpec pkcs8EncodedKeySpec = new PKCS8EncodedKeySpec(keyBytes);
        //指定加密算法
        KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORTHM);
        //取私钥匙对象
        PrivateKey privateKey2 = keyFactory.generatePrivate(pkcs8EncodedKeySpec);

        //用私钥对信息生成数字签名
        Signature signature = Signature.getInstance(signType);
        signature.initSign(privateKey2);
        signature.update(data);

        return encryptBASE64(signature.sign());
    }

    /**
     * 校验数字签名 SHA256WITHRSA
     *
     * @param data      加密数据
     * @param publicKey 公钥
     * @param sign      数字签名
     * @return 验签结果
     * @throws Exception 异常
     */
    public static boolean verify(byte[] data, String publicKey, String sign) throws Exception {
        return verify(SignTypeEnum.SHA256WITHRSA, data, publicKey, sign);
    }

    /**
     * 校验数字签名
     *
     * @param signType  签名类型
     * @param data      加密数据
     * @param publicKey 公钥
     * @param sign      数字签名
     * @return 验签结果
     * @throws Exception 异常
     */
    public static boolean verify(SignTypeEnum signType, byte[] data, String publicKey, String sign)
            throws Exception {
        byte[] keyBytes = decryptBASE64(publicKey);
        return verify(signType.getDesc(), data, keyBytes, sign);
    }

    public static boolean verify(String signType, byte[] data, byte[] keyBytes, String sign)
            throws Exception {
        //解密公钥
        //byte[] keyBytes = decryptBASE64(publicKey);
        //构造X509EncodedKeySpec对象
        X509EncodedKeySpec x509EncodedKeySpec = new X509EncodedKeySpec(keyBytes);
        //指定加密算法
        KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORTHM);
        //取公钥匙对象
        PublicKey publicKey2 = keyFactory.generatePublic(x509EncodedKeySpec);

        Signature signature = Signature.getInstance(signType);
        signature.initVerify(publicKey2);
        signature.update(data);
        //验证签名是否正常
        return signature.verify(decryptBASE64(sign));
    }


    /**
     * 获取每次加密的最大长度
     *
     * @param keyFactory KeyFactory
     * @param key        公钥
     * @return 单词加密最大长度
     * @throws Exception 异常
     */
    private static int getMaxEncryptBlockSize(KeyFactory keyFactory, Key key) throws GeneralSecurityException {
        //默认先设置成RSA1024的最大加密长度
        int maxLength = 117;
        try {
            RSAPublicKeySpec publicKeySpec = keyFactory.getKeySpec(key, RSAPublicKeySpec.class);
            int keyLength = publicKeySpec.getModulus().bitLength();
            maxLength = keyLength / 8 - 11;
        } catch (GeneralSecurityException e) {
            throw e;
        }
        return maxLength;
    }

    /**
     * 根据加密算法类型获取单次最大加密长度
     *
     * @param encryptionType
     * @return
     */
    private static int getMaxEncryptBlockSizeByEncryptionType(EncryptionModeEnum encryptionType) {
        if (encryptionType == EncryptionModeEnum.RSA1024) {
            return 1024 / 8 - 11;
        } else if (encryptionType == EncryptionModeEnum.RSA2048) {
            return 2048 / 8 - 11;
        }

        return 1024 / 8 - 11;
    }

    /***
     * 获取每次解密最大长度
     *
     * @param keyFactory KeyFactory
     * @param key        私钥
     * @return 单次解密最大长度
     * @throws Exception 异常
     */
    private static int getMaxDecryptBlockSize(KeyFactory keyFactory, Key key) throws GeneralSecurityException {
        //默认先设置成RSA1024的最大解密长度
        int maxLength = 128;
        try {
            RSAPrivateKeySpec publicKeySpec = keyFactory.getKeySpec(key, RSAPrivateKeySpec.class);
            int keyLength = publicKeySpec.getModulus().bitLength();
            maxLength = keyLength / 8;
        } catch (GeneralSecurityException e) {
            throw e;
        }
        return maxLength;
    }

    /**
     * @param encryptionType
     * @return
     */
    private static int getMaxDecryptBlockSizeByEncryptionType(EncryptionModeEnum encryptionType) {
        if (encryptionType == EncryptionModeEnum.RSA1024) {
            return 1024 / 8;
        } else if (encryptionType == EncryptionModeEnum.RSA2048) {
            return 2048 / 8;
        }

        return 1024 / 8;
    }

}
