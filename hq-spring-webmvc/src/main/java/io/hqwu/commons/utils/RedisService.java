package io.hqwu.commons.utils;

import java.util.Set;

/**
 * Created with IntelliJ IDEA for hq-spring-webmvc
 *
 * @author taige (Wu, Hongqiang)
 * Date: 2021-09-17
 * Time: 10:03
 */
public interface RedisService {

    boolean expire(String key, long timeoutInSeconds);

    long expire(String key);

    boolean set(String key, Object value, long timeoutInSeconds);

    Object get(String key);

    boolean exists(String key);

    void del(String... keys);

    Set<String> keys(String pattern);
}
