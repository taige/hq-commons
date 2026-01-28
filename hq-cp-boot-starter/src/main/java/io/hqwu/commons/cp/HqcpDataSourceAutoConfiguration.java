package io.hqwu.commons.cp;

import io.hqwu.commons.SecurityService;
import io.hqwu.commons.SecurityServiceLocalImpl;
import io.hqwu.commons.util.Logger;
import io.hqwu.commons.util.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created with IntelliJ IDEA for hq-cp-boot-starter
 *
 * @author taige (Wu, Hongqiang)
 * Date: 2021-02-21
 * Time: 8:13 p.m.
 */
@Configuration
@ConditionalOnClass(HqcpDataSource.class)
@AutoConfigureBefore(DataSourceAutoConfiguration.class)
public class HqcpDataSourceAutoConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger();

    @Bean(initMethod = "init")
    @ConditionalOnMissingBean
    public DataSource dataSource(DataSourceProperties basicProperties, ObjectProvider<SecurityService> securityServiceProvider) {
        LOGGER.debug("init HqcpDataSource");
        SecurityService selectedService = selectPreferRemote(securityServiceProvider);
        return new HqcpDataSourceBoot(basicProperties, selectedService);
    }

    /**
     * 从所有可用的 SecurityService Bean 中选择一个。
     * 优先选择非 SecurityServiceLocalImpl 的实例（即远程实现）。
     * @param provider Spring 提供的 ObjectProvider
     * @return 选中的 SecurityService，如果没有则返回 null
     */
    private SecurityService selectPreferRemote(ObjectProvider<SecurityService> provider) {
        // 获取所有 SecurityService 实例
        List<SecurityService> services = provider.orderedStream().collect(Collectors.toList());

        if (services.isEmpty()) {
            LOGGER.debug("No SecurityService bean found, will use default local implementation.");
            return null;
        }

        SecurityService fallback = null;
        for (SecurityService service : services) {
            if (!(service instanceof SecurityServiceLocalImpl)) {
                // 找到了一个远程实现，这是最佳选择
                LOGGER.info("Using remote SecurityService implementation: {}", service.getClass().getName());
                return service;
            }
            // 这是一个本地实现，先存起来作为备选
            fallback = service;
        }

        // 如果循环结束，说明只找到了本地实现
        LOGGER.info("Using local SecurityService implementation: {}", fallback.getClass().getName());
        return fallback;
    }

}
