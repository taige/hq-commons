package com.umpay.commons.cp;

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
public class HqCpDataSourceBoot extends UmpayCPDataSource implements InitializingBean {

    private final DataSourceProperties basicProperties;

    public HqCpDataSourceBoot(DataSourceProperties basicProperties) {
        this.basicProperties = basicProperties;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
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

}
