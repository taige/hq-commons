package io.hqwu.commons.bean.converters;

import io.hqwu.commons.bean.ValueOf;

import java.time.ZoneId;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Created with Intellij IDEA for qrcode-api-server
 *
 * @author zhangyao
 * @since 2021-09-30 10:15 a.m.
 */
public class String2ZoneId implements ValueOf<String, ZoneId> {
    private static final ConcurrentMap<String, ZoneId> zoneIds = new ConcurrentHashMap<>();

    @Override
    public ZoneId valueOf(String s, Object o, String s2, Object o1, String s1, String... strings) {
        return s == null ? null : zoneIds.computeIfAbsent(s, ZoneId::of);
    }

}
