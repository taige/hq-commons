package io.hqwu.commons.util;

import java.text.DecimalFormatSymbols;
import java.util.Formatter;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created with IntelliJ IDEA
 * User: taige
 * Date: 2018/4/19
 * Time: 下午2:39
 */
public class AmountUtilLocale {

    private static final ConcurrentHashMap<String, AmountUtilLocale> pool = new ConcurrentHashMap<String, AmountUtilLocale>();

    private final Locale locale;
    private final DecimalFormatSymbols dfs;
    private final String fmtString;

    /**
     * 获取默认语言的MoneyUtil
     * @return
     */
    public static AmountUtilLocale getInstance() {
        return getInstance(null, null);
    }

    /**
     * 获取指定语言的MoneyUtil
     * @param language lowercase two-letter ISO-639 code.
     * @return
     */
    public static AmountUtilLocale getInstance(String language) {
        return getInstance(language, null);
    }

    /**
     * 获取指定语言-国家的MoneyUtil
     * @param language lowercase two-letter ISO-639 code.
     * @param country  uppercase two-letter ISO-3166 code.
     * @return
     */
    public static AmountUtilLocale getInstance(String language, String country) {
        String key = String.format("%s-%s", language, country);
        if (! pool.containsKey(key)) {
            synchronized (pool) {
                if (! pool.containsKey(key)) {
                    Locale locale = StringUtil.isBlank(language) ? Locale.getDefault() :
                            (StringUtil.isBlank(country) ? new Locale(language) : new Locale(language, country));
                    AmountUtilLocale util = new AmountUtilLocale(locale);
                    pool.put(key, util);
                    return util;
                }
            }
        }
        return pool.get(key);
    }

    private AmountUtilLocale(Locale locale) {
        this.locale = locale;
        this.dfs = DecimalFormatSymbols.getInstance(this.locale);
        this.fmtString = this.dfs.getDecimalSeparator() + "%0";
    }

    /**
     * 分转元(即小数点左移2位) 1 -> '0.01'
     * @param cent 分 整型
     * @return 元 格式化字符串(精度：小数点后2位）
     */
    public String cent2Dollar(long cent) {
        return cent2Dollar(cent, 2, false);
    }

    /**
     * 分转元(即小数点左移 precision位)
     * @param cent 分 整型
     * @param precision 左移的位数 取值范围：1-9，如果大于9，会抛出RuntimeException
     * @return 元 格式化字符串(精度：小数点后 precision位）
     */
    public String cent2Dollar(long cent, int precision) {
        return cent2Dollar(cent, precision, false);
    }

    /**
     * 分转元(即小数点左移2位，且增加千分位标记)
     * @param cent 分 整型
     * @param formatThous true: 增加千分位标记, false: 没有千分位
     * @return 元 格式化字符串, 如 123456 -> '1,234.56'
     */
    public String cent2Dollar(long cent, boolean formatThous) {
        return cent2Dollar(cent, 2, formatThous);
    }

    /**
     * 分转元 (可以指定精度和千分位)
     * @param cent
     * @param precision 左移的位数 取值范围：1-9，如果大于9，会抛出RuntimeException
     * @param formatThous
     * @return
     */
    public String cent2Dollar(long cent, int precision, boolean formatThous) {
        if (precision <= 0) {
            return formatThous ? formatThousands(cent) : String.valueOf(cent);
        }
        if (precision >= 10) {
            throw new IllegalArgumentException("precision is too big than 10");
        }
        StringBuilder sb = new StringBuilder();
        if (cent < 0) {
            sb.append('-');
        }
        long centAbs = Math.abs(cent);
        int centBase = (int) Math.pow(10, precision);
        long d = centAbs / centBase;
        long c = centAbs % centBase;

        if (formatThous) {
            formatThousands(d, sb);
        } else {
            sb.append(d);
        }
        Formatter fmt = new Formatter(sb, this.locale);
        fmt.format(this.fmtString + precision + 'd', c);
        fmt.flush();
        return sb.toString();
    }

