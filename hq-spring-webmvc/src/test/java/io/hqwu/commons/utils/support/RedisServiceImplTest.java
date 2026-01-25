package io.hqwu.commons.utils.support;

import io.hqwu.commons.utils.RedisService.RedisLock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * {@link RedisServiceImpl} 的单元测试类（使用Mock，不需要真实Redis服务器）。
 * <p>
 * 测试覆盖：
 * <ul>
 *   <li>基本操作：set, get, exists, del等Redis基础命令</li>
 *   <li>过期时间：expire设置和获取</li>
 *   <li>Key前缀：带前缀和不带前缀的场景</li>
 *   <li>分布式锁：tryLock, release, 重入锁等</li>
 *   <li>异常处理：空key、Redis异常等边界情况</li>
 *   <li>批量操作：批量删除等</li>
 * </ul>
 * </p>
 *
 * @author taige (Wu, Hongqiang)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RedisServiceImpl 单元测试")
class RedisServiceImplTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    private RedisServiceImpl redisService;

    @BeforeEach
    void setUp() {
        redisService = new RedisServiceImpl(redisTemplate);
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    // ==================== 基本操作测试 ====================

    @Test
    @DisplayName("测试set方法 - 带超时时间")
    void testSetWithTimeout() {
        String key = "testKey";
        String value = "testValue";
        long timeout = 60L;

        boolean result = redisService.set(key, value, timeout);

        assertTrue(result);
        verify(valueOperations).set(eq(key), eq(value), eq(timeout), eq(TimeUnit.SECONDS));
    }

    @Test
    @DisplayName("测试set方法 - 不带超时时间")
    void testSetWithoutTimeout() {
        String key = "testKey";
        String value = "testValue";

        boolean result = redisService.set(key, value, 0);

        assertTrue(result);
        verify(valueOperations).set(eq(key), eq(value));
        verify(valueOperations, never()).set(anyString(), any(), anyLong(), any(TimeUnit.class));
    }

    @Test
    @DisplayName("测试set方法 - 负超时时间")
    void testSetWithNegativeTimeout() {
        String key = "testKey";
        String value = "testValue";

        boolean result = redisService.set(key, value, -1);

        assertTrue(result);
        verify(valueOperations).set(eq(key), eq(value));
    }

    @Test
    @DisplayName("测试set方法 - key为空")
    void testSetWithBlankKey() {
        assertThrows(IllegalArgumentException.class, () -> {
            redisService.set("", "value", 60);
        });
        assertThrows(IllegalArgumentException.class, () -> {
            redisService.set(null, "value", 60);
        });
        assertThrows(IllegalArgumentException.class, () -> {
            redisService.set("   ", "value", 60);
        });
    }

    @Test
    @DisplayName("测试get方法 - 正常获取")
    void testGet() {
        String key = "testKey";
        String expectedValue = "testValue";
        when(valueOperations.get(key)).thenReturn(expectedValue);

        Object result = redisService.get(key);

        assertEquals(expectedValue, result);
        verify(valueOperations).get(key);
    }

    @Test
    @DisplayName("测试get方法 - key不存在")
    void testGetKeyNotExists() {
        String key = "nonExistentKey";
        when(valueOperations.get(key)).thenReturn(null);

        Object result = redisService.get(key);

        assertNull(result);
        verify(valueOperations).get(key);
    }

    @Test
    @DisplayName("测试get方法 - key为空")
    void testGetWithBlankKey() {
        assertThrows(IllegalArgumentException.class, () -> {
            redisService.get("");
        });
    }

    @Test
    @DisplayName("测试exists方法 - key存在")
    void testExistsKeyExists() {
        String key = "existingKey";
        when(redisTemplate.hasKey(key)).thenReturn(true);

        boolean result = redisService.exists(key);

        assertTrue(result);
        verify(redisTemplate).hasKey(key);
    }

    @Test
    @DisplayName("测试exists方法 - key不存在")
    void testExistsKeyNotExists() {
        String key = "nonExistentKey";
        when(redisTemplate.hasKey(key)).thenReturn(false);

        boolean result = redisService.exists(key);

        assertFalse(result);
        verify(redisTemplate).hasKey(key);
    }

    @Test
    @DisplayName("测试del方法 - 单个key")
    void testDelSingleKey() {
        String key = "keyToDelete";
        when(redisTemplate.delete(key)).thenReturn(true);

        redisService.del(key);

        verify(redisTemplate).delete(key);
    }

    @Test
    @DisplayName("测试del方法 - 多个key")
    void testDelMultipleKeys() {
        String[] keys = {"key1", "key2", "key3"};
        List<String> keyList = Arrays.asList(keys);
        when(redisTemplate.delete(anyList())).thenReturn(3L);

        redisService.del(keys);

        verify(redisTemplate).delete(argThat((List<String> list) ->
            list != null && list.size() == 3 &&
            list.containsAll(keyList)
        ));
    }

    @Test
    @DisplayName("测试keys方法 - 查找匹配的key")
    void testKeys() {
        String pattern = "test*";
        Set<String> expectedKeys = new HashSet<>(Arrays.asList("test1", "test2", "test3"));
        when(redisTemplate.keys(pattern)).thenReturn(expectedKeys);

        Set<String> result = redisService.keys(pattern);

        assertEquals(expectedKeys, result);
        verify(redisTemplate).keys(pattern);
    }

    @Test
    @DisplayName("测试keys方法 - 无匹配结果")
    void testKeysNoMatch() {
        String pattern = "nonexistent*";
        when(redisTemplate.keys(pattern)).thenReturn(new HashSet<>());

        Set<String> result = redisService.keys(pattern);

        assertTrue(result.isEmpty());
        verify(redisTemplate).keys(pattern);
    }

    // ==================== 过期时间测试 ====================

    @Test
    @DisplayName("测试expire方法 - 设置过期时间")
    void testSetExpire() {
        String key = "testKey";
        long timeout = 120L;
        when(redisTemplate.expire(key, timeout, TimeUnit.SECONDS)).thenReturn(true);

        boolean result = redisService.expire(key, timeout);

        assertTrue(result);
        verify(redisTemplate).expire(key, timeout, TimeUnit.SECONDS);
    }

    @Test
    @DisplayName("测试expire方法 - 设置零或负过期时间")
    void testSetExpireWithZeroOrNegative() {
        String key = "testKey";

        boolean result1 = redisService.expire(key, 0);
        assertFalse(result1);

        boolean result2 = redisService.expire(key, -1);
        assertFalse(result2);

        verify(redisTemplate, never()).expire(anyString(), anyLong(), any(TimeUnit.class));
    }

    @Test
    @DisplayName("测试expire方法 - 获取过期时间")
    void testGetExpire() {
        String key = "testKey";
        long expectedExpire = 100L;
        when(redisTemplate.getExpire(key, TimeUnit.SECONDS)).thenReturn(expectedExpire);

        long result = redisService.expire(key);

        assertEquals(expectedExpire, result);
        verify(redisTemplate).getExpire(key, TimeUnit.SECONDS);
    }

    @Test
    @DisplayName("测试expire方法 - 获取不存在key的过期时间")
    void testGetExpireKeyNotExists() {
        String key = "nonExistentKey";
        when(redisTemplate.getExpire(key, TimeUnit.SECONDS)).thenReturn(-2L);

        long result = redisService.expire(key);

        assertEquals(-2L, result);
    }

    // ==================== Key前缀测试 ====================

    @Test
    @DisplayName("测试keyPrefix - set和get操作")
    void testKeyPrefixSetAndGet() {
        String prefix = "app:";
        redisService.setKeyPrefix(prefix);
        String key = "testKey";
        String value = "testValue";

        redisService.set(key, value, 60);

        verify(valueOperations).set(eq(prefix + key), eq(value), eq(60L), eq(TimeUnit.SECONDS));

        when(valueOperations.get(prefix + key)).thenReturn(value);
        Object result = redisService.get(key);

        assertEquals(value, result);
        verify(valueOperations).get(prefix + key);
    }

    @Test
    @DisplayName("测试keyPrefix - keys操作返回去掉前缀的key")
    void testKeyPrefixKeys() {
        String prefix = "app:";
        redisService.setKeyPrefix(prefix);
        String pattern = "test*";
        Set<String> redisKeys = new HashSet<>(Arrays.asList("app:test1", "app:test2", "app:test3"));
        when(redisTemplate.keys(prefix + pattern)).thenReturn(redisKeys);

        Set<String> result = redisService.keys(pattern);

        assertEquals(3, result.size());
        assertTrue(result.contains("test1"));
        assertTrue(result.contains("test2"));
        assertTrue(result.contains("test3"));
        assertFalse(result.stream().anyMatch(k -> k.startsWith(prefix)));
    }

    @Test
    @DisplayName("测试keyPrefix - delete操作")
    void testKeyPrefixDelete() {
        String prefix = "app:";
        redisService.setKeyPrefix(prefix);
        String key = "testKey";
        when(redisTemplate.delete(prefix + key)).thenReturn(true);

        redisService.del(key);

        verify(redisTemplate).delete(prefix + key);
    }

    // ==================== 分布式锁测试 ====================

    @Test
    @DisplayName("测试RedisLock - tryLock成功")
    void testRedisLockTryLockSuccess() {
        String key = "lockKey";
        long timeout = 30L;
        when(valueOperations.setIfAbsent(eq(key), anyString(), eq(timeout), eq(TimeUnit.SECONDS)))
            .thenReturn(true);

        RedisLock lock = redisService.redisLock(key, timeout);
        boolean locked = lock.tryLock();

        assertTrue(locked);
        verify(valueOperations).setIfAbsent(eq(key), anyString(), eq(timeout), eq(TimeUnit.SECONDS));
    }

    @Test
    @DisplayName("测试RedisLock - tryLock失败")
    void testRedisLockTryLockFailure() {
        String key = "lockKey";
        long timeout = 30L;
        when(valueOperations.setIfAbsent(eq(key), anyString(), eq(timeout), eq(TimeUnit.SECONDS)))
            .thenReturn(false);

        RedisLock lock = redisService.redisLock(key, timeout);
        boolean locked = lock.tryLock();

        assertFalse(locked);
    }

    @Test
    @DisplayName("测试RedisLock - 重入锁")
    void testRedisLockReentrant() {
        String key = "lockKey";
        long timeout = 30L;
        when(valueOperations.setIfAbsent(eq(key), anyString(), eq(timeout), eq(TimeUnit.SECONDS)))
            .thenReturn(true);
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any()))
            .thenReturn(1L);

        RedisLock lock = redisService.redisLock(key, timeout);

        assertTrue(lock.tryLock()); // 第一次加锁
        assertTrue(lock.tryLock()); // 重入
        assertTrue(lock.tryLock()); // 再次重入

        // 只调用一次setIfAbsent
        verify(valueOperations, times(1)).setIfAbsent(eq(key), anyString(), eq(timeout), eq(TimeUnit.SECONDS));

        // 释放锁，重入次数递减
        assertTrue(lock.release()); // lockCount 3 -> 2
        assertTrue(lock.release()); // lockCount 2 -> 1
        assertTrue(lock.release()); // lockCount 1 -> 0, 真正释放

        // 只在最后一次release时才真正执行lua脚本
        verify(redisTemplate, times(1)).execute(any(RedisScript.class), anyList(), any());
    }

    @Test
    @DisplayName("测试RedisLock - release成功")
    void testRedisLockReleaseSuccess() {
        String key = "lockKey";
        long timeout = 30L;
        when(valueOperations.setIfAbsent(eq(key), anyString(), eq(timeout), eq(TimeUnit.SECONDS)))
            .thenReturn(true);
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any()))
            .thenReturn(1L);

        RedisLock lock = redisService.redisLock(key, timeout);
        lock.tryLock();
        boolean released = lock.release();

        assertTrue(released);
        verify(redisTemplate).execute(any(RedisScript.class), anyList(), any());
    }

    @Test
    @DisplayName("测试RedisLock - 同一线程获取同一个锁实例")
    void testRedisLockSameInstanceInSameThread() {
        String key = "lockKey";
        long timeout = 30L;

        RedisLock lock1 = redisService.redisLock(key, timeout);
        RedisLock lock2 = redisService.redisLock(key, timeout);

        assertSame(lock1, lock2, "同一线程应该获取到同一个锁实例");
    }

    @Test
    @DisplayName("测试RedisLock - tryLock带超时时间成功")
    void testRedisLockTryLockWithTimeoutSuccess() {
        String key = "lockKey";
        long timeout = 30L;
        when(valueOperations.setIfAbsent(eq(key), anyString(), eq(timeout), eq(TimeUnit.SECONDS)))
            .thenReturn(true);

        RedisLock lock = redisService.redisLock(key, timeout);
        boolean locked = lock.tryLock(1, TimeUnit.SECONDS);

        assertTrue(locked);
    }

    @Test
    @DisplayName("测试RedisLock - tryLock带超时时间失败")
    void testRedisLockTryLockWithTimeoutFailure() {
        String key = "lockKey";
        long timeout = 30L;
        when(valueOperations.setIfAbsent(eq(key), anyString(), eq(timeout), eq(TimeUnit.SECONDS)))
            .thenReturn(false);

        RedisLock lock = redisService.redisLock(key, timeout);
        boolean locked = lock.tryLock(100, TimeUnit.MILLISECONDS);

        assertFalse(locked);
        // 应该尝试多次
        verify(valueOperations, atLeast(2)).setIfAbsent(eq(key), anyString(), eq(timeout), eq(TimeUnit.SECONDS));
    }

    @Test
    @DisplayName("测试RedisLock - tryLock重入带超时时间")
    void testRedisLockTryLockWithTimeoutReentrant() {
        String key = "lockKey";
        long timeout = 30L;
        when(valueOperations.setIfAbsent(eq(key), anyString(), eq(timeout), eq(TimeUnit.SECONDS)))
            .thenReturn(true);

        RedisLock lock = redisService.redisLock(key, timeout);
        assertTrue(lock.tryLock());
        assertTrue(lock.tryLock(1, TimeUnit.SECONDS)); // 重入应该立即成功

        // 重入不应该再次调用setIfAbsent
        verify(valueOperations, times(1)).setIfAbsent(eq(key), anyString(), eq(timeout), eq(TimeUnit.SECONDS));
    }

    @Test
    @DisplayName("测试RedisLock - 多线程场景")
    void testRedisLockMultiThreaded() throws InterruptedException {
        String key = "lockKey";
        long timeout = 30L;
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(10);

        // 第一个线程获取锁成功，其他线程失败
        AtomicBoolean firstLock = new AtomicBoolean(true);
        when(valueOperations.setIfAbsent(eq(key), anyString(), eq(timeout), eq(TimeUnit.SECONDS)))
            .thenAnswer(invocation -> {
                if (firstLock.compareAndSet(true, false)) {
                    return true;
                }
                return false;
            });

        // 创建10个线程尝试获取锁
        for (int i = 0; i < 10; i++) {
            new Thread(() -> {
                try {
                    RedisLock lock = redisService.redisLock(key, timeout);
                    if (lock.tryLock()) {
                        successCount.incrementAndGet();
                    } else {
                        failCount.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            }).start();
        }

        latch.await(5, TimeUnit.SECONDS);

        // 只有一个线程应该成功获取锁
        assertEquals(1, successCount.get());
        assertEquals(9, failCount.get());
    }

    // ==================== 异常处理测试 ====================

    @Test
    @DisplayName("测试异常处理 - Redis操作异常")
    void testExceptionHandling() {
        String key = "testKey";
        when(valueOperations.get(key)).thenThrow(new RuntimeException("Redis connection error"));

        Object result = redisService.get(key);

        assertNull(result); // 异常时应返回null
    }

    @Test
    @DisplayName("测试异常处理 - set操作异常")
    void testSetExceptionHandling() {
        String key = "testKey";
        String value = "testValue";
        doThrow(new RuntimeException("Redis connection error"))
            .when(valueOperations).set(anyString(), any(), anyLong(), any(TimeUnit.class));

        boolean result = redisService.set(key, value, 60);

        assertFalse(result); // 异常时应返回false
    }

    @Test
    @DisplayName("测试异常处理 - exists操作异常")
    void testExistsExceptionHandling() {
        String key = "testKey";
        when(redisTemplate.hasKey(key)).thenThrow(new RuntimeException("Redis connection error"));

        boolean result = redisService.exists(key);

        assertFalse(result); // 异常时应返回false
    }

    @Test
    @DisplayName("测试异常处理 - delete操作异常")
    void testDeleteExceptionHandling() {
        String key = "testKey";
        when(redisTemplate.delete(key)).thenThrow(new RuntimeException("Redis connection error"));

        // 不应该抛出异常
        assertDoesNotThrow(() -> redisService.del(key));
    }

    @Test
    @DisplayName("测试异常处理 - 批量delete操作异常")
    void testBatchDeleteExceptionHandling() {
        String[] keys = {"key1", "key2", "key3"};
        when(redisTemplate.delete(anyList())).thenThrow(new RuntimeException("Redis connection error"));

        // 不应该抛出异常
        assertDoesNotThrow(() -> redisService.del(keys));
    }

    // ==================== 边界条件测试 ====================

    @Test
    @DisplayName("测试边界条件 - 空数组删除")
    void testDelWithEmptyArray() {
        // 空数组不应该执行任何操作
        redisService.del();

        verify(redisTemplate, never()).delete(anyString());
        verify(redisTemplate, never()).delete(anyList());
    }

    @Test
    @DisplayName("测试边界条件 - value为null")
    void testSetWithNullValue() {
        String key = "testKey";

        boolean result = redisService.set(key, null, 60);

        assertTrue(result);
        verify(valueOperations).set(eq(key), isNull(), eq(60L), eq(TimeUnit.SECONDS));
    }

    @Test
    @DisplayName("测试边界条件 - 空keyPrefix")
    void testEmptyKeyPrefix() {
        redisService.setKeyPrefix("");
        String key = "testKey";
        String value = "testValue";

        redisService.set(key, value, 60);

        // 空前缀应该等同于无前缀
        verify(valueOperations).set(eq(key), eq(value), eq(60L), eq(TimeUnit.SECONDS));
    }

    @Test
    @DisplayName("测试边界条件 - null keyPrefix")
    void testNullKeyPrefix() {
        redisService.setKeyPrefix(null);
        String key = "testKey";
        String value = "testValue";

        redisService.set(key, value, 60);

        // null前缀应该等同于无前缀
        verify(valueOperations).set(eq(key), eq(value), eq(60L), eq(TimeUnit.SECONDS));
    }

    @Test
    @DisplayName("测试边界条件 - 重复设置keyPrefix")
    void testSetKeyPrefixMultipleTimes() {
        redisService.setKeyPrefix("prefix1:");
        redisService.setKeyPrefix("prefix2:");
        String key = "testKey";
        String value = "testValue";

        redisService.set(key, value, 60);

        // 应该使用最后设置的前缀
        verify(valueOperations).set(eq("prefix2:" + key), eq(value), eq(60L), eq(TimeUnit.SECONDS));
    }

    @Test
    @DisplayName("测试边界条件 - 重复delete相同的keys")
    void testDelWithDuplicateKeys() {
        String[] keys = {"key1", "key1", "key2", "key2", "key3"};
        when(redisTemplate.delete(anyList())).thenReturn(3L);

        redisService.del(keys);

        // 应该去重后删除
        verify(redisTemplate).delete(argThat((List<String> list) ->
            list != null && list.size() == 3
        ));
    }

    @Test
    @DisplayName("测试边界条件 - keys中包含null")
    void testDelWithNullInKeys() {
        String[] keys = {"key1", null, "key2", null};
        when(redisTemplate.delete(anyList())).thenReturn(2L);

        redisService.del(keys);

        // null应该被过滤掉
        verify(redisTemplate).delete(argThat((List<String> list) ->
            list != null && list.size() == 2 && !list.contains(null)
        ));
    }

    @Test
    @DisplayName("测试RedisLock - 锁过期后release")
    void testRedisLockReleaseAfterExpired() {
        String key = "lockKey";
        long timeout = 30L;
        when(valueOperations.setIfAbsent(eq(key), anyString(), eq(timeout), eq(TimeUnit.SECONDS)))
            .thenReturn(true);
        // 模拟锁已过期，返回0
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any()))
            .thenReturn(0L);

        RedisLock lock = redisService.redisLock(key, timeout);
        lock.tryLock();
        boolean released = lock.release();

        assertTrue(released); // 即使锁已过期，也应该返回true
    }

    @Test
    @DisplayName("测试RedisLock - release执行异常")
    void testRedisLockReleaseWithException() {
        String key = "lockKey";
        long timeout = 30L;
        when(valueOperations.setIfAbsent(eq(key), anyString(), eq(timeout), eq(TimeUnit.SECONDS)))
            .thenReturn(true);
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any()))
            .thenThrow(new RuntimeException("Redis error"));

        RedisLock lock = redisService.redisLock(key, timeout);
        lock.tryLock();
        boolean released = lock.release();

        assertFalse(released); // 异常时应返回false
    }

    @Test
    @DisplayName("测试RedisLock - 未加锁就release")
    void testRedisLockReleaseWithoutLock() {
        String key = "lockKey";
        long timeout = 30L;

        RedisLock lock = redisService.redisLock(key, timeout);
        boolean released = lock.release();

        assertTrue(released); // 未加锁时release应该返回true
        verify(redisTemplate, never()).execute(any(RedisScript.class), anyList(), any());
    }

    @Test
    @DisplayName("测试expire方法 - 异常处理返回默认值 (覆盖第82行)")
    void testExpireExceptionReturnsDefault() {
        String key = "testKey";
        long timeout = 120L;
        when(redisTemplate.expire(key, timeout, TimeUnit.SECONDS))
            .thenThrow(new RuntimeException("Redis connection error"));

        boolean result = redisService.expire(key, timeout);

        assertFalse(result); // 异常时应返回false (第82行的默认值)
    }

    @Test
    @DisplayName("测试RedisLock.tryLock - 异常处理返回默认值 (覆盖第165行)")
    void testTryLockExceptionReturnsDefault() {
        String key = "lockKey";
        long timeout = 30L;
        when(valueOperations.setIfAbsent(eq(key), anyString(), eq(timeout), eq(TimeUnit.SECONDS)))
            .thenThrow(new RuntimeException("Redis connection error"));

        RedisLock lock = redisService.redisLock(key, timeout);
        boolean locked = lock.tryLock();

        assertFalse(locked); // 异常时应返回false (第165行的默认值)
    }

    @Test
    @DisplayName("测试RedisLock.tryLock带超时 - 异常处理返回默认值 (覆盖第193行)")
    void testTryLockWithTimeoutExceptionReturnsDefault() {
        String key = "lockKey";
        long timeout = 30L;
        when(valueOperations.setIfAbsent(eq(key), anyString(), eq(timeout), eq(TimeUnit.SECONDS)))
            .thenThrow(new RuntimeException("Redis connection error"));

        RedisLock lock = redisService.redisLock(key, timeout);
        boolean locked = lock.tryLock(100, TimeUnit.MILLISECONDS);

        assertFalse(locked); // 异常时应返回false (第193行的默认值)
    }
}
