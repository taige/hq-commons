package com.umpay.commons.util;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.Validate;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.InputStream;
import java.security.*;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.apache.commons.lang3.Validate.notNull;

/**
 * Description:信息安全工具类：摘要/对称加解密/非对称加解密(MD5/SHA/DES/DESede/AES)
 * <b>由于CBC实现较为复杂，且使用不是非常广泛，暂不实现
 *
 * @author shenjianlin <a href="mailto:ustbsjl@gmail.com">ustbsjl@gmail.com</a> <br>
 *         QQ: 79043549
 * @version 1.0 2013-8-12 加解密只实现ECB模式（较为常用）
 * @history
 */

public class SecurityUtil extends DigestUtils {
    private static final Logger LOGGER = new Logger();

    /**
     * 加密模式
     */
    public static final int ENCRYPT_MODE = Cipher.ENCRYPT_MODE;
    /**
     * 解密模式
     */
    public static final int DECRYPT_MODE = Cipher.DECRYPT_MODE;
    /**
     * 默认加密模式：ECB（电子密码本）
     */
    private static final String DEFAULT_MODE = "ECB";
    /**
     * 默认对称加密填充算法
     */
    private static final String DEFAULT_SYM_PADDING = "PKCS5Padding";
    /**
     * 默认非对称加密填充算法
     */
    private static final String DEFAULT_ASYM_PADDING = "PKCS1Padding";

    private static final int IV_LENGTH = 16;

    private static final int GCM_IV_LENGTH = 12;

    private static final int GCM_TAG_LENGTH = 16;

    public SecurityUtil(final String name) {
        super(name);
    }

    /**
     * 通用加解密方式
     *
     * @param data   原文
     * @param key    密钥
     * @param opmode 模式（ENCODE_MODE|DECRYPT_MODE）
     * @param alg    算法：算法/模式/填充算法，如DES/ECB/PKCS5Padding
     * @return 加密/解密结果。字节数组形式
     * @throws java.security.GeneralSecurityException 加解密出现错误时抛出
     * @throws IllegalArgumentException               参数不合法时抛出
     */
    public static byte[] cipher(byte[] data, Key key, int opmode, String alg, AlgorithmParameterSpec iv)
            throws GeneralSecurityException, IllegalArgumentException {
        long start = System.nanoTime();
        if (ArrayUtils.isEmpty(data)) {
            LOGGER.debug("#cipher empty data!");
            return data;// 无需加解密
        }
        Validate.isTrue(opmode == ENCRYPT_MODE || opmode == DECRYPT_MODE,
                "加密|解密");
        Validate.notNull(alg, "加解密算法不能为空");
        Cipher c1 = Cipher.getInstance(alg);
        if (iv == null) {
            c1.init(opmode, notNull(key, "empty key!"));
        } else {
            c1.init(opmode, notNull(key, "empty key!"), iv);// validate and init
        }
        byte[] result = c1.doFinal(data);
        long used = System.nanoTime() - start;
        logCipher(data, opmode, result, used);// debug 日志
        return result;
    }

    static void logCipher(byte[] data, int opmode, byte[] result, long used) {
        int maxToPrint = 128;
//        if (_log.isDebugEnabled()) {// 参数大于3个时，避免创建数组
            LOGGER.trace("#cipher %s [%s]-->[%s] used %s nano.",
                    opmode == ENCRYPT_MODE ? "encode" : "decode",
                    IOUtil.bcd(data, maxToPrint),
                    IOUtil.bcd(result, maxToPrint), Formatter.formatNS(used));// 最多输出maxToPrint个字节
//        }
    }

