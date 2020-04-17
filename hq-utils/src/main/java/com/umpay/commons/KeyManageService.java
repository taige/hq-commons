package com.umpay.commons;

import java.security.GeneralSecurityException;

/**
 * Created with IntelliJ IDEA for unpay-common
 * User: taige
 * Date: 2018/6/8
 * Time: 上午11:20
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
