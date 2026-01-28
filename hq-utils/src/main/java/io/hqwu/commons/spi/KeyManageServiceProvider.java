package io.hqwu.commons.spi;

import io.hqwu.commons.security.KeyManageService;

/**
 * KeyManageService 的服务提供者接口 (SPI)。
 * 用于解耦核心服务与具体的服务发现机制（如 Spring）。
 *
 * @author Wu, Hongqiang
 * @since 2026-01-28
 */
@FunctionalInterface
public interface KeyManageServiceProvider {

    /**
     * 获取 KeyManageService 实例。
     * @return KeyManageService 实例，如果没有则返回 null。
     */
    KeyManageService get();
}