    private static byte[] symCipher(byte[] data, byte[] bkey, int opmode,
                                    String alg) throws GeneralSecurityException,
            IllegalArgumentException {
        Validate.isTrue(ArrayUtils.isNotEmpty(bkey), "密钥不能为空");
        Validate.notNull(alg, "加解密算法不能为空");
        Validate.isTrue(opmode == ENCRYPT_MODE || opmode == DECRYPT_MODE, "加密|解密");
        String[] algs = StringUtil.split(alg, '/');
        SecretKey key = new SecretKeySpec(bkey, algs[0]);
        AlgorithmParameterSpec algorithmParameterSpec = null;
        final byte[] input = Arrays.copyOf(data, data.length);
        byte[] iv = null;
        byte[] output = null;
        if (alg.toUpperCase().contains("GCM")) {
            if (opmode == ENCRYPT_MODE) {
                // 随机iv
                iv = new byte[GCM_IV_LENGTH];
                SecureRandom random = new SecureRandom();
                random.nextBytes(iv);
            } else {
                iv = Arrays.copyOf(input, GCM_IV_LENGTH);
//                System.arraycopy(data, 0, iv, 0, iv.length);
                data = Arrays.copyOfRange(input, iv.length, input.length);
            }
            algorithmParameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
        } else if (! alg.toUpperCase().contains("ECB")) {
            if (isRandomAesIV()) {
                if (opmode == ENCRYPT_MODE) {
                    iv = new byte[IV_LENGTH];
                    SecureRandom random = new SecureRandom();
                    random.nextBytes(iv);
                } else {
                    if (input.length <= IV_LENGTH) {
                        iv = bkey;
                    } else {
                        iv = Arrays.copyOf(input, IV_LENGTH);
//                    System.arraycopy(data, 0, iv, 0, iv.length);
                        data = Arrays.copyOfRange(input, iv.length, input.length);
                        algorithmParameterSpec = new IvParameterSpec(iv);
                        output = cipher(data, key, opmode, alg, algorithmParameterSpec);
                        if (output == null || output.length == 0) {
                            output = null;
                            iv = bkey;
                            algorithmParameterSpec = null;
                            data = input;
                        }
                    }
                }
                if (algorithmParameterSpec != null) {
                    algorithmParameterSpec = new IvParameterSpec(iv);
                }
            } else {
                algorithmParameterSpec = new IvParameterSpec(bkey); //使用CBC模式，需要一个向量iv
            }
        }
        if (output == null) {
            output = cipher(data, key, opmode, alg, algorithmParameterSpec);
        }
        if (iv != null && opmode == ENCRYPT_MODE) {
            // 把iv附在加密数据前面
            byte[] result = new byte[iv.length + output.length];
            System.arraycopy(iv, 0, result, 0, iv.length);
            System.arraycopy(output, 0, result, iv.length, output.length);
            return result;
        }
        return output;
    }

    /**
     * AES IV向量是否随机
     *  默认不随机（即IV=KEY）
     *  显示设置参数 RANDOM_AES_IV=true，打开随机IV
     * @return AES IV向量是否随机
     */
    private static boolean isRandomAesIV() {
        String random = System.getProperty("RANDOM_AES_IV");
        if (random == null) {
            random = System.getenv("RANDOM_AES_IV");
            if (random == null) {
                random = "false";
            }
        }
        if (random.equalsIgnoreCase("true")) {
            return true;
        }
        return false;
    }

    /**
     * 通用对称加密
     *
     * @param data 原文
     * @param bkey 密钥字节形式
     * @param alg  算法：算法/模式/填充算法，如DES/ECB/PKCS5Padding
     * @return 加密结果, 字节数组形式
     * @throws java.security.GeneralSecurityException 加解密出现错误时抛出
     * @throws IllegalArgumentException               参数不合法时抛出
     */
    public static byte[] symEncrypt(byte[] data, byte[] bkey, String alg)
            throws GeneralSecurityException, IllegalArgumentException {
        return symCipher(data, bkey, ENCRYPT_MODE, alg);
    }

    /**
     * 通用对称解密
     *
     * @param bcipher 原文
     * @param bkey    密钥字节形式
     * @param alg     算法：算法/模式/填充算法，如DES/ECB/PKCS5Padding
     * @return 解密结果, 字节数组形式
     * @throws java.security.GeneralSecurityException 加解密出现错误时抛出
     * @throws IllegalArgumentException               参数不合法时抛出
     */
    public static byte[] symDecrypt(byte[] bcipher, byte[] bkey, String alg)
            throws GeneralSecurityException, IllegalArgumentException {
        return symCipher(bcipher, bkey, DECRYPT_MODE, alg);
    }

    /**
     * DES加密
     *
     * @param data    明文
     * @param bkey    密钥字节形式
     * @param mode    加解密模式(ECB|CBC|CFB|OFB)
     * @param padding 填充算法(PKCS1Padding|PKCS5Padding|PKCS7Padding...)
     * @return 加密结果, 字节数组形式
     * @throws java.security.GeneralSecurityException 加解密出现错误时抛出
     * @throws IllegalArgumentException               参数不合法时抛出
     */
    public static byte[] desEncrypt(byte[] data, byte[] bkey, String mode,
                                    String padding) throws GeneralSecurityException,
            IllegalArgumentException {
        String alg = String.format("DES/%s/%s", mode, padding);
        return symCipher(data, bkey, ENCRYPT_MODE, alg);
    }

    /**
     * 采用默认模式（ECB）DES加密
     *
     * @see {@link #desEncrypt(byte[], byte[], String, String)}
     */
    public static byte[] desEncrypt(byte[] data, byte[] bkey, String padding)
            throws GeneralSecurityException, IllegalArgumentException {
        return desEncrypt(data, bkey, DEFAULT_MODE, padding);
    }

