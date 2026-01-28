package io.hqwu.commons.spring;

import io.hqwu.commons.security.KeyManageService;
import io.hqwu.commons.security.KeyManageServiceLocalImpl;
import io.hqwu.commons.spi.KeyManageServiceProvider;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Spring 环境下的 KeyManageService 提供者。
 * 实现了 SPI 接口，并能从 Spring 容器中查找 Bean。
 *
 * @author Wu, Hongqiang
 * @since 2026-01-28
 */
@Component
public class SpringKeyManageServiceProvider implements KeyManageServiceProvider, ApplicationContextAware {

    private static ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        SpringKeyManageServiceProvider.applicationContext = applicationContext;
    }

    @Override
    public KeyManageService get() {
        if (applicationContext == null) {
            return null;
        }

        Map<String, KeyManageService> candidates = applicationContext.getBeansOfType(KeyManageService.class);
        if (candidates.isEmpty()) {
            return null;
        }

        // 恢复“优先选择远程实现”的逻辑
        KeyManageService fallback = null;
        for (KeyManageService service : candidates.values()) {
            if (!(service instanceof KeyManageServiceLocalImpl)) {
                return service; // 找到远程实现
            }
            fallback = service; // 备选的本地实现
        }
        return fallback; // 只找到了本地实现
    }
}
