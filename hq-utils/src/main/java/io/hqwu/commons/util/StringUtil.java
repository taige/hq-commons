package io.hqwu.commons.util;

import org.apache.commons.lang3.StringUtils;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Description:常用字符串操作类。commons-lang3中不能提供的在此类定义。
 * 
 * @version 1.0 2013-8-12 依赖commons-lang3的实现

 * @see org.apache.commons.lang3.StringUtils
 */

public final class StringUtil extends StringUtils {
    public final static String CRLF = System.getProperty("line.separator");
    // -----------------------金额处理-------------------------
    /**
     * 金额（分）字符串形式转为金额（元）字符串形式.采用四舍五入方式.
     *
     * <pre>
     * assertEquals(&quot;0&quot;, StringUtil.centToDollar(null));
     * assertEquals(&quot;0&quot;, StringUtil.centToDollar(&quot;&quot;));
     * assertEquals(&quot;0.00&quot;, StringUtil.centToDollar(&quot;0&quot;));
     * assertEquals(&quot;-0.01&quot;, StringUtil.centToDollar(&quot;-1&quot;));
     * assertEquals(&quot;1.00&quot;, StringUtil.centToDollar(&quot;100&quot;));
     * assertEquals(&quot;大数处理&quot;, &quot;1000000000.00&quot;,
     *         StringUtil.centToDollar(&quot;100000000000&quot;));
     * assertEquals(&quot;-1.00&quot;, StringUtil.centToDollar(&quot;-100&quot;));
     * assertEquals(&quot;1.00&quot;, StringUtil.centToDollar(&quot;100.4&quot;));
     * assertEquals(&quot;1.01&quot;, StringUtil.centToDollar(&quot;100.5&quot;));
     * assertEquals(&quot;1.01&quot;, StringUtil.centToDollar(&quot;100.6&quot;));
     * </pre>
     *
     * @param cent
     *            金额（分）字符串形式，如100表示100分=1元
     * @return 金额（元）字符串形式
     */
    public static final String centToDollar(String cent) {
        return transformNumber(cent, "0.00", 0.01);
    }

    /**
     * 金额（分）字符串形式转为金额（元）字符串形式.采用四舍五入方式.
     *
     * <pre>
     * assertEquals(&quot;0&quot;, StringUtil.centToDollarShort(null));
     * assertEquals(&quot;0&quot;, StringUtil.centToDollarShort(&quot;&quot;));
     * assertEquals(&quot;0&quot;, StringUtil.centToDollarShort(&quot;0&quot;));
     * assertEquals(&quot;-0&quot;, StringUtil.centToDollarShort(&quot;-1&quot;));
     * assertEquals(&quot;1&quot;, StringUtil.centToDollarShort(&quot;100&quot;));
     * assertEquals(&quot;大数处理&quot;, &quot;1000000000&quot;,
     *         StringUtil.centToDollarShort(&quot;100000000000&quot;));
     * assertEquals(&quot;-1&quot;, StringUtil.centToDollarShort(&quot;-100&quot;));
     * assertEquals(&quot;1&quot;, StringUtil.centToDollarShort(&quot;100.4&quot;));
     * assertEquals(&quot;1&quot;, StringUtil.centToDollarShort(&quot;100.5&quot;));
     * assertEquals(&quot;1&quot;, StringUtil.centToDollarShort(&quot;100.6&quot;));
     * </pre>
     *
     * @param cent
     *            金额（分）字符串形式，如100表示100分=1元
     * @return 金额（元）字符串形式
     */
    public static String centToDollarShort(String cent) {
        return transformNumber(cent, "###", 0.01);
    }

    /**
     * 金额（元）字符串形式转为金额（分）字符串形式.采用四舍五入方式.
     *
     * <pre>
     * assertEquals(&quot;0&quot;, StringUtil.dollarToCent(null));
     * assertEquals(&quot;0&quot;, StringUtil.dollarToCent(&quot;&quot;));
     * assertEquals(&quot;100&quot;, StringUtil.dollarToCent(&quot;1&quot;));
     * assertEquals(&quot;-100&quot;, StringUtil.dollarToCent(&quot;-1&quot;));
     * assertEquals(&quot;大数处理&quot;, &quot;10000000000000&quot;,
     *         StringUtil.dollarToCent(&quot;100000000000&quot;));
     * assertEquals(&quot;123&quot;, StringUtil.dollarToCent(&quot;1.23&quot;));
     * assertEquals(&quot;1023&quot;, StringUtil.dollarToCent(&quot;10.23&quot;));
     * </pre>
     *
     * @param dollar
     *            金额（元）字符串形式，如1表示1元=100分
     * @return 金额（分）字符串形式
     */
    public static String dollarToCent(String dollar) {
        return transformNumber(dollar, "###", 100);
    }

