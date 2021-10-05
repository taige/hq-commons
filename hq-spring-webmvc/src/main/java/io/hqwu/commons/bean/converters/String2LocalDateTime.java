package io.hqwu.commons.bean.converters;

import io.hqwu.commons.bean.ValueOf;

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
        String fmt = params.length > 1 ? params[1] : "yyyyMMddHHmmss";
        DateTimeFormatter formatter = dateTimeFormatters.computeIfAbsent(fmt, DateTimeFormatter::ofPattern);
        return LocalDateTime.parse(srcValue, formatter);
    }

}
