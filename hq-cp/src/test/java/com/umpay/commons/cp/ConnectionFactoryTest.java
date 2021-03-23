package com.umpay.commons.cp;

import com.jolbox.bonecp.MockJDBCDriver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.util.Locale;

/**
 * Created with IntelliJ IDEA for plat-arch-svn
 * User: taige
 * Date: 13-11-5
 * Time: 下午10:53
 */
public class ConnectionFactoryTest {

    MockJDBCDriver driver;

    @BeforeEach
    public void setUp() throws Exception {
        driver = new MockJDBCDriver();
    }

    @AfterEach
    public void tearDown() throws Exception {
        if (driver != null) {
            driver.disable();
            driver = null;
        }
    }

    @Test
    public void testGetConnection() throws Exception {
        Connection conn = ConnectionFactory.getConnection();
        conn.close();
        Connection conn2 = ConnectionFactory.getConnection(true);
        conn2.close();
        ConnectionFactory.shutdown();
    }

    @Test
    public void testGetConnection1() throws Exception {
        Locale.setDefault(Locale.CHINA);
        Connection conn = ConnectionFactory.getConnection("jdbc1");
        conn.close();
        Connection conn2 = ConnectionFactory.getConnection("jdbc1", true);
        conn2.close();
        ConnectionFactory.shutdown("jdbc1");
    }

    @Test
    public void testGetConnection2() throws Exception {
        Connection conn = ConnectionFactory.getConnection("jdbc2");
        conn.close();
        Connection conn2 = ConnectionFactory.getConnection("jdbc2", true);
        conn2.close();
        ConnectionFactory.shutdown("jdbc2");
    }

    @Test
    public void testReloadProperties() throws Exception {
        UmpayCP cp = ConnectionFactory.getUmpayCPInstance();
        cp.reloadProperties();
        cp.shutdown();

        UmpayCP cp2 = ConnectionFactory.getUmpayCPInstance("jdbc2");
        cp2.reloadProperties();
        cp2.shutdown();
    }

}