    /**
     * 金额（元） 转换成 字符串
     * @param dollar
     * @return
     */
    public static String dollarToString(double dollar) {
        return String.format("%.2f", dollar);
    }

    /**
     * 数字格式转换
     *
     * @param str
     *            原始数字字符串形式
     * @param format
     *            需要转换的格式(###|0.00|....)
     * @param rate
     *            转换利率，如100,0.1等
     * @return 转换后的字符串形式
     * @see java.text.DecimalFormat
     */
    private static String transformNumber(String str, String format, double rate) {
        if (StringUtil.isEmpty(str)) {
            return "0";
        }
        return new DecimalFormat(format).format(Double.parseDouble(str) * rate);
    }

    /**
     * 保护PAN, Track, CVC.
     * <pre>
     * "8881170010011367" 会被转换为 "888117******1367"
     * "8881170010011367=020128375" 会被转换为 "888117******1367=*********"
     * "8881170010011367D020128375" 会被转换为 "888117******1367D*********"
     * "998881170010011367^David^1609123000" 会被转换为 "998881********1367^David^**********"
     * "123" 会被转换为 "***"
     * </pre>
     * @param s 需要保护的敏感信息
     * @return 去除敏感信息的字符串
     */
    public static String protect(String s) {
        if (isEmpty(s)) {
            return s;
        }
        StringBuilder sb = new StringBuilder();
        int len = s.length();
        int clear = len > 6 ? 6 : 0;
        int lastFourIndex = -1;
        if (clear > 0) {
            lastFourIndex = s.indexOf('=') - 4;
            if (lastFourIndex < 0) {
                lastFourIndex = s.indexOf('^') - 4;
            }
            if (lastFourIndex < 0 && s.indexOf('^') < 0) {
                lastFourIndex = s.indexOf('D') - 4;
            }
            if (lastFourIndex < 0) {
                lastFourIndex = len - 4;
            }
        }
        for (int i = 0; i < len; i++) {
            if (s.charAt(i) == '=' || (s.charAt(i) == 'D' && s.indexOf('^') < 0)) {
                clear = 1;
            } else if (s.charAt(i) == '^') {
                lastFourIndex = 0;
                clear = len - i;
            } else if (i == lastFourIndex) {
                clear = 4;
            }
            sb.append(clear-- > 0 ? s.charAt(i) : '*');
        }
        s = sb.toString();
        int charCount = s.replaceAll("[^\\^]", "").length();
        if (charCount == 2) {
            s = s.substring(0, s.lastIndexOf("^") + 1);
            s = rightPad(s, len, '*');
        }
        return s;
    }

    /**
     * 在字符串左侧按长度填充字符
     *
     * @param s   原字符串
     * @param len 填充后的总字符串长度
     * @param c   填充字符
     * @return 填充完的字符串
     */
    public static String padleft(String s, int len, char c) {
        s = s.trim();
        if (s.length() > len)
            throw new IllegalArgumentException("invalid len " + s.length() + "/" + len);
        StringBuilder d = new StringBuilder(len);
        int fill = len - s.length();
        while (fill-- > 0)
            d.append(c);
        d.append(s);
        return d.toString();
    }


    /**
     * 检查指定的字符串列表是否不为空。
     */
    public static boolean areNotEmpty(String... values) {
        boolean result = true;
        if (values == null || values.length == 0) {
            result = false;
        } else {
            for (String value : values) {
                result &= !isBlank(value);
            }
        }
        return result;
    }

    /**
     * 根据Map的key排序，组成形式为 key1=value1&key2=value2 的string，如果value是空则不加入最终的string
     * @param unSignMapData
     * @return
     */
    public static String getSortDataByMap(Map<String, String> unSignMapData) {
        StringBuffer content = new StringBuffer();
        List<String> keys = new ArrayList<String>(unSignMapData.keySet());
        Collections.sort(keys);
        int index = 0;
        for (int i = 0; i < keys.size(); i++) {
            String key = keys.get(i);
            String value = unSignMapData.get(key);
            if (! StringUtil.isBlank(value)) {
                content.append((index == 0 ? "" : "&") + key + "=" + value);
                index++;
            }
        }
        return content.toString();
    }
}
