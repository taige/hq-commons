package io.hqwu.commons.boot4compat;

import io.hqwu.commons.email.EmailService;
import io.hqwu.commons.email.autoconfigure.EmailAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * hq-email-boot-starter（SB3 线的 starter，未做 boot4 变体）在 SB4 下自动装配是否生效。
 */
class EmailBoot4Test {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(EmailAutoConfiguration.class));

    @Test
    void emailAutoConfigurationWorksOnBoot4() {
        contextRunner
                .withPropertyValues(
                        "hq-commons.email.from-address=noreply@example.com",
                        "hq-commons.email.smtp.host=localhost"
                )
                .run(context -> assertThat(context).hasSingleBean(EmailService.class));
    }

    @Test
    void emailAutoConfigurationBacksOffWithoutProperties() {
        contextRunner.run(context -> assertThat(context).doesNotHaveBean(EmailService.class));
    }
}
