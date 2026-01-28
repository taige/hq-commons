package io.hqwu.commons.spi;

import io.hqwu.commons.security.KeyManageService;
import io.hqwu.commons.security.KeyManageServiceLocalImpl;

/**
 * 测试用的 KeyManageServiceProvider 实现。
 * 用于测试 SPI 加载逻辑。
 *
 * @author Wu, Hongqiang
 * @since 2026-01-28
 */
public class TestKeyManageServiceProvider implements KeyManageServiceProvider {

    @Override
    public KeyManageService get() {
        // 返回一个自定义的 KeyManageService 实例用于测试
        return new TestKeyManageService();
    }

    /**
     * 测试用的 KeyManageService 实现类
     */
    public static class TestKeyManageService extends KeyManageServiceLocalImpl {
        // 标记类，用于在测试中识别这是通过 SPI 加载的实例
        private final String marker = "SPI_LOADED";

        public String getMarker() {
            return marker;
        }
    }
}
