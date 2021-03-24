package com.umpay.commons.cp;


import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;


@SpringBootTest(classes = DemoApplication.class)
@ActiveProfiles("test2")
public class HqCpDataSourceTest2 {

    @Autowired
    private UmpayCPDataSource dataSource;

    @Test
    public void testDataSourcePropertiesOverridden() throws Exception {
        assertNotNull(dataSource);
        assertEquals("jdbc:mysql://test.com:3306/billpayment", dataSource.getUrl());
        assertEquals("bp_user", dataSource.getUsername());
        assertEquals("bp_password", dataSource.getPassword());
        assertEquals("some-db-driver-class", dataSource.getDriverClassName());
    }
}
