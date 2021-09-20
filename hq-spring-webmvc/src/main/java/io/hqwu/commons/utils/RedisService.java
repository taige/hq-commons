package io.hqwu.commons.utils;

import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Created with IntelliJ IDEA for hq-spring-webmvc
 *
 * @author taige (Wu, Hongqiang)
 * Date: 2021-09-17
 * Time: 10:03
 */
public interface RedisService {

    /**
     * 设置key超时时间
     * @param key
     * @param timeoutInSeconds
     * @return
     */
    boolean expire(String key, long timeoutInSeconds);

    /**
     * 获取key超时时间
     * @param key
     * @return -2 - key不存在
     */
    long expire(String key);

    /**
     * set key-value with timeout
     * @param key
     * @param value
     * @param timeoutInSeconds <=0 不超时
     * @return
     */
    boolean set(String key, Object value, long timeoutInSeconds);

    /**
     * get value of key
     * @param key
     * @return
     */
    Object get(String key);

    /**
     * key是否存在
     * @param key
     * @return
     */
    boolean exists(String key);

    /**
     * 删除key
     * @param keys
     */
    void del(String... keys);

    /**
     * 匹配pattern的key集合
     * @param pattern
     * @return
     */
    Set<String> keys(String pattern);

    /**
     * 获取redis lock（并不锁定）
     * @param key key of lock
     * @param timeoutInSeconds lock超时时间(超过时间自动释放)
     * @return
     */
    RedisLock redisLock(String key, long timeoutInSeconds);

    /**
     * 可重入redis lock
     */
    interface RedisLock {

        /**
         * 尝试锁定（尝试一次）
         * @return
         */
        boolean tryLock();

        /**
         * 尝试锁定（尝试指定时长）
         * @param tryTime
         * @param unit
         * @return
         */
        boolean tryLock(long tryTime, TimeUnit unit);

        /**
         * 释放锁
         * @return
         */
        boolean release();
    }
}
