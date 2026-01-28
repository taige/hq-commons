package io.hqwu.commons.spi;

import io.hqwu.commons.security.KeyManageService;

/**
 * 返回 null 的测试 Provider。
 * 用于测试当 provider.get() 返回 null 时的逻辑。
 *
 * @author Wu, Hongqiang
 * @since 2026-01-28
 */
public class NullKeyManageServiceProvider implements KeyManageServiceProvider {

    @Override
    public KeyManageService get() {
        // 故意返回 null，测试 SPI 查找逻辑会跳过此 provider
        return null;
    }
}
