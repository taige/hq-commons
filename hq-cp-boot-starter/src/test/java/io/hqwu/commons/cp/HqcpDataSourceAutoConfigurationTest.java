package io.hqwu.commons.cp;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * HqcpDataSourceAutoConfiguration 单元测试
 */
class HqcpDataSourceAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    DataSourceAutoConfiguration.class,
                    HqcpDataSourceAutoConfiguration.class
            ));

    @Test
    void testAutoConfigurationLoaded() {
        this.contextRunner
                .withPropertyValues(
                        "spring.datasource.url=jdbc:mysql://localhost:3306/test",
                        "spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver",
                        "spring.datasource.username=root",
                        "spring.datasource.password=password"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(DataSource.class);
                    assertThat(context.getBean(DataSource.class)).isInstanceOf(HqcpDataSourceBoot.class);
                });
    }

    @Test
    void testAutoConfigurationWithHqcpProperties() {
        this.contextRunner
                .withPropertyValues(
                        "spring.datasource.url=jdbc:mysql://localhost:3306/basic",
                        "spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver",
                        "spring.datasource.username=basic_user",
                        "spring.datasource.password=basic_password",
                        "spring.datasource.hqcp.url=jdbc:mysql://localhost:3306/hqcp",
                        "spring.datasource.hqcp.username=hqcp_user",
                        "spring.datasource.hqcp.password=hqcp_password",
                        "spring.datasource.hqcp.min-connections=5",
                        "spring.datasource.hqcp.max-connections=20"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(DataSource.class);
                    var dataSource = context.getBean(HqcpDataSourceBoot.class);
                    assertThat(dataSource).isNotNull();
                    assertThat(dataSource.getUsername()).isEqualTo("hqcp_user");
                    assertThat(dataSource.getPassword()).isEqualTo("hqcp_password");
                    assertThat(dataSource.getUrl()).isEqualTo("jdbc:mysql://localhost:3306/hqcp");
                    assertThat(dataSource.getMinConnections()).isEqualTo(5);
                    assertThat(dataSource.getMaxConnections()).isEqualTo(20);
                });
    }

    @Test
    void testAutoConfigurationBeforeDataSourceAutoConfiguration() {
        this.contextRunner
                .withPropertyValues(
                        "spring.datasource.url=jdbc:mysql://localhost:3306/test",
                        "spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver",
                        "spring.datasource.username=sa",
                        "spring.datasource.password="
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(DataSource.class);
                    // HqcpDataSourceAutoConfiguration 应该先于 DataSourceAutoConfiguration 执行
                    assertThat(context.getBean(DataSource.class)).isInstanceOf(HqcpDataSourceBoot.class);
                });
    }

    @Test
    void testConditionalOnMissingBean() {
        this.contextRunner
                .withUserConfiguration(CustomDataSourceConfiguration.class)
                .withPropertyValues(
                        "spring.datasource.url=jdbc:mysql://localhost:3306/test",
                        "spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(DataSource.class);
                    // 应该使用用户自定义的 DataSource，而不是自动配置的
                    assertThat(context.getBean(DataSource.class)).isInstanceOf(CustomDataSource.class);
                });
    }

    @Test
    void testWithOnlyBasicProperties() {
        this.contextRunner
                .withPropertyValues(
                        "spring.datasource.url=jdbc:mysql://localhost:3306/basic",
                        "spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver",
                        "spring.datasource.username=basic_user",
                        "spring.datasource.password=basic_pass"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(DataSource.class);
                    var dataSource = context.getBean(HqcpDataSourceBoot.class);
                    // 验证使用了基础属性
                    assertThat(dataSource.getUsername()).isEqualTo("basic_user");
                    assertThat(dataSource.getPassword()).isEqualTo("basic_pass");
                    assertThat(dataSource.getUrl()).isEqualTo("jdbc:mysql://localhost:3306/basic");
                });
    }

    @Test
    void testConfigurationIsConditionalOnClass() {
        this.contextRunner
                .withPropertyValues(
                        "spring.datasource.url=jdbc:mysql://localhost:3306/test",
                        "spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver"
                )
                .run(context -> {
                    // 验证配置类被正确加载
                    assertThat(context).hasBean("dataSource");
                    assertThat(context.getBean("dataSource")).isInstanceOf(HqcpDataSourceBoot.class);
                });
    }

    /**
     * 自定义 DataSource 配置（用于测试 @ConditionalOnMissingBean）
     */
    @Configuration
    static class CustomDataSourceConfiguration {
        @Bean
        public DataSource dataSource() {
            return new CustomDataSource();
        }
    }

    /**
     * 自定义 DataSource 实现（用于测试）
     */
    static class CustomDataSource extends HqcpDataSource {
        // 标记类，用于类型判断
    }
}
