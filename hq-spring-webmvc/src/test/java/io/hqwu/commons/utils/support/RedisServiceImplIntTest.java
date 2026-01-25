package io.hqwu.commons.utils.support;

import io.hqwu.commons.config.RedisConfig;
import io.hqwu.commons.utils.RedisService;
import io.hqwu.commons.utils.RedisService.RedisLock;
import lombok.CustomLog;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Created with IntelliJ IDEA for qrcode-api-server
 *
 * @author taige (Wu, Hongqiang)
 * Date: 2021-09-17
 * Time: 11:05
 */
@ActiveProfiles("test")
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {RedisConfig.class})
@ImportAutoConfiguration(RedisAutoConfiguration.class)
@CustomLog
@DisplayName("RedisServiceImpl 集成测试")
class RedisServiceImplIntTest {

    @Autowired
    private RedisService redisService;

    private String testKey = "1234";

    @BeforeEach
    void setUp() {
        redisService.set(testKey, "hello world", 600);
    }

    @AfterEach
    void tearDown() {
        redisService.keys("*").forEach(key -> redisService.del(key));
    }

    @Test
    void test_get_expire() {
        long exp = redisService.expire(testKey);
        LOGGER.debug("expire = %d", exp);
        assertTrue(600 >= exp && exp >= 598);
    }

    @Test
    void test_get_expire_keyNotExists() {
        long exp = redisService.expire(testKey + "-not-exists");
        assertEquals(-2, exp);
    }

    @Test
    void test_set_expire() {
        boolean rc = redisService.expire(testKey, 100);
        assertTrue(rc);
        long exp = redisService.expire(testKey);
        LOGGER.debug("expire = %d", exp);
        assertTrue(100 >= exp && exp >= 98);
    }

    @Test
    void test_set_expire_keyNotExists() {
        String key = testKey + "-not-exists";
        boolean rc = redisService.expire(key, 100);
        assertFalse(rc);
        long exp = redisService.expire(key);
        assertEquals(-2, exp);
    }

    @Test
    void test_set_and_get() {
        String key = "test-set-key";
        boolean rc = redisService.set(key, "hello world 1234", 0);
        assertTrue(rc);
        String value = (String) redisService.get(key);
        assertEquals("hello world 1234", value);
    }

    @Test
    void test_get() {
        String value = (String) redisService.get(testKey);
        assertEquals("hello world", value);
    }

    @Test
    void test_get_keyNotExists() {
        String key = "test-get-key";
        String value = (String) redisService.get(key);
        assertNull(value);
    }

    @Test
    void test_exists() {
        assertTrue(redisService.exists(testKey));
        String key = "test-exists-key";
        assertFalse(redisService.exists(key));
    }

    @Test
    void test_del() {
        redisService.set("del_key_1", "hello world 1234", 0);
        redisService.set("del_key_2", "hello world 1234", 0);
        redisService.set("del_key_3", "hello world 1234", 0);
        assertTrue(redisService.exists("del_key_1"));
        assertTrue(redisService.exists("del_key_2"));
        assertTrue(redisService.exists("del_key_3"));

        redisService.del("del_key_1");
        assertFalse(redisService.exists("del_key_1"));
        redisService.del("del_key_2", "del_key_3");
        assertFalse(redisService.exists("del_key_2"));
        assertFalse(redisService.exists("del_key_3"));
    }

    @Test
    void test_del_keyNotExists() {
        redisService.del("del_key_1");
    }

    @Test
    void test_lock_easy_everything_ok() {
        RedisLock lock = redisService.redisLock("lock-test-key", 100);
        if (lock.tryLock()) {
            try {
                LOGGER.debug("locked");
            } finally {
                lock.release();
            }
        }
    }

    @Test
    void test_lock_too_short() throws Exception {
        RedisLock lock = redisService.redisLock("lock-test-key", 2);
        if (lock.tryLock()) {
            try {
                LOGGER.debug("locked, and wait 3 sec to expired");
                Thread.sleep(3000);
            } finally {
                lock.release();
            }
        }
    }

    @Test
    void test_lock_re_enter() throws Exception {
        RedisLock lock = redisService.redisLock("lock-test-key", 100);
        if (lock.tryLock()) {
            try {
                LOGGER.debug("locked");
                assertTrue(lock.tryLock());
                assertTrue(lock.tryLock());

                RedisLock lock1 = redisService.redisLock("lock-test-key", 100);
                assertSame(lock, lock1);

                assertTrue(lock1.tryLock());
                assertTrue(lock1.release());

                assertTrue(lock.release());
                assertTrue(lock.release());
            } finally {
                lock.release();
            }
        }
    }

    @Test
    void test_lock_second_thread_lock_fail() throws Exception {
        RedisLock lock = redisService.redisLock("lock-test-key", 100);
        Thread thread = new Thread(() -> {
            RedisLock lock2 = redisService.redisLock("lock-test-key", 100);
            assertNotSame(lock, lock2);
            assertFalse(lock2.tryLock());
        });
        if (lock.tryLock()) {
            try {
                thread.start();
                LOGGER.debug("locked and mock busying");
                Thread.sleep(1000);
                thread.join();
            } finally {
                lock.release();
            }
        }
    }

    @Test
    void test_lock_second_thread_wait_and_get_lock() throws Exception {
        RedisLock lock = redisService.redisLock("lock-test-key", 100);
        Thread thread = new Thread(() -> {
            RedisLock lock2 = redisService.redisLock("lock-test-key", 100);
            try {
                assertTrue(lock2.tryLock(3, TimeUnit.SECONDS));
            } finally {
                lock2.release();
            }
        });
        if (lock.tryLock()) {
            try {
                thread.start();
                LOGGER.debug("locked and mock busying");
                Thread.sleep(1000);
            } finally {
                lock.release();
            }
        }
        thread.join();
    }

}