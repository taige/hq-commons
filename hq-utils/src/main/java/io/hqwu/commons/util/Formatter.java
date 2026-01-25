package io.hqwu.commons.util;

import lombok.experimental.UtilityClass;

/**
 * 字符串格式化工具类。
 * <p>
 * 该工具类提供了一系列字符串格式化方法，主要功能包括：
 * <ul>
 *   <li>数字格式化：将数字左对齐或右对齐到固定长度</li>
 *   <li>字符串对齐：支持左对齐和右对齐，可指定填充字符</li>
 *   <li>字符串裁剪：支持从左侧或右侧裁剪字符串到指定长度</li>
 *   <li>数字可读化：为长整型数字添加千分位分隔符，便于阅读</li>
 * </ul>
 * </p>
 *
 * <p>
 * 主要应用场景：
 * <ul>
 *   <li>日志输出格式化</li>
 *   <li>控制台表格显示</li>
 *   <li>性能指标展示（如纳秒时间格式化）</li>
 *   <li>固定宽度文本处理</li>
 * </ul>
 * </p>
 *
 * @author taige (Wu, Hongqiang)
 * @see StringUtil
 * @since 1.0
 */
@UtilityClass
public class Formatter {
    
    public static String fmtNumR(long num) {
        return ralign(String.valueOf(num), 8, ' ');
    }

    public static String fmtNumL(long num) {
        return lalign(String.valueOf(num), 8, ' ');
    }

    public static String fmtStrR(String str, int len) {
        if (str.getBytes().length >= len) {
            return str;
        }
        return ralign(str, len, ' ');        
    }

    public static String fmtStrL(String str, int len) {
        if (str.getBytes().length >= len) {
            return str;
        }
        return lalign(str, len);        
    }

    public static String ltrim(String str, int len) {
        byte[] bstr = str.getBytes();
        if (bstr.length <= len) {
            return str;
        }
        byte[] obs = new byte[len];
        int offset = bstr.length - len;
        System.arraycopy(bstr, offset, obs, 0, len);
        return new String(obs);
    }
    
    public static String rtrim(String str, int len) {
        byte[] bstr = str.getBytes();
        if (bstr.length <= len) {
            return str;
        }
        byte[] obs = new byte[len];
        System.arraycopy(bstr, 0, obs, 0, len);
        return new String(obs);
    }
    
    public static String ralign(String str, int len) {
        return ralign(str, len, '0');
    }

    public static String lalign(String str, int len) {
        return lalign(str, len, ' ');
    }

    public static String ralign(String str, int len, char fill) {
        return align(str, len, fill, false);
    }

    public static String lalign(String str, int len, char fill) {
        return align(str, len, fill, true);
    }

    public static String align(String str, int len, char fill, boolean leftAlign) {
        if (str == null) {
            str = "";
        }
        byte[] buf = new byte[len];
        byte[] bstr = str.getBytes();
        int str_len = bstr.length;
        int fill_len = len - str_len;
        if (leftAlign) {
            if (fill_len > 0) {
                System.arraycopy(bstr, 0, buf, 0, str_len);
            } else {
                System.arraycopy(bstr, 0, buf, 0, len);
            }
        } else {
            if (fill_len > 0) {
                System.arraycopy(bstr, 0, buf, fill_len, str_len);
            } else {
                System.arraycopy(bstr, -fill_len, buf, 0, len);
            }
        }
        for (int i = 0; i < fill_len; i++) {
            if (leftAlign) {
                buf[i + str_len] = (byte) fill;
            } else {
                buf[i] = (byte) fill;
            }
        }
        return new String(buf);
    }

    public static String trim(String str) {
        return str == null ? "" : str.trim();
    }

    /**
     * 把纳秒的输出增加千分位，方便人工读数
     * 1234567 => 1,234,567
     * @param ns 1234567
     * @return 1,234,567
     */
    public static String formatNS(long ns) {
        String src = String.valueOf(ns);
        int len = src.length();
        int count = len / 3;
        int first = len % 3;
        if (count < 1 || (count == 1 && first == 0)) {
            return src;
        }
        if (first == 0) {
            first = 3;
            count--;
        }
        StringBuilder sb = new StringBuilder(len + count);
        for (int i = 0; i < len; i++) {
            sb.append(src.charAt(i));
            if ((i+1) == first) {
                sb.append(',');
            } else if (i > first && ((i+1-first)%3) == 0 && (i+1) < len) {
                sb.append(',');
            }
        }
        String fmt = sb.toString();
        //assert fmt.length() == (len+count);
        return fmt;
    }
}