    /**
     * 采用默认模式（ECB）,默认填充（PKCS5Padding）的DES加密
     *
     * @see {@link #desEncrypt(byte[], byte[], String, String)}
     */
    public static byte[] desEncrypt(byte[] data, byte[] bkey)
            throws GeneralSecurityException, IllegalArgumentException {
        return desEncrypt(data, bkey, DEFAULT_MODE, DEFAULT_SYM_PADDING);
    }

    /**
     * DES解密
     *
     * @param bcipher 明文
     * @param bkey    密钥字节形式
     * @param mode    加解密模式(ECB|CBC|CFB|OFB)
     * @param padding 填充算法(PKCS1Padding|PKCS5Padding|PKCS7Padding...)
     * @return 解密结果, 字节数组形式
     * @throws java.security.GeneralSecurityException 加解密出现错误时抛出
     * @throws IllegalArgumentException               参数不合法时抛出
     */
    public static byte[] desDecrypt(byte[] bcipher, byte[] bkey, String mode,
                                    String padding) throws GeneralSecurityException,
            IllegalArgumentException {
        String alg = String.format("DES/%s/%s", mode, padding);
        return symCipher(bcipher, bkey, DECRYPT_MODE, alg);
    }

    /**
     * 采用默认模式（ECB）的DES解密
     *
     * @see {@link #desDecrypt(byte[], byte[], String, String)}
     */
    public static byte[] desDecrypt(byte[] bcipher, byte[] bkey, String padding)
            throws GeneralSecurityException, IllegalArgumentException {
        return desDecrypt(bcipher, bkey, DEFAULT_MODE, padding);
    }

    /**
     * 采用默认模式（ECB）,默认填充（PKCS5Padding）的DES解密
     *
     * @see {@link #desDecrypt(byte[], byte[], String, String)}
     */
    public static byte[] desDecrypt(byte[] bcipher, byte[] bkey)
            throws GeneralSecurityException, IllegalArgumentException {
        return desDecrypt(bcipher, bkey, DEFAULT_MODE, DEFAULT_SYM_PADDING);
    }

    /**
     * 填充3DES算法密钥 16->24
     *
     * @param bkey 3DES密钥，16字节或24字节
     * @return 24字节3DES密钥
     */
    static byte[] paddingDesedeKey(byte[] bkey) {
        Validate.notNull(bkey, "3DES密钥不能为空");
        if (bkey.length == 24) {
            return bkey;
        } else if (bkey.length == 16) {
            return IOUtil.joinBytes(bkey, bkey, 0, 8);
        }
        throw new IllegalArgumentException("3DES密钥只能为16字节/24字节");
    }

    /**
     * 3DES加密
     *
     * @param data    明文
     * @param bkey    密钥字节形式
     * @param mode    加解密模式(ECB|CBC|CFB|OFB)
     * @param padding 填充算法(PKCS1Padding|PKCS5Padding|PKCS7Padding...)
     * @return 加密结果, 字节数组形式
     * @throws java.security.GeneralSecurityException 加解密出现错误时抛出
     * @throws IllegalArgumentException               参数不合法时抛出
     */
    public static byte[] desedeEncrypt(byte[] data, byte[] bkey, String mode,
                                       String padding) throws GeneralSecurityException,
            IllegalArgumentException {
        // 特殊处理：如果密钥为16字节，则自动扩充为24字节
        byte[] bkey24 = paddingDesedeKey(bkey);
        String alg = String.format("DESede/%s/%s", mode, padding);
        return symCipher(data, bkey24, ENCRYPT_MODE, alg);
    }

    /**
     * 采用默认模式（ECB）3DES加密
     *
     * @see {@link #desedeEncrypt(byte[], byte[], String, String)}
     */
    public static byte[] desedeEncrypt(byte[] data, byte[] bkey, String padding)
            throws GeneralSecurityException, IllegalArgumentException {
        return desedeEncrypt(data, bkey, DEFAULT_MODE, padding);
    }

    /**
     * 采用默认模式（ECB）,默认填充（PKCS5Padding）的3DES加密
     *
     * @see {@link #desedeEncrypt(byte[], byte[], String, String)}
     */
    public static byte[] desedeEncrypt(byte[] data, byte[] bkey)
            throws GeneralSecurityException, IllegalArgumentException {
        return desedeEncrypt(data, bkey, DEFAULT_MODE, DEFAULT_SYM_PADDING);
    }

