package io.hqwu.commons.config;

import io.hqwu.commons.utils.RedisService;
import io.hqwu.commons.utils.support.RedisServiceImpl;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Created with IntelliJ IDEA for qrcode-api-server
 *
 * @author taige (Wu, Hongqiang)
 * Date: 2021-09-16
 * Time: 17:30
 */
@TestConfiguration
@ImportAutoConfiguration({JacksonAutoConfiguration.class})
public class RedisConfig {

    /**
     * 使用Json序列化保存对象到redis
     * @param redisConnectionFactory
     * @return
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(
            RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, Object>  template= new RedisTemplate<>();

        //设置key的序列化和反序列化类型 采用StringRedisSerializer
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());

        //设置value的序列化和反序列化类型 采用GenericJackson2JsonRedisSerializer
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());

        template.setConnectionFactory(redisConnectionFactory);
        return template;
    }

    /**
     * 直接操作redis时的实用工具Service
     * （大部分redis使用场景通过spring cache完成）
     * @param redisTemplate
     * @return
     */
    @Bean
    @ConfigurationProperties(prefix = "spring.cache.redis")
    public RedisService redisService(
            RedisTemplate<String, Object> redisTemplate) {
        return new RedisServiceImpl(redisTemplate);
    }

}