    /**
     * 增加千分位，方便人工读数
     * 1234567 => 1,234,567
     * @param bigNumber 1234567
     * @return 1,234,567
     */
    public String formatThousands(long bigNumber) {
        return formatThousands(bigNumber, null);
    }


    /**
     * 增加千分位，方便人工读数
     * 1234567 => 1,234,567
     * @param bigNumber 1234567
     * @param outBuffer 如果not null，则格式化字符串直接输出到buffer，返回空串""；如果是null, 则返回格式化字符串
     * @return 1,234,567 if outBuffer is null
     */
    public String formatThousands(long bigNumber, StringBuilder outBuffer) {
        String src = String.valueOf(bigNumber);
        int len = src.length();
        int count = len / 3;
        int first = len % 3;
        if (count < 1 || (count == 1 && first == 0)) {
            if (outBuffer == null) {
                return src;
            }
            outBuffer.append(src);
            return "";
        }
        if (first == 0) {
            first = 3;
            count--;
        }
        StringBuilder sb = (outBuffer == null ? new StringBuilder(len + count) : outBuffer);
        for (int i = 0; i < len; i++) {
            sb.append(src.charAt(i));
            if ((i+1) == first) {
                sb.append(this.dfs.getGroupingSeparator());
            } else if (i > first && ((i+1-first)%3) == 0 && (i+1) < len) {
                sb.append(this.dfs.getGroupingSeparator());
            }
        }
        return outBuffer == null ? sb.toString() : "";
    }

    /**
     * 元转分 1.00 -> 100
     * @param dollar String
     * @return long 分
     */
    public long dollar2Cent(String dollar) {
        return dollar2Cent(dollar, 2);
    }

    /**
     * 元转分 1.00 -> 100
     * @param dollar String
     * @param precision int 精度. 当小数点后的位数>precision，以 precision 为准，会忽略之后的数字
     * @return long 分
     */
    public long dollar2Cent(String dollar, int precision) {
        if (dollar == null || dollar.trim().equals("")) {
            return 0;
        }
        if (precision < 0) {
            precision = 0;
        } else if (precision >= 10) {
            throw new IllegalArgumentException("precision is too big than 10: " + precision);
        }
        dollar = dollar.trim();
        StringBuilder outBuf = new StringBuilder();
        int prec_consumed = 0;
        boolean dec_flag = false;
        boolean val_flag = false;
        for (int i = 0; i < dollar.length(); i++) {
            char ch = dollar.charAt(i);
            switch(ch) {
                case '+':
                    if (i > 0) {
                        throw new NumberFormatException(dollar);
                    }
                    break;
                case '-':
                    if (i > 0) {
                        throw new NumberFormatException(dollar);
                    }
                    outBuf.append('-');
                    break;
                case '.':
                case ',':
                case ' ':
                case '\'':
                    if (this.dfs.getDecimalSeparator() == ch) {
                        if (dec_flag) {
                            throw new NumberFormatException(dollar);
                        }
                        dec_flag = true;
                    }
                    break;
                default:
                    if ('0' <= ch && ch <= '9') {
                        if (!dec_flag || prec_consumed < precision) {
                            // ignore more decimal
                            if ('0' != ch || val_flag) {
                                // skip leading 0
                                outBuf.append(ch);
                                val_flag = true;
                            }
                        }
                    } else {
                        throw new NumberFormatException(dollar);
                    }
                    if (dec_flag) {
                        prec_consumed++;
                    }
            }
        }
        for (int i = prec_consumed; i < precision; i++) {
            outBuf.append('0');
        }
        if (val_flag) {
            try {
                return Long.parseLong(outBuf.toString());
            } catch (NumberFormatException e) {
                // 万一有未知bug，让outBuf不能转换成long，抛出输入的字符串的异常
                throw new NumberFormatException(dollar);
            }
        } else {
            // 没有有效数据，则返回0
            return 0;
        }
    }


}
