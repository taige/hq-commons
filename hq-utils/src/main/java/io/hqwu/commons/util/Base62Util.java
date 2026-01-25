package io.hqwu.commons.util;

import lombok.experimental.UtilityClass;

import java.util.Arrays;
import java.util.zip.CRC32;

/**
 * Base62 进制转换工具类。
 *
 * <p>提供 10 进制数字（long）与 62 进制字符串之间的相互转换功能。
 * 62 进制字符集由数字 0-9、大写字母 A-Z 和小写字母 a-z 组成。
 *
 * <p>主要功能：
 * <ul>
 *     <li>支持 long 类型数字的 Base62 编码与解码。</li>
 *     <li>支持指定最小长度的编码输出（通过 {@link StringUtil#leftPad} 补位）。</li>
 *     <li>支持集成 CRC32 校验码（使用 {@link CRC32}），用于增强数据传输或存储时的完整性校验。</li>
 * </ul>
 *
 * @see java.util.zip.CRC32
 * @see io.hqwu.commons.util.StringUtil
 * @author taige (Wu, Hongqiang)
 */
@UtilityClass
public class Base62Util {
    public static final String MAX_VALUE = "AzL8n0Y58m7";

    public static final int MAX_VALUE_LEN = MAX_VALUE.length();

    public static final int DEFAULT_MIN_LENGTH = 4;

    private static final char[] B62_TABLE = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".toCharArray();

    private static final int SCALE = 62;

    private static final int CRC_LEN = 6;


    static long crc32(byte[] bs) {
        CRC32 crc32 = new CRC32();
        crc32.update(bs);
        return crc32.getValue();
    }

    static String crc32_6(byte[] bs) {
        String crc = encode(crc32(bs), CRC_LEN);
        if (crc.length() > CRC_LEN) {
            System.err.println("crc = " + crc);
            crc = crc.substring(crc.length() - CRC_LEN);
            throw new RuntimeException("crc = " + crc);
        }
        return crc;
    }

    /**
     * 10进制数字转62进制
     * @param num
     * @return
     */
    public static String encode(long num) {
        return encode(num, DEFAULT_MIN_LENGTH);
    }

    /**
     * 10进制数字转62进制，尾部添加CRC32校验码
     * @param num
     * @param appendCRC
     * @return
     */
    public static String encode(long num, boolean appendCRC) {
        if (appendCRC) {
            String b62 = encode(num, 1);
            String crc6 = crc32_6(b62.getBytes());
            return b62 + crc6;
        } else {
            return encode(num);
        }
    }

    /**
     * 10进制数字转62进制
     * @param number
     * @param minLength
     * @return
     */
    public static String encode(long number, int minLength) {
        if (number < 0) {
            throw new IllegalArgumentException("number must be positive");
        }
        StringBuilder sb = new StringBuilder();
        long remainder;
        while (number > SCALE - 1) {
            //对 scale 进行求余，然后将余数追加至 sb 中，由于是从末位开始追加的，因此最后需要反转字符串
            remainder = number % SCALE;
            sb.append(B62_TABLE[(int) remainder]);
            //除以进制数，获取下一个末尾数
            number = number / SCALE;
        }
        sb.append(B62_TABLE[(int) number]);
        String value = sb.reverse().toString();
        return StringUtil.leftPad(value, minLength, '0');
    }

    /**
     * 62进制转为10进制数字
     * @param numberString
     * @return
     */
    public static long decode(String numberString) {
        if (StringUtil.isBlank(numberString) || numberString.length() > MAX_VALUE_LEN
                || (numberString.length() == MAX_VALUE_LEN && numberString.compareTo(MAX_VALUE) > 0)) {
            throw new NumberFormatException(String.format("<%s> is not a Base62 char", numberString));
        }
        char[] ch = numberString.toCharArray();
        long result = 0;
        long base = 1;
        for (int i = ch.length - 1; i >= 0; i--) {
            int index = Arrays.binarySearch(B62_TABLE, ch[i]);
            if (index < 0) {
                throw new NumberFormatException(String.format("<%s> is not a Base62 char", ch[i]));
            }
            result += index * base;
            base *= SCALE;
        }
        return result;
    }

    public static long decode(String numberString, boolean checkCRC) {
        if (checkCRC) {
            if (numberString.length() < CRC_LEN + 1) {
                throw new NumberFormatException(String.format("<%s> is not a CRC-Base62 number", numberString));
            }
            String crc_in = numberString.substring(numberString.length() - CRC_LEN);
            String payload = numberString.substring(0, numberString.length() - CRC_LEN);
            String crc = crc32_6(payload.getBytes());
            if (crc_in.equals(crc)) {
                return decode(payload);
            } else {
                throw new IllegalArgumentException(String.format("<%s> CRC check failed", numberString));
            }
        } else {
            return decode(numberString);
        }
    }

}
