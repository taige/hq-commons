package io.hqwu.commons.cp.util;

import java.util.Set;

/**
 * Created with IntelliJ IDEA for hq-commons-parent
 *
 * @author taige (Wu, Hongqiang)
 * Date: 2025-03-11
 * Time: 08:48
 */
public class SqlMasker {

    /**
     * 对SQL中的敏感字段进行mask
     * @param sql 输入的 SQL 语句
     * @param sensitiveFields 敏感字段集合
     * @param maskPattern 脱敏模式（例如 "****"）
     * @return 脱敏后的 SQL 语句
     */
    public static String maskSensitiveFields(String sql, Set<String> sensitiveFields, String maskPattern) {

        return sql;
    }

    /**
     * 对(敏感)字段进行mask <br/>
     *    mask规则：<br/>
     *      1. 如果field长度 <= maskPattern长度，则将field全部mask <br/>
     *      2. 如果field长度介于maskPattern长度的1-3倍之间，则将field中间部分mask，多余的字符数平均到首尾保留下来，不能平均时，将多余字符数保留到首位 <br/>
     *      3. 如果field长度 > maskPattern长度的3倍，收尾保留maskPattern长度，中间部分用 maskPattern 循环填充保证替换完后的字段长度跟原来一致 <br/>
     * @param field 原始字段
     * @param maskPattern mask模式, 如"***", "####", "????", "*#?●○"
     * @return 处理后的字段
     */
    static String maskField(String field, String maskPattern) {
        if (field == null || maskPattern == null || maskPattern.isEmpty()) {
            return field;
        }

        int fLen = field.length();
        int pLen = maskPattern.length();

        // 规则1：字段长度 <= 掩码长度，全部mask
        if (fLen <= pLen) {
            return maskPattern;
        }

        StringBuilder sb = new StringBuilder(fLen);

        // 规则2：字段长度在掩码长度1-3倍之间
        if (fLen <= pLen * 3) {
            int remain = fLen - pLen;
            int front = (remain + 1) / 2;  // 不能平均时多余字符保留到首位
            sb.append(field, 0, front)
                    .append(maskPattern)
                    .append(field, fLen - remain/2, fLen);
            return sb.toString();
        }

        // 规则3：字段长度大于掩码长度3倍
        int midLen = fLen - 2 * pLen;
        sb.append(field, 0, pLen);  // 前缀

        // 循环填充中间部分
        for (int i = 0; i < midLen / pLen; i++) {
            sb.append(maskPattern);
        }
        if (midLen % pLen > 0) {
            sb.append(maskPattern, 0, midLen % pLen);
        }

        sb.append(field, fLen - pLen, fLen);  // 后缀
        return sb.toString();
    }

}