    /**
     * 3DES解密
     *
     * @param bcipher 明文
     * @param bkey    密钥字节形式
     * @param mode    加解密模式(ECB|CBC|CFB|OFB)
     * @param padding 填充算法(PKCS1Padding|PKCS5Padding|PKCS7Padding...)
     * @return 解密结果, 字节数组形式
     * @throws java.security.GeneralSecurityException 加解密出现错误时抛出
     * @throws IllegalArgumentException               参数不合法时抛出
     */
    public static byte[] desedeDecrypt(byte[] bcipher, byte[] bkey,
                                       String mode, String padding) throws GeneralSecurityException,
            IllegalArgumentException {
        // 特殊处理：如果密钥为16字节，则自动扩充为24字节
        byte[] bkey24 = paddingDesedeKey(bkey);
        String alg = String.format("DESede/%s/%s", mode, padding);
        return symCipher(bcipher, bkey24, DECRYPT_MODE, alg);
    }

    /**
     * 采用默认模式（ECB）的3DES解密
     *
     * @see {@link #desedeDecrypt(byte[], byte[], String, String)}
     */
    public static byte[] desedeDecrypt(byte[] bcipher, byte[] bkey,
                                       String padding) throws GeneralSecurityException,
            IllegalArgumentException {
        return desedeDecrypt(bcipher, bkey, DEFAULT_MODE, padding);
    }

    /**
     * 采用默认模式（ECB）,默认填充（PKCS5Padding）的3DES解密
     *
     * @see {@link #desedeDecrypt(byte[], byte[], String, String)}
     */
    public static byte[] desedeDecrypt(byte[] bcipher, byte[] bkey)
            throws GeneralSecurityException, IllegalArgumentException {
        return desedeDecrypt(bcipher, bkey, DEFAULT_MODE, DEFAULT_SYM_PADDING);
    }

    /**
     * AES加密
     *
     * @param data    明文
     * @param bkey    密钥字节形式
     * @param mode    加解密模式(ECB|CBC|CFB|OFB)
     * @param padding 填充算法(PKCS1Padding|PKCS5Padding|PKCS7Padding...)
     * @return 加密结果, 字节数组形式
     * @throws java.security.GeneralSecurityException 加解密出现错误时抛出
     * @throws IllegalArgumentException               参数不合法时抛出
     */
    public static byte[] aesEncrypt(byte[] data, byte[] bkey, String mode,
                                    String padding) throws GeneralSecurityException,
            IllegalArgumentException {
        String alg = String.format("AES/%s/%s", mode == null ? DEFAULT_MODE : mode, padding == null ? DEFAULT_SYM_PADDING : padding);
        return symCipher(data, bkey, ENCRYPT_MODE, alg);
    }

    /**
     * 采用默认模式（ECB）AES加密
     *
     * @see {@link #aesEncrypt(byte[], byte[], String, String)}
     */
    public static byte[] aesEncrypt(byte[] data, byte[] bkey, String padding)
            throws GeneralSecurityException, IllegalArgumentException {
        return aesEncrypt(data, bkey, DEFAULT_MODE, padding);
    }

    /**
     * 采用默认模式（ECB）,默认填充（PKCS5Padding）的AES加密
     *
     * @see {@link #aesEncrypt(byte[], byte[], String, String)}
     */
    public static byte[] aesEncrypt(byte[] data, byte[] bkey)
            throws GeneralSecurityException, IllegalArgumentException {
        return aesEncrypt(data, bkey, DEFAULT_MODE, DEFAULT_SYM_PADDING);
    }


    // 增加AES加密方式，固定密钥和向量


//    public static String aesDecrypt(String secretBase64) throws Exception {
//        byte[] pwdAesRawKey = getSecretKey();
//        SecretKeySpec key = new SecretKeySpec(pwdAesRawKey, "AES");
//        byte[] ivp = pwdAesRawKey;
//        IvParameterSpec iv = new IvParameterSpec(ivp);//使用CBC模式，需要一个向量iv，可增加加密算法的强度
//        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
//        cipher.init(Cipher.DECRYPT_MODE, key, iv);
//        byte[] decode = cipher.doFinal(Base64.decodeBase64(secretBase64));
//        return new String(decode, "UTF-8");
//    }
//
//    public static String aesEncrypt(String plainText) throws Exception {
//        byte[] pwdAesRawKey = getSecretKey();
//        SecretKeySpec key = new SecretKeySpec(pwdAesRawKey, "AES");
//        byte[] ivp = pwdAesRawKey;
//        IvParameterSpec iv = new IvParameterSpec(ivp);//使用CBC模式，需要一个向量iv，可增加加密算法的强度
//        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
//        cipher.init(Cipher.ENCRYPT_MODE, key, iv);
//
//        byte[] decode = cipher.doFinal(plainText.getBytes("UTF-8"));
//        return Base64.encodeBase64String(decode);
//    }

