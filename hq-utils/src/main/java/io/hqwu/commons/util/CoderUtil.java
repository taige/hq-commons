package io.hqwu.commons.util;

import java.security.MessageDigest;

/**
 * 编码与加密工具类。
 * <p>
 * 提供常用的数据处理与安全散列算法，主要功能包括：
 * <ul>
 *   <li>基于 {@link Base64Util} 的 Base64 编码与解码</li>
 *   <li>基于 {@link MessageDigest} 的 MD5 摘要计算</li>
 *   <li>基于 {@link MessageDigest} 的 SHA 摘要计算</li>
 * </ul>
 * 适用于数据传输加密、数字签名摘要生成及基础的数据转换场景。
 */
public class CoderUtil {
    public static final String KEY_SHA = "SHA";
    public static final String KEY_MD5 = "MD5";

    /**
     * BASE64解密
     * @param key
     * @return
     * @throws Exception
     */
    public static byte[] decryptBASE64(String key) {
        return Base64Util.base64ToByteArray(key);
    }

    /**
     * BASE64加密
     * @param key
     * @return
     * @throws Exception
     */
    public static String encryptBASE64(byte[] key) throws Exception {
        return Base64Util.byteArrayToBase64(key);
    }

    /**
     * MD5加密
     * @param data
     * @return
     * @throws Exception
     */
    public static byte[] encryptMD5(byte[] data) throws Exception {
        MessageDigest md5 = MessageDigest.getInstance(KEY_MD5);
        md5.update(data);
        return md5.digest();
    }

    /**
     * SHA加密
     * @param data
     * @return
     * @throws Exception
     */
    public static byte[] encryptSHA(byte[] data) throws Exception {
        MessageDigest sha = MessageDigest.getInstance(KEY_SHA);
        sha.update(data);
        return sha.digest();
    }
}
