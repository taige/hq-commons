package io.hqwu.commons.dubbo.serialize;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.umpay.commons.util.Logger;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.ExtensionFactory;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.serialize.ObjectInput;
import org.apache.dubbo.common.serialize.ObjectOutput;
import org.apache.dubbo.common.serialize.Serialization;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

/**
 * Created with IntelliJ IDEA for hq-dubbo-ext
 *
 * @author taige (Wu, Hongqiang)
 * Date: 2021-09-20
 * Time: 21:55
 */
public class JacksonSerialization implements Serialization, ApplicationContextAware {
    private static final Logger LOGGER = new Logger();

    public static final String OBJECT_MAPPER_BEAN = "dubboObjectMapper";

    private static ApplicationContext applicationContext;

    private ObjectMapper objectMapper;

    public JacksonSerialization() {
    }

    public JacksonSerialization(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public byte getContentTypeId() {
        return 23;
    }

    @Override
    public String getContentType() {
        return "application/json";
    }

    @Override
    public ObjectOutput serialize(URL url, OutputStream output) throws IOException {
        if (objectMapper == null) {
            initObjectMapper();
        }
        return new JacksonObjectOutput(output, objectMapper);
    }

    @Override
    public ObjectInput deserialize(URL url, InputStream input) throws IOException {
        if (objectMapper == null) {
            initObjectMapper();
        }
        return new JacksonObjectInput(input, objectMapper);
    }

    private synchronized void initObjectMapper() {
        if (objectMapper == null) {
            objectMapper = ExtensionLoader.getExtensionLoader(ExtensionFactory.class).getAdaptiveExtension().getExtension(ObjectMapper.class, OBJECT_MAPPER_BEAN);
            if (objectMapper == null) {
                objectMapper = getObjectMapper();
                if (objectMapper == null) {
                    Jackson2ObjectMapperBuilder builder = ExtensionLoader.getExtensionLoader(ExtensionFactory.class).getAdaptiveExtension().getExtension(Jackson2ObjectMapperBuilder.class, "jacksonObjectMapperBuilder");
                    if (builder != null) {
                        objectMapper = DubboObjectMapperAutoConfiguration.dubboObjectMapper(builder);
                    } else {
                        LOGGER.debug("get dubboObjectMapper buidler is NULL");
                    }
                    throw new NoSuchBeanDefinitionException(OBJECT_MAPPER_BEAN);
                }
            } else {
                LOGGER.debug("get dubboObjectMapper from ExtensionLoader");
            }
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        JacksonSerialization.applicationContext = applicationContext;
        LOGGER.debug("setApplicationContext()");
    }

    private ObjectMapper getObjectMapper() {
        if (applicationContext == null) {
            LOGGER.debug("applicationContext is NULL when getObjectMapper ...");
            return null;
        }
        ObjectMapper objectMapper;
        try {
            objectMapper = applicationContext.getBean(OBJECT_MAPPER_BEAN, ObjectMapper.class);
            LOGGER.debug("get dubboObjectMapper from ApplicationContext");
        } catch (BeansException e) {
            Map.Entry<String, ObjectMapper> entry = applicationContext.getBeansOfType(ObjectMapper.class).entrySet().stream().findAny()
                    .orElseThrow(() -> e);
            objectMapper = entry.getValue();
            LOGGER.warn("get ObjectMapper bean %s failed: %s, failover to use %s", OBJECT_MAPPER_BEAN, e.toString(), entry.getKey());
        }
        return objectMapper;
    }
}
