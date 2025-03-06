package io.hqwu.commons.cp;


import io.hqwu.commons.util.Logger;
import io.hqwu.commons.util.LoggerFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;


@SpringBootTest(classes = DemoApplication.class)
@ActiveProfiles("test")
public class HqcpDataSourceTest {

    private static final Logger LOGGER = LoggerFactory.getLogger();

    @Autowired
    private HqcpDataSource dataSource;

    @Test
    public void testDataSourceExists() throws Exception {
        assertNotNull(dataSource);
    }

    @Test
    public void testDataSourcePropertiesOverridden() throws Exception {
        assertNotNull(dataSource);
        assertEquals("jdbc:mysql://test.com:3306/billpayment2", dataSource.getUrl());
        assertEquals("test", dataSource.getUsername());
        assertEquals("bp_password1", dataSource.getPassword());
        assertEquals("some-db-driver", dataSource.getDriverClassName());
        //      min-connections: 5
        assertEquals(5, dataSource.getMinConnections());
        //      max-connections: 20
        assertEquals(20, dataSource.getMaxConnections());
        //      idle-timeout-sec: 200
        assertEquals(200, dataSource.getIdleTimeoutSec());
        //      checkout-timeout-millisec: 20000
        assertEquals(20000, dataSource.getCheckoutTimeoutMillisec());
        //      query-timeout: 100
        assertEquals(100, dataSource.getQueryTimeout());
        //      login-timeout: 20
        assertEquals(20, dataSource.getLoginTimeout());
        //      check-statement: check it
        assertEquals("check it", dataSource.getCheckStatement());
        //      commit-on-close: true
        assertTrue(dataSource.isCommitOnClose());
        //      connection-info: a=A&b=B
        Properties properties = dataSource.getConnectionProperties();
        LOGGER.info(properties);
        assertEquals("A", properties.getProperty("a"));
        assertEquals("B", properties.getProperty("b"));
        assertEquals("test", properties.getProperty("user"));
        assertEquals("bp_password1", properties.getProperty("password"));
        //      info-sql-threshold: 20
        assertEquals(20, dataSource.getInfoSqlThreshold());
        //      warn-sql-threshold: 200
        assertEquals(200, dataSource.getWarnSqlThreshold());
        //      jmx-level: 1
        assertEquals(1, dataSource.getJmxLevel());
        //      lazy-init: true
        assertTrue(dataSource.isLazyInit());
        //      max-pre-statements: 20
        assertEquals(20, dataSource.getMaxPreStatements());
        //      max-statements: 200
        assertEquals(200, dataSource.getMaxStatements());
        //      print-sql: false
        assertFalse(dataSource.isPrintSql());
        //      transaction-mode: false
        assertFalse(dataSource.isTransactionMode());
        //      verbose: true
        assertTrue(dataSource.isVerbose());
        //      use-oracle-implicit-cache: false
        assertFalse(dataSource.isUseOracleImplicitCache());

    }
}
