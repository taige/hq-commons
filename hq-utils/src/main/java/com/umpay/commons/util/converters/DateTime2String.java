package com.umpay.commons.util.converters;

import com.umpay.commons.util.ValueOf;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA for pp-gopay-fa
 * User: taige
 * Date: 2020/5/5
 * Time: 11:28
 */
public class DateTime2String implements ValueOf<LocalDateTime, String> {

    private Map<String, DateTimeFormatter> dateTimeFormatters = new HashMap<>();

    @Override
    public String valueOf(LocalDateTime srcValue, Object srcBean, String srcProperty, Object targetBean, String targetProperty, String... params) {
        String fmt = params.length > 0 ? params[0] : "yyyy-MM-dd HH:mm:ss";
        DateTimeFormatter formatter = dateTimeFormatters.computeIfAbsent(fmt, DateTimeFormatter::ofPattern);
        return formatter.format(srcValue);
    }
}