    /**
     * AES解密
     *
     * @param bcipher 密文
     * @param bkey    密钥字节形式
     * @param mode    加解密模式(ECB|CBC|CFB|OFB)
     * @param padding 填充算法(PKCS1Padding|PKCS5Padding|PKCS7Padding...)
     * @return 解密结果, 字节数组形式
     * @throws java.security.GeneralSecurityException 加解密出现错误时抛出
     * @throws IllegalArgumentException               参数不合法时抛出
     */
    public static byte[] aesDecrypt(byte[] bcipher, byte[] bkey, String mode,
                                    String padding) throws GeneralSecurityException,
            IllegalArgumentException {
        String alg = String.format("AES/%s/%s", mode == null ? DEFAULT_MODE : mode, padding == null ? DEFAULT_SYM_PADDING : padding);
        return symCipher(bcipher, bkey, DECRYPT_MODE, alg);
    }

    /**
     * 采用默认模式（ECB）的AES解密
     *
     * @see {@link #aesDecrypt(byte[], byte[], String, String)}
     */
    public static byte[] aesDecrypt(byte[] bcipher, byte[] bkey, String padding)
            throws GeneralSecurityException, IllegalArgumentException {
        return aesDecrypt(bcipher, bkey, DEFAULT_MODE, padding);
    }

    /**
     * 采用默认模式（ECB）,默认填充（PKCS5Padding）的AES解密
     *
     * @see {@link #aesDecrypt(byte[], byte[], String, String)}
     */
    public static byte[] aesDecrypt(byte[] bcipher, byte[] bkey)
            throws GeneralSecurityException, IllegalArgumentException {
        return aesDecrypt(bcipher, bkey, DEFAULT_MODE, DEFAULT_SYM_PADDING);
    }

    // ----------------------证书构建---------------------------

    /**
     * 产生非对称密钥
     *
     * @param keyType 密钥类型 （RSA | DSA）
     * @param keySize 密钥长度(512 | 1024 | 2048)
     * @return 公私钥对
     * @throws java.security.NoSuchAlgorithmException 不支持的算法时抛出
     */
    public static final KeyPair genKeyPair(String keyType, int keySize)
            throws NoSuchAlgorithmException {
        long start = System.currentTimeMillis();
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance(keyType);
        if (keySize <= 0) {
            keySize = 1024; // 缺省
        }
        keyGen.initialize(keySize, new SecureRandom());
        KeyPair keyPair = keyGen.generateKeyPair();
        long cost = System.currentTimeMillis() - start;
//        if (_log.isDebugEnabled()) {// 参数大于3个时，避免创建数组
        LOGGER.debugf("# genKeyPair(%s,%d)...use %d ms.", keyType, keySize,
                    cost);
//        }
        return keyPair;
    }

    /**
     * @see #genKeyPair(String, int)
     */
    public static final KeyPair genKeyPair(String keyType)
            throws NoSuchAlgorithmException {
        return genKeyPair(keyType, 1024);
    }

    /**
     * 根据PKCS8格式的字节流转换为RSA私钥
     *
     * @param pkcs8 PKCS8格式的字节流
     * @return RSA私钥
     * @throws java.security.GeneralSecurityException
     */
    public static final PrivateKey getPKCS8PrivateKey(byte[] pkcs8)
            throws GeneralSecurityException {
        PKCS8EncodedKeySpec ks = new PKCS8EncodedKeySpec(pkcs8);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return kf.generatePrivate(ks); // 获取私钥
    }

    /**
     * 将X509格式的字节流转成RSA公钥
     *
     * @param x509 X509格式的字节流
     * @return RSA公钥
     * @throws java.security.GeneralSecurityException
     */
    public static final PublicKey getX509PublicKey(byte[] x509)
            throws GeneralSecurityException {
        X509EncodedKeySpec ks = new X509EncodedKeySpec(x509);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return kf.generatePublic(ks); // 获取公钥
    }

    /**
     * 读取X509标准的证书
     *
     * @param is 输入流
     * @return X509Certificate
     * @throws java.security.GeneralSecurityException
     */
    public static final X509Certificate readX509Cert(InputStream is)
            throws GeneralSecurityException {
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        X509Certificate interCert = (X509Certificate) cf
                .generateCertificate(is);
        return interCert;
    }

    /**
     * 效率：以文件名为Key缓存证书
     */
    private static final ConcurrentMap<String, X509Certificate> certs = new ConcurrentHashMap<String, X509Certificate>();

