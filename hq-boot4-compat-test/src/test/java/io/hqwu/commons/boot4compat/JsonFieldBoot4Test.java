package io.hqwu.commons.boot4compat;

import io.hqwu.commons.annotation.JsonField;
import io.hqwu.commons.annotation.support.JsonFieldArgumentProcessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SB4（默认 Jackson 3）下 {@link JsonFieldArgumentProcessor} 的可用性。
 * <p>
 * 处理器内部把 Body 解析成 Jackson 2 的 {@code Map<String, JsonNode>}，所以要给它 Jackson 2 的消息转换器
 * （{@link MappingJackson2HttpMessageConverter}，Framework 7 里已标记废弃但仍在）。这正是 SB3 消费方一直以来的接法。
 */
@SpringBootTest(classes = {Boot4CompatApplication.class, JsonFieldBoot4Test.Jackson2Config.class})
class JsonFieldBoot4Test {

    @Autowired
    private WebApplicationContext wac;

    @Autowired
    private EchoController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
        controller.name = null;
        controller.age = null;
    }

    @Test
    void jsonFieldResolvesWithExplicitJackson2Converter() throws Exception {
        mockMvc.perform(post("/echo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"user_name\": \"Zhang San\", \"user_age\": 25}"))
                .andExpect(status().isOk());

        assertEquals("Zhang San", controller.name);
        assertEquals(25, controller.age);
    }

    @Configuration
    static class Jackson2Config implements WebMvcConfigurer {

        @Bean
        public JsonFieldArgumentProcessor jsonFieldArgumentProcessor() {
            List<HttpMessageConverter<?>> converters = List.of(new MappingJackson2HttpMessageConverter());
            return new JsonFieldArgumentProcessor(converters);
        }

        @Bean
        public EchoController echoController() {
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
        Integer age;

        @PostMapping("/echo")
        public void echo(@JsonField("user_name") String name, @JsonField("user_age") Integer age) {
            this.name = name;
            this.age = age;
        }
    }
}
