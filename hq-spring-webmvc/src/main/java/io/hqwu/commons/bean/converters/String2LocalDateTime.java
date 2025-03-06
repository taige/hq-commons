package io.hqwu.commons.bean.converters;

import io.hqwu.commons.bean.ValueOf;
import io.hqwu.commons.util.StringUtil;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created with Intellij IDEA for qrcode-api-server
 *
 * @author zhangyao
 * @since 2021-10-03 9:47 a.m.
 */

public class String2LocalDateTime implements ValueOf<String, LocalDateTime> {

    private static final Map<String, DateTimeFormatter> dateTimeFormatters = new ConcurrentHashMap<>();

    @Override
    public LocalDateTime valueOf(String srcValue, Object srcBean, String srcProperty, Object targetBean, String targetProperty, String... params) {
        if (StringUtil.isBlank(srcValue)) {
            return null;
        }
        String fmt = params.length > 0 ? params[0] : "yyyyMMddHHmmss";
        DateTimeFormatter formatter = dateTimeFormatters.computeIfAbsent(fmt, DateTimeFormatter::ofPattern);
        return LocalDateTime.parse(srcValue, formatter);
    }

}
