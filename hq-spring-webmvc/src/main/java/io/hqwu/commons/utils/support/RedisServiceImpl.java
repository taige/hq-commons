package io.hqwu.commons.utils.support;

import com.umpay.commons.util.Logger;
import com.umpay.commons.util.StringUtil;
import io.hqwu.commons.utils.RedisService;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Created with IntelliJ IDEA for qrcode-api-server
 *
 * @author taige (Wu, Hongqiang)
 * Date: 2021-09-17
 * Time: 10:04
 */
public class RedisServiceImpl implements RedisService {
    private static final Logger LOGGER = new Logger();

    private final RedisTemplate<String, Object> redisTemplate;

    private String keyPrefix = null;

    public RedisServiceImpl(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void setKeyPrefix(String keyPrefix) {
        this.keyPrefix = keyPrefix;
    }

    private String prefixKey(String key) {
        if (StringUtil.isBlank(key)) {
            return null;
        }
        return StringUtil.isBlank(keyPrefix) ? key : keyPrefix + key;
    }

    private <T> T safeExecute(Function<String, T> executor, String key, Supplier<T> valueIfNull) {
        if (StringUtil.isBlank(key)) {
            throw new IllegalArgumentException("redis.key must not be blank");
        }
        T rs = null;
        try {
            rs = executor.apply(prefixKey(key));
        } catch (RuntimeException e) {
            LOGGER.warn("redis command for %s failed: ", key, e);
        }
        return rs != null ? rs : (valueIfNull == null ? null : valueIfNull.get());
    }

    private <T> T safeExecute(Function<Collection<String>, T> executor, String[] keys, Supplier<T> valueIfNull) {
        if (keys == null || keys.length == 0) {
            throw new IllegalArgumentException("redis.key must not be blank");
        }
        T rs = null;
        try {
            rs = executor.apply(Arrays.stream(keys)
                    .map(this::prefixKey)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet()));
        } catch (RuntimeException e) {
            LOGGER.warn("redis command for %s failed: ", keys, e);
        }
        return rs != null ? rs : (valueIfNull == null ? null : valueIfNull.get());
    }

    @Override
    public boolean expire(String key, long timeoutInSeconds) {
        return safeExecute(_key -> {
            if (timeoutInSeconds > 0) {
                return redisTemplate.expire(_key, timeoutInSeconds, TimeUnit.SECONDS);
            }
            return false;
        }, key, () -> false);
    }

    @Override
    public long expire(String key) {
        return safeExecute(_key -> redisTemplate.getExpire(_key, TimeUnit.SECONDS), key, () -> -1L);
    }

    @Override
    public boolean set(String key, Object value, long timeoutInSeconds) {
        return safeExecute(_key -> {
            if (timeoutInSeconds > 0) {
                redisTemplate.opsForValue().set(_key, value, timeoutInSeconds, TimeUnit.SECONDS);
            } else {
                redisTemplate.opsForValue().set(_key, value);
            }
            return true;
        }, key, () -> false);
    }

    @Override
    public Object get(String key) {
        return safeExecute(_key -> redisTemplate.opsForValue().get(_key), key, null);
    }

    @Override
    public boolean exists(String key) {
        return safeExecute(redisTemplate::hasKey, key, () -> false);
    }

    @Override
    public void del(String ... keys) {
        if (keys.length == 1) {
            safeExecute(redisTemplate::delete, keys[0], () -> false);
        } else if (keys.length > 1) {
            safeExecute(redisTemplate::delete, keys, () -> false);
        }
    }

    @Override
    public Set<String> keys(String pattern) {
        Set<String> _keys = safeExecute(redisTemplate::keys, pattern, Collections::emptySet);
        if (StringUtil.isNotBlank(keyPrefix)) {
            int len = keyPrefix.length();
            return _keys.stream().map(key -> key.substring(len)).collect(Collectors.toSet());
        }
        return _keys;
    }

}
