package io.hqwu.commons.security;

import java.security.GeneralSecurityException;

/**
 * 密钥管理服务接口。
 * <p>
 * 该接口定义了基于系统主密钥（Master Key）的数据加解密标准操作。
 * 主要用于保障敏感数据在存储或传输过程中的安全性，支持字节数组与 Base64 编码加密字符串之间的转换。
 * </p>
 *
 * @see java.security.GeneralSecurityException
 * @author Wu, Hongqiang
 * @since 2018/6/8
 */
public interface KeyManageService {

    /**
     * 用主密钥解密
     * @param cipher base64格式的加密数据
     * @return
     * @throws GeneralSecurityException
     */
    byte[] decrypt(String cipher) throws GeneralSecurityException;

    /**
     * 用主密钥加密
     * @param data
     * @return base64格式的加密数据
     * @throws GeneralSecurityException
     */
    String encrypt(byte[] data) throws GeneralSecurityException;

}
