package com.umpay.commons.cp;

import com.umpay.commons.util.Logger;
import com.umpay.commons.util.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * Created with IntelliJ IDEA for hq-cp-boot-starter
 *
 * @author taige (Wu, Hongqiang)
 * Date: 2021-02-21
 * Time: 8:13 p.m.
 */
@Configuration
@ConditionalOnClass(UmpayCPDataSource.class)
@AutoConfigureBefore(DataSourceAutoConfiguration.class)
public class HqCpDataSourceAutoConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger();

    @Bean
    @ConditionalOnMissingBean
    public DataSource dataSource(DataSourceProperties basicProperties) {
        LOGGER.debug("init HqCpDataSource");
        return new HqCpDataSourceBoot(basicProperties);
    }

}
