package io.hqwu.commons.email.autoconfigure;

import io.hqwu.commons.email.DefaultEmailServiceImpl;
import io.hqwu.commons.email.DefaultEmailSessionFactory;
import io.hqwu.commons.email.EmailService;
import io.hqwu.commons.email.EmailService.EmailSessionFactory;
import jakarta.mail.Address;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.internet.InternetAddress;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link EmailAutoConfiguration} 单元测试
 */
class EmailAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(EmailAutoConfiguration.class));

    @Test
    void shouldNotLoadWhenFromAddressIsMissing() {
        contextRunner
                .withPropertyValues("hq-commons.email.smtp.host=smtp.example.com")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(EmailService.class);
                    assertThat(context).doesNotHaveBean(EmailSessionFactory.class);
                });
    }

    @Test
    void shouldNotLoadWhenSmtpHostIsMissing() {
        contextRunner
                .withPropertyValues("hq-commons.email.from-address=test@example.com")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(EmailService.class);
                    assertThat(context).doesNotHaveBean(EmailSessionFactory.class);
                });
    }

    @Test
    void shouldLoadWithMinimalConfiguration() {
        contextRunner
                .withPropertyValues(
                        "hq-commons.email.from-address=sender@example.com",
                        "hq-commons.email.smtp.host=smtp.example.com"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(EmailService.class);
                    assertThat(context).hasSingleBean(EmailSessionFactory.class);
                    assertThat(context).hasSingleBean(Address.class);
                    assertThat(context).hasBean("smtpProperties");
                    // passwordAuthentication bean 存在但值为 null（因为没有配置 auth）
                    assertThat(context).hasBean("passwordAuthentication");
                });
    }

    @Test
    void shouldCreatePasswordAuthenticationWhenAuthConfigured() {
        contextRunner
                .withPropertyValues(
                        "hq-commons.email.from-address=sender@example.com",
                        "hq-commons.email.smtp.host=smtp.example.com",
                        "hq-commons.email.auth.username=user@example.com",
                        "hq-commons.email.auth.password=secret"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(PasswordAuthentication.class);
                    var passwordAuth = context.getBean(PasswordAuthentication.class);
                    assertThat(passwordAuth.getUserName()).isEqualTo("user@example.com");
                    assertThat(passwordAuth.getPassword()).isEqualTo("secret");
                });
    }

    @Test
    void shouldConfigureSmtpProperties() {
        contextRunner
                .withPropertyValues(
                        "hq-commons.email.from-address=sender@example.com",
                        "hq-commons.email.smtp.host=smtp.gmail.com",
                        "hq-commons.email.smtp.port=587",
                        "hq-commons.email.smtp.starttls.enable=true"
                )
                .run(context -> {
                    var props = (Properties) context.getBean("smtpProperties");
                    assertThat(props.getProperty("mail.smtp.host")).isEqualTo("smtp.gmail.com");
                    assertThat(props.getProperty("mail.smtp.port")).isEqualTo("587");
                    assertThat(props.getProperty("mail.smtp.starttls.enable")).isEqualTo("true");
                });
    }

    @Test
    void shouldAutoEnableSmtpAuthWhenAuthConfigured() {
        contextRunner
                .withPropertyValues(
                        "hq-commons.email.from-address=sender@example.com",
                        "hq-commons.email.smtp.host=smtp.example.com",
                        "hq-commons.email.auth.username=user@example.com",
                        "hq-commons.email.auth.password=secret"
                )
                .run(context -> {
                    var props = (Properties) context.getBean("smtpProperties");
                    // Properties 中存储的是 Boolean 对象
                    assertThat(props.get("mail.smtp.auth")).isEqualTo(true);
                });
    }

    @Test
    void shouldNotAutoEnableSmtpAuthWhenExplicitlyConfigured() {
        contextRunner
                .withPropertyValues(
                        "hq-commons.email.from-address=sender@example.com",
                        "hq-commons.email.smtp.host=smtp.example.com",
                        "hq-commons.email.smtp.auth=false",
                        "hq-commons.email.auth.username=user@example.com",
                        "hq-commons.email.auth.password=secret"
                )
                .run(context -> {
                    var props = (Properties) context.getBean("smtpProperties");
                    assertThat(props.getProperty("mail.smtp.auth")).isEqualTo("false");
                });
    }

    @Test
    void shouldCreateFromAddressWithoutNickname() {
        contextRunner
                .withPropertyValues(
                        "hq-commons.email.from-address=sender@example.com",
                        "hq-commons.email.smtp.host=smtp.example.com"
                )
                .run(context -> {
                    var address = context.getBean(Address.class);
                    assertThat(address).isInstanceOf(InternetAddress.class);
                    assertThat(address.toString()).isEqualTo("sender@example.com");
                });
    }

    @Test
    void shouldCreateFromAddressWithNickname() {
        contextRunner
                .withPropertyValues(
                        "hq-commons.email.from-address=sender@example.com",
                        "hq-commons.email.from-nickname=Test Sender",
                        "hq-commons.email.smtp.host=smtp.example.com"
                )
                .run(context -> {
                    var address = context.getBean(Address.class);
                    assertThat(address).isInstanceOf(InternetAddress.class);
                    // 昵称会被 MIME 编码，验证包含邮箱地址即可
                    assertThat(address.toString()).contains("sender@example.com");
                    // 验证确实是带昵称的格式
                    assertThat(address.toString()).contains("Test Sender");
                });
    }

    @Test
    void shouldCreateFromAddressWithChineseNickname() {
        contextRunner
                .withSystemProperties("mail.mime.charset=UTF-8")  // 在 Spring 上下文创建时设置系统属性
                .withPropertyValues(
                        "hq-commons.email.from-address=sender@example.com",
                        "hq-commons.email.from-nickname=测试发件人",
                        "hq-commons.email.smtp.host=smtp.example.com"
                )
                .run(context -> {
                    var address = context.getBean(Address.class);
                    assertThat(address).isInstanceOf(InternetAddress.class);
                    var addressString = address.toString();
                    // 中文昵称会被 MIME 编码为 Base64 格式，例如 =?UTF-8?B?...?= <email>
                    assertThat(addressString).contains("sender@example.com");
                    // 验证包含 MIME 编码标记（表示昵称被正确编码）
                    assertThat(addressString).startsWith("=?UTF-8?B?");
                    assertThat(addressString).contains("?=");
                });
    }

    @Test
    void shouldCreateDefaultEmailSessionFactory() {
        contextRunner
                .withPropertyValues(
                        "hq-commons.email.from-address=sender@example.com",
                        "hq-commons.email.smtp.host=smtp.example.com"
                )
                .run(context -> {
                    var factory = context.getBean(EmailSessionFactory.class);
                    assertThat(factory).isInstanceOf(DefaultEmailSessionFactory.class);
                });
    }

    @Test
    void shouldConfigureDebugMode() {
        contextRunner
                .withPropertyValues(
                        "hq-commons.email.from-address=sender@example.com",
                        "hq-commons.email.smtp.host=smtp.example.com",
                        "hq-commons.email.debug.enabled=true",
                        "hq-commons.email.debug.logger=email.debug"
                )
                .run(context -> {
                    var factory = context.getBean(EmailSessionFactory.class);
                    assertThat(factory).isInstanceOf(DefaultEmailSessionFactory.class);
                    // Debug 配置会传递到 factory，但无法直接验证私有字段
                    // 通过创建 Session 来间接验证
                    var session = factory.getSession();
                    assertThat(session).isNotNull();
                });
    }

    @Test
    void shouldCreateDefaultEmailService() {
        contextRunner
                .withPropertyValues(
                        "hq-commons.email.from-address=sender@example.com",
                        "hq-commons.email.smtp.host=smtp.example.com"
                )
                .run(context -> {
                    var emailService = context.getBean(EmailService.class);
                    assertThat(emailService).isInstanceOf(DefaultEmailServiceImpl.class);
                });
    }

    @Test
    void shouldAllowCustomEmailSessionFactory() {
        contextRunner
                .withPropertyValues(
                        "hq-commons.email.from-address=sender@example.com",
                        "hq-commons.email.smtp.host=smtp.example.com"
                )
                .withUserConfiguration(CustomEmailSessionFactoryConfig.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(EmailSessionFactory.class);
                    // 自定义 factory 会覆盖默认实现
                    assertThat(context).hasBean("customEmailSessionFactory");
                    assertThat(context).doesNotHaveBean(DefaultEmailSessionFactory.class);
                });
    }

    @Test
    void shouldAllowCustomEmailService() {
        contextRunner
                .withPropertyValues(
                        "hq-commons.email.from-address=sender@example.com",
                        "hq-commons.email.smtp.host=smtp.example.com"
                )
                .withUserConfiguration(CustomEmailServiceConfig.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(EmailService.class);
                    // 自定义 service 会覆盖默认实现
                    assertThat(context).hasBean("customEmailService");
                    assertThat(context).doesNotHaveBean(DefaultEmailServiceImpl.class);
                });
    }

    @Test
    void shouldConfigureCompleteSetup() {
        contextRunner
                .withPropertyValues(
                        "hq-commons.email.from-address=sender@example.com",
                        "hq-commons.email.from-nickname=System Admin",
                        "hq-commons.email.smtp.host=smtp.gmail.com",
                        "hq-commons.email.smtp.port=587",
                        "hq-commons.email.smtp.starttls.enable=true",
                        "hq-commons.email.smtp.ssl.trust=smtp.gmail.com",
                        "hq-commons.email.auth.username=admin@example.com",
                        "hq-commons.email.auth.password=password123",
                        "hq-commons.email.debug.enabled=true",
                        "hq-commons.email.debug.logger=io.hqwu.commons.email"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(EmailService.class);
                    assertThat(context).hasSingleBean(EmailSessionFactory.class);
                    assertThat(context).hasSingleBean(PasswordAuthentication.class);

                    var props = (Properties) context.getBean("smtpProperties");
                    assertThat(props.getProperty("mail.smtp.host")).isEqualTo("smtp.gmail.com");
                    assertThat(props.getProperty("mail.smtp.port")).isEqualTo("587");
                    assertThat(props.getProperty("mail.smtp.starttls.enable")).isEqualTo("true");
                    assertThat(props.getProperty("mail.smtp.ssl.trust")).isEqualTo("smtp.gmail.com");
                    // Boolean 对象存储在 Properties 中
                    assertThat(props.get("mail.smtp.auth")).isEqualTo(true);

                    var passwordAuth = context.getBean(PasswordAuthentication.class);
                    assertThat(passwordAuth.getUserName()).isEqualTo("admin@example.com");

                    var address = context.getBean(Address.class);
                    assertThat(address.toString()).contains("sender@example.com");
                });
    }

    @Configuration
    static class CustomEmailSessionFactoryConfig {
        @Bean
        public EmailSessionFactory customEmailSessionFactory() {
            return () -> null; // 简化实现用于测试
        }
    }

    @Configuration
    static class CustomEmailServiceConfig {
        @Bean
        public EmailService customEmailService() {
            return message -> { /* 简化实现用于测试 */ };
        }
    }
}
