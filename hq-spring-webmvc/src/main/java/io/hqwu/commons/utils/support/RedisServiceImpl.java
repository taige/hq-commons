package io.hqwu.commons.utils.support;

import io.hqwu.commons.util.Logger;
import io.hqwu.commons.util.StringUtil;
import io.hqwu.commons.utils.RedisService;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

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

    private <T> T safeExecute(Function<List<String>, T> executor, String[] keys, Supplier<T> valueIfNull) {
        if (keys == null || keys.length == 0) {
            throw new IllegalArgumentException("redis.key must not be blank");
        }
        T rs = null;
        try {
            rs = executor.apply(Arrays.stream(keys)
                    .map(this::prefixKey)
                    .filter(Objects::nonNull)
                    .distinct()
                    .collect(Collectors.toList()));
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
                LOGGER.debug("set %s with timeout %d sec", key, timeoutInSeconds);
            } else {
                redisTemplate.opsForValue().set(_key, value);
                LOGGER.debug("set %s", key);
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
            boolean rc = safeExecute(redisTemplate::delete, keys[0], () -> false);
            LOGGER.debug("del %s result: %s", keys[0], rc);
        } else if (keys.length > 1) {
            long rc = safeExecute(redisTemplate::delete, keys, () -> 0L);
            LOGGER.debug("del %s result: %s", Arrays.asList(keys), rc);
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

    private ThreadLocal<Map<String, RedisLock>> lockHolders = ThreadLocal.withInitial(HashMap::new);

    //定义释放锁的lua脚本
    private final static RedisScript<Long> UNLOCK_LUA_SCRIPT = new DefaultRedisScript<>(
            "if redis.call(\"get\",KEYS[1]) == ARGV[1] then return redis.call(\"del\",KEYS[1]) else return 0 end",
            Long.class
    );

    @Override
    public RedisLock redisLock(String key, long timeoutInSeconds) {
        RedisLock redisLock = lockHolders.get().get(key);
        if (redisLock != null) {
            return redisLock;
        }

        final String lockId = UUID.randomUUID().toString();
        redisLock = new RedisLock() {
            private Random random = new Random();
            private int lockCount = 0;

            @Override
            public boolean tryLock() {
                if (lockCount > 0) {
                    lockCount++;
                    LOGGER.debug("lock %s re-enter: %d", key, lockCount);
                    return true;
                }
                boolean rc = RedisServiceImpl.this.safeExecute(_key ->
                                redisTemplate.opsForValue().setIfAbsent(_key, lockId, timeoutInSeconds, TimeUnit.SECONDS),
                        key, () -> false);
                if (rc) {
                    lockCount++;
                }
                LOGGER.info("lock %s result: %s", key, rc);
                return rc;
            }

            @Override
            public boolean tryLock(long tryTime, TimeUnit unit) {
                if (lockCount > 0) {
                    lockCount++;
                    LOGGER.debug("lock %s re-enter: %d", key, lockCount);
                    return true;
                }
                final long deadline = System.nanoTime() + unit.toNanos(tryTime);
                boolean rc = tryLockUntil(deadline);
                if (rc) {
                    lockCount++;
                }
                LOGGER.debug("lock %s result: %s", key, rc);
                return rc;
            }

            private boolean tryLockUntil(long deadline) {
                while (true) {
                    boolean rc = RedisServiceImpl.this.safeExecute(_key ->
                                    redisTemplate.opsForValue().setIfAbsent(_key, lockId, timeoutInSeconds, TimeUnit.SECONDS),
                            key, () -> false);
                    if (rc) {
                        return true;
                    }
                    long nanosTimeout = deadline - System.nanoTime();
                    if (TimeUnit.NANOSECONDS.toMillis(nanosTimeout) <= 0) {
                        return false;
                    }
                    long randomSleep = Math.min(Math.max(random.nextInt(100), 10),
                            TimeUnit.NANOSECONDS.toMillis(nanosTimeout));
                    try {
                        LOGGER.trace("sleep %s ms and retry lock", randomSleep);
                        Thread.sleep(randomSleep, (int) (nanosTimeout % 1000000));
                    } catch (InterruptedException e) {
                        return false;
                    }
                }
            }

            @Override
            public boolean release() {
                if (lockCount != 1) {
                    if (lockCount > 1) {
                        lockCount--;
                    }
                    LOGGER.debug("lock %s releasing: %d", key, lockCount);
                    return true;
                }
                long rc = RedisServiceImpl.this.safeExecute(_keys ->
                                redisTemplate.execute(UNLOCK_LUA_SCRIPT, _keys, lockId),
                        new String[] { key }, () -> -1L);
                /*
                 * rc:
                 *  1 - del ok;
                 *  0 - key expired
                 *  -1 - error
                 */
                if (rc >= 0) {
                    lockCount--;
                    if (lockCount <= 0) {
                        lockHolders.get().remove(key);
                    }
                    LOGGER.debug("lock %s released: %d", key, rc);
                    return true;
                }
                LOGGER.debug("lock %s release failed: %d", key, rc);
                return false;
            }

        };
        lockHolders.get().put(key, redisLock);
        return redisLock;
    }

}
