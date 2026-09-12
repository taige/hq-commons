package io.hqwu.commons.boot4compat;

import io.hqwu.commons.annotation.JsonField;
import io.hqwu.commons.annotation.support.JsonFieldArgumentProcessor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * 反向探针：把 SB4 默认的 Jackson 3 转换器（{@link JacksonJsonHttpMessageConverter}）交给
 * {@link JsonFieldArgumentProcessor}，记录它的失败方式。
 * <p>
 * 这条测试是"已知限制"的文档：SB4 消费方不能拿 Boot 自动装配的转换器列表直接喂给处理器。
 */
@SpringBootTest(classes = {Boot4CompatApplication.class, JsonFieldJackson3ConverterBoot4Test.Jackson3Config.class})
class JsonFieldJackson3ConverterBoot4Test {

    @Autowired
    private WebApplicationContext wac;

    @Autowired
    private EchoController controller;

    @Test
    void jsonFieldDoesNotResolveWithJackson3Converter() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();

        MvcResult result = mockMvc.perform(post("/echo3")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"user_name\": \"Zhang San\", \"user_age\": 25}"))
                .andReturn();

        // 记录到测试输出，便于人看到底怎么失败的
        System.out.println("[boot4-compat] Jackson3 converter -> status=" + result.getResponse().getStatus()
                + ", resolvedException=" + result.getResolvedException());

        assertThat(result.getResponse().getStatus()).isNotEqualTo(200);
        assertThat(controller.name).isNull();
    }

    @Configuration
    static class Jackson3Config implements WebMvcConfigurer {

        @Bean
        public JsonFieldArgumentProcessor jsonFieldArgumentProcessor() {
            List<HttpMessageConverter<?>> converters = List.of(new JacksonJsonHttpMessageConverter());
            return new JsonFieldArgumentProcessor(converters);
        }

        @Bean
        public EchoController echoController3() {
            return new EchoController();
        }

        @Override
        public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
            resolvers.add(jsonFieldArgumentProcessor());
        }
    }

    @RestController
    static class EchoController {
        String name;

        @PostMapping("/echo3")
        public void echo(@JsonField("user_name") String name) {
            this.name = name;
        }
    }
}