    /**
     * 读取X509标准的证书,不保证每个文件只读取一次，初始化并发读取可能读取多次
     *
     * @param filename 证书文件
     * @return X509Certificate
     * @throws java.security.GeneralSecurityException
     */
    public static final X509Certificate readX509Cert(String filename)
            throws IOException, GeneralSecurityException {
        X509Certificate cached = certs.get(filename);
        if (cached != null) {// 文件已经读取过，则直接返回上次结果
            LOGGER.debugf("# readX509Cert(%s) return cached instance.", filename);
            return cached;
        }
        InputStream is = null;
        try {
            is = FileUtil.toInputStream(filename);
            X509Certificate created = readX509Cert(is);
            certs.putIfAbsent(filename, created);
            LOGGER.debugf("# readX509Cert(%s) return new instance.", filename);
            return created;
        } finally {
            IOUtil.close(is, "readX509Cert");
        }
    }

    /**
     * 读取KeyStore
     *
     * @param filename KeyStore文件名
     * @param pwd      KeyStore 密码
     * @param ksType   KeyStore类型（JKS|JceKS|PKCS12|BKS|UBER），一般为JKS
     * @return KeyStore密钥库
     * @throws java.io.IOException                    读取文件异常时抛出
     * @throws java.security.GeneralSecurityException 读取密钥失败时抛出
     */
    public static final KeyStore readKeyStore(String filename, String pwd,
                                              String ksType) throws IOException, GeneralSecurityException {
        KeyStore cached = keyStores.get(filename);
        if (cached != null) {// 文件已经读取过，则直接返回上次结果
            LOGGER.debugf("# readKeyStore(%s) return cached instance.", filename);
            return cached;
        }
        InputStream fis = null;
        try {
            fis = FileUtil.toInputStream(filename);
            KeyStore created = readKeyStore(fis, pwd, ksType);
            LOGGER.debugf("# readKeyStore(%s) return new instance.", filename);
            keyStores.putIfAbsent(filename, created);
            return created;
        } finally {
            IOUtil.close(fis, "readKeyStore ok");
        }
    }

    /**
     * 效率：以文件名为Key缓存KeyStore
     */
    private static final ConcurrentMap<String, KeyStore> keyStores = new ConcurrentHashMap<String, KeyStore>();

