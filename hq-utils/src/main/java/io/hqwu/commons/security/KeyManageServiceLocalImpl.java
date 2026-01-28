package io.hqwu.commons.security;

import io.hqwu.commons.util.SecurityUtil;
import org.apache.commons.codec.binary.Base64;

import java.security.GeneralSecurityException;

/**
 * {@link KeyManageService} 的本地实现类。
 * <p>
 * 该类主要负责提供本地化的密钥管理与加解密服务。它通过预定义的全局主密钥，
 * 配合 {@link SecurityUtil} 工具类实现数据的安全加密与解密操作。
 * 适用于需要对敏感信息进行基础加解密处理的业务场景。
 * </p>
 *
 * @author Wu, Hongqiang
 * @see KeyManageService
 * @see SecurityUtil
 * @since 2018/6/8
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
