package io.hqwu.commons.boot4compat;

import io.hqwu.commons.security.KeyManageService;
import io.hqwu.commons.spring.SpringKeyManageServiceProvider;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * hq-utils-spring 的 {@link SpringKeyManageServiceProvider}（SPI → Spring bean 桥）在 SB4 / Framework 7 下是否正常。
 */
class KeyManageServiceBoot4Test {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(SpringKeyManageServiceProvider.class);

    @Test
    void providerReturnsNullWhenNoKeyManageServiceBean() {
        contextRunner.run(context -> {
            SpringKeyManageServiceProvider provider = context.getBean(SpringKeyManageServiceProvider.class);
            assertThat(provider.get()).isNull();
        });
    }

    @Test
    void providerReturnsRegisteredKeyManageServiceBean() {
        KeyManageService remote = mock(KeyManageService.class);
        contextRunner
                .withBean("remoteKeyManageService", KeyManageService.class, () -> remote)
                .run(context -> {
                    SpringKeyManageServiceProvider provider = context.getBean(SpringKeyManageServiceProvider.class);
                    assertThat(provider.get()).isSameAs(remote);
                });
    }
}