    /**
     * 将流转换成KeyStore
     *
     * @param is     输入流
     * @param pwd    KeyStore 密码
     * @param ksType KeyStore类型（JKS|JceKS|PKCS12|BKS|UBER），一般为JKS
     * @return KeyStore密钥库
     * @throws java.io.IOException                    读取文件异常时抛出
     * @throws java.security.GeneralSecurityException 读取密钥失败时抛出
     */
    public static final KeyStore readKeyStore(InputStream is, String pwd,
                                              String ksType) throws IOException, GeneralSecurityException {
        KeyStore store = KeyStore.getInstance(ksType);
        try {
            store.load(is, pwd.toCharArray());
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
            } catch (IOException var2) {
            }
        }
        return store;
    }

    /**
     * @see #readKeyStore(java.io.InputStream, String, String)
     */
    public static final KeyStore readJks(InputStream is, String pwd)
            throws IOException, GeneralSecurityException {
        return readKeyStore(is, pwd, "JKS");
    }

    /**
     * @see #readKeyStore(String, String, String)
     */
    public static final KeyStore readJks(String filename, String pwd)
            throws IOException, GeneralSecurityException {
        return readKeyStore(filename, pwd, "JKS");
    }

    /**
     * @see #readKeyStore(java.io.InputStream, String, String)
     */
    public static final KeyStore readPKCS12(InputStream is, String pwd)
            throws IOException, GeneralSecurityException {
        return readKeyStore(is, pwd, "PKCS12");
    }

    /**
     * @see #readKeyStore(String, String, String)
     */
    public static final KeyStore readPKCS12(String filename, String pwd)
            throws IOException, GeneralSecurityException {
        return readKeyStore(filename, pwd, "PKCS12");
    }

    /**
     * 输出KeyStore中的内容,供查看
     *
     * @param keystore KeyStore
     * @param pwd      KeyStore密码
     * @throws java.security.GeneralSecurityException
     */
    public static final void printKeyStore(KeyStore keystore, String pwd)
            throws GeneralSecurityException {
        int i = 0;
        for (Enumeration<String> e = keystore.aliases(); e.hasMoreElements(); i++) {
            String alias = e.nextElement();
            X509Certificate cert = (X509Certificate) keystore
                    .getCertificate(alias);
            if (cert != null) {
                Date start = cert.getNotBefore();
                Date end = cert.getNotAfter();
                LOGGER.debugf("%d.证书别名:%s ", i, alias);
                LOGGER.debugf("%d.证书类型:%s/v%d", i, cert.getType(),
                        cert.getVersion());
                LOGGER.debugf("%d.密钥算法:%s ", i, cert.getPublicKey().getAlgorithm());
                LOGGER.debugf("%d.签名算法:%s ", i, cert.getSigAlgName());
                LOGGER.debugf("%d.证书序号:%s ", i, cert.getSerialNumber());
                LOGGER.debugf("%d.有效期(%s----%s)", i, start, end);
                LOGGER.debugf("%d.持有者:%s ", i, cert.getSubjectDN());
                LOGGER.debugf("%d.签发者:%s ", i, cert.getIssuerDN());
            }
            // 必须要有口令才可访问私钥
            if (pwd != null) {
                Key privateKey = keystore.getKey(alias, pwd.toCharArray());
                if (privateKey == null) {
                    LOGGER.debugf("%d.别名:%s无私钥 ", i, alias);
                    continue;
                }
                LOGGER.debugf("%d.私钥类名:%s ", i, privateKey.getClass());
                LOGGER.debugf("%d.私钥格式:%s/%s}", i, privateKey.getAlgorithm(),
                        privateKey.getFormat());
            }
        }
    }

    /**
     * 从文件中读取密钥（反序列化）
     *
     * @param keyfile 密钥文件
     * @return 密钥（java.security.Key）
     * @throws java.io.IOException    读取文件出错
     * @throws ClassNotFoundException 文件内容无法反序列化为Key
     */
    public static Key readObjectKey(String keyfile) throws IOException {
        return (Key) FileUtil.readObject(keyfile);
    }

    // ----------------------签名验签---------------------------

    /**
     * 使用私钥生成签名
     *
     * @param data   待签名数据
     * @param priKey 私钥
     * @param alg    签名算法（MD5WITHRSA|SHA1WITHRSA|SHA256WITHDSA|SHA1WITHDSA...）
     * @return 签名信息
     * @throws java.security.GeneralSecurityException
     */
    public static byte[] sign(byte[] data, PrivateKey priKey, String alg)
            throws GeneralSecurityException {
        long start = System.currentTimeMillis();
        Signature sig = Signature.getInstance(alg);
        sig.initSign(priKey);
        sig.update(data);
        byte[] signed = sig.sign();
        long cost = System.currentTimeMillis() - start;
        LOGGER.debugf("# sign() using %s use %d ms.", alg, cost);
        return signed;
    }

    /**
     * 使用公钥验证签名是否正确
     *
     * @param data   签名原文
     * @param pubKey 公钥
     * @param alg    签名算法
     * @param sign   原签名信息
     * @return 验签通过返回<code>true</code>，否则返回<code>false</code>
     * @throws java.security.GeneralSecurityException
     */
    public static boolean verify(byte[] data, PublicKey pubKey, String alg,
                                 byte[] sign) throws GeneralSecurityException {
        long start = System.currentTimeMillis();
        Signature sig = Signature.getInstance(alg);// MD5withRSA
        sig.initVerify(pubKey);
        sig.update(data);
        boolean ok = sig.verify(sign);
        long cost = System.currentTimeMillis() - start;
//        if (_log.isDebugEnabled()) {// 参数大于3个时，避免创建数组
            LOGGER.debugf("# verify() using %s result is %s use %d ms.", alg, ok,
                    cost);
//        }
        return ok;
    }

    /**
     * 使用私钥生成签名，签名编码为Base64格式
     *
     * @param data   待签名数据
     * @param priKey 私钥
     * @param alg    签名算法（MD5WITHRSA|SHA1WITHRSA|SHA256WITHDSA|SHA1WITHDSA...）
     * @return 签名信息, Base64格式
     * @throws java.security.GeneralSecurityException
     */
    public static String signPEM(byte[] data, PrivateKey priKey, String alg)
            throws GeneralSecurityException {
        byte[] signed = sign(data, priKey, alg);
        return Base64.encodeBase64String(signed);
    }

    /**
     * 使用公钥验证签名是否正确
     *
     * @param data       签名原文
     * @param pkey       公钥
     * @param alg        签名算法
     * @param signBase64 原签名信息,Base64格式
     * @return 验签通过返回<code>true</code>，否则返回<code>false</code>
     * @throws java.security.GeneralSecurityException
     */
    public static boolean verifyPEM(byte[] data, PublicKey pkey, String alg,
                                    String signBase64) throws GeneralSecurityException {
        byte[] signed = Base64.decodeBase64(signBase64);
        return verify(data, pkey, alg, signed);
    }

    public static void printProvidersInfo() {
        String[] serviceTypes = {"Signature", "MessageDigest", "Cipher",
                "Mac", "KeyStore"};
        for (String serviceType : serviceTypes) {
            LOGGER.debugf("%s = %s", serviceType,
                    Security.getAlgorithms(serviceType));
        }
    }

    private static byte[] asymCipher(byte[] data, Key key, int opmode,
                                     String alg) throws GeneralSecurityException,
            IllegalArgumentException {
        Validate.notNull(key, "密钥不能为空");
        Validate.notNull(alg, "加解密算法不能为空");
        return cipher(data, key, opmode, alg, null);
    }

    /**
     * RSA加密
     *
     * @param bplain  明文
     * @param rsaKey  RSA密钥
     * @param mode    加解密模式(ECB|CBC|CFB|OFB)
     * @param padding 填充算法(PKCS1Padding|PKCS5Padding|PKCS7Padding...)
     * @return 加密结果, 字节数组形式
     * @throws java.security.GeneralSecurityException 加解密出现错误时抛出
     * @throws IllegalArgumentException               参数不合法时抛出
     */
    public static byte[] rsaEncrypt(byte[] bplain, Key rsaKey, String mode,
                                    String padding) throws GeneralSecurityException,
            IllegalArgumentException {
        String alg = String.format("RSA/%s/%s", mode, padding);
        return asymCipher(bplain, rsaKey, ENCRYPT_MODE, alg);
    }

    /**
     * 使用默认ECB模式进行RSA加密
     *
     * @param bplain  明文
     * @param rsaKey  RSA密钥
     * @param padding 填充算法(PKCS1Padding|PKCS5Padding|PKCS7Padding...)
     * @return 加密结果, 字节数组形式
     * @throws java.security.GeneralSecurityException 加解密出现错误时抛出
     * @throws IllegalArgumentException               参数不合法时抛出
     */
    public static byte[] rsaEncrypt(byte[] bplain, Key rsaKey, String padding)
            throws GeneralSecurityException, IllegalArgumentException {
        return rsaEncrypt(bplain, rsaKey, DEFAULT_MODE, padding);
    }

    /**
     * 使用默认ECB模式,PKCS1Padding填充算法进行RSA加密
     *
     * @param bplain 明文
     * @param rsaKey RSA密钥
     * @return 加密结果, 字节数组形式
     * @throws java.security.GeneralSecurityException 加解密出现错误时抛出
     * @throws IllegalArgumentException               参数不合法时抛出
     */
    public static byte[] rsaEncrypt(byte[] bplain, Key rsaKey)
            throws GeneralSecurityException, IllegalArgumentException {
        return rsaEncrypt(bplain, rsaKey, DEFAULT_MODE, DEFAULT_ASYM_PADDING);
    }

    /**
     * RSA解密
     *
     * @param bcipher 密文
     * @param rsaKey  RSA密钥
     * @param mode    加解密模式(ECB|CBC|CFB|OFB)
     * @param padding 填充算法(PKCS1Padding|PKCS5Padding|PKCS7Padding...)
     * @return 解密结果, 字节数组形式
     * @throws java.security.GeneralSecurityException 加解密出现错误时抛出
     * @throws IllegalArgumentException               参数不合法时抛出
     */
    public static byte[] rsaDecrypt(byte[] bcipher, Key rsaKey, String mode,
                                    String padding) throws GeneralSecurityException,
            IllegalArgumentException {
        String alg = String.format("RSA/%s/%s", mode, padding);
        return asymCipher(bcipher, rsaKey, DECRYPT_MODE, alg);
    }

    /**
     * 使用默认的ECB模式进行RSA解密
     *
     * @see #rsaDecrypt(byte[], java.security.Key, String, String)
     */
    public static byte[] rsaDecrypt(byte[] bcipher, Key rsaKey, String padding)
            throws GeneralSecurityException, IllegalArgumentException {
        return rsaDecrypt(bcipher, rsaKey, DEFAULT_MODE, padding);
    }

    /**
     * 使用默认的ECB模式,PKCS1Padding填充算法进行RSA解密
     *
     * @see #rsaDecrypt(byte[], java.security.Key, String, String)
     */
    public static byte[] rsaDecrypt(byte[] bcipher, Key rsaKey)
            throws GeneralSecurityException, IllegalArgumentException {
        return rsaDecrypt(bcipher, rsaKey, DEFAULT_MODE, DEFAULT_ASYM_PADDING);
    }

}
