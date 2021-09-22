package io.hqwu.commons.dubbo.serialize;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.umpay.commons.util.Logger;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.jackson.JsonComponentModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

/**
 * Created with IntelliJ IDEA for hq-commons-parent
 *
 * @author taige (Wu, Hongqiang)
 * Date: 2021-09-22
 * Time: 14:33
 */
@Configuration(proxyBeanMethods = false)
@ImportAutoConfiguration(JacksonAutoConfiguration.class)
@ConditionalOnMissingBean(name = JacksonSerialization.OBJECT_MAPPER_BEAN)
public class DubboObjectMapperAutoConfiguration {
    private static final Logger LOGGER = new Logger();

    @Bean
    public ObjectMapper dubboObjectMapper(
            Jackson2ObjectMapperBuilder objectMapperBuilder,
            JsonComponentModule jsonComponentModule) {
        ObjectMapper objectMapper = objectMapperBuilder.build();
        // null不写入
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        // 保留timezone
        objectMapper.disable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE);
        // 写入zone id
        objectMapper.enable(SerializationFeature.WRITE_DATES_WITH_ZONE_ID);
        objectMapper.registerModule(jsonComponentModule);
        objectMapper.enable(JsonGenerator.Feature.IGNORE_UNKNOWN);
        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        objectMapper.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);
        LOGGER.debug("default dubboObjectMapper initialized");
        return objectMapper;
    }

}
