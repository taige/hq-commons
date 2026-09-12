package io.hqwu.commons.boot4compat;

import io.hqwu.commons.mybatisplus.CommonFieldsFiller;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/**
 * 模拟一个 Spring Boot 4 消费方应用：HQCP（boot4 starter）做数据源，MyBatis-Plus boot4 starter 装配，
 * hq-mybatis-plus-extension 的 CommonFieldsFiller 当 MetaObjectHandler。
 */
@SpringBootApplication
@MapperScan("io.hqwu.commons.boot4compat.mapper")
public class Boot4CompatApplication {

    public static void main(String[] args) {
        SpringApplication.run(Boot4CompatApplication.class, args);
    }

    @Bean
    public CommonFieldsFiller commonFieldsFiller() {
        return new CommonFieldsFiller().setCurrentUserIdSupplier(() -> "boot4-tester");
    }
}
