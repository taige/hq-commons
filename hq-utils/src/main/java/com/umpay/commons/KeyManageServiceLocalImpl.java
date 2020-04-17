package com.umpay.commons;

import com.umpay.commons.util.SecurityUtil;
import org.apache.commons.codec.binary.Base64;

import java.security.GeneralSecurityException;

/**
 * Created with IntelliJ IDEA for unpay-common
 * User: taige
 * Date: 2018/6/8
 * Time: 上午11:22
 */
public class KeyManageServiceLocalImpl implements KeyManageService {

    private static final byte[] MASTERY_KEY;

    static {
        String masteryKey = System.getProperty("SECRET_KEY");
        if (masteryKey == null) {
            masteryKey = System.getenv("SECRET_KEY");
        }
        if (masteryKey == null) {
            masteryKey = "Common.UNPay.Com";
        }
        if (Base64.isBase64(masteryKey)) {
            MASTERY_KEY = Base64.decodeBase64(masteryKey);
        } else {
            MASTERY_KEY = masteryKey.getBytes();
        }
    }

    protected byte[] getMasterKey() {
        return MASTERY_KEY;
    }

    @Override
    public byte[] decrypt(String cipher) throws GeneralSecurityException {
        byte[] bkey;
        if (Base64.isBase64(cipher)) {
            bkey = Base64.decodeBase64(cipher);
        } else {
            bkey = cipher.getBytes();
        }
        return SecurityUtil.aesDecrypt(bkey, getMasterKey());
    }

    @Override
    public String encrypt(byte[] data) throws GeneralSecurityException {
        byte[] encKey = SecurityUtil.aesEncrypt(data, getMasterKey());
        return Base64.encodeBase64String(encKey);
    }

}
