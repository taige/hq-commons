package io.hqwu.commons.cp;

import io.hqwu.commons.security.SecurityService;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Created with IntelliJ IDEA for hq-cp-boot-starter
 *
 * @author taige (Wu, Hongqiang)
 * Date: 2021-02-24
 * Time: 9:46 a.m.
 */
@ConfigurationProperties(prefix = "spring.datasource.hqcp")
public class HqcpDataSourceBoot extends HqcpDataSource implements InitializingBean, DisposableBean {

    private final DataSourceProperties basicProperties;
    private final SecurityService securityService;

    public HqcpDataSourceBoot(DataSourceProperties basicProperties) {
        this(basicProperties, null);
    }

    public HqcpDataSourceBoot(DataSourceProperties basicProperties, SecurityService securityService) {
        this.basicProperties = basicProperties;
        this.securityService = securityService;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        // 注入 SecurityService
        if (this.securityService != null) {
            super.setSecurityService(this.securityService);
        }

        // if not found prefix 'spring.datasource.hqcp' jdbc properties,
        // 'spring.datasource' prefix jdbc properties will be used.
        if (super.getUsername() == null) {
            super.setUsername(basicProperties.determineUsername());
        }
        if (super.getPassword() == null) {
            super.setPassword(basicProperties.determinePassword());
        }
        if (super.getDriverClassName() == null) {
            super.setDriverClassName(basicProperties.getDriverClassName());
        }
        if (super.getUrl() == null) {
            super.setUrl(basicProperties.determineUrl());
        }
    }

    @Override
    public void destroy() {
        // 关闭数据源，资源释放
        super.shutdown();
    }

}
