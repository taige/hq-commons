package com.umpay.commons.cp;

import com.jolbox.bonecp.MockConstant;
import com.jolbox.bonecp.MockJDBCDriver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.sql.SQLException;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Created with IntelliJ IDEA for plat-arch-svn
 * User: taige
 * Date: 13-10-30
 * Time: 下午2:24
 */
public class UmpayCPConfigTest {

    UmpayCPConfig config;
    Properties prop;

    @BeforeEach
    public void before() throws Exception {
        config = new UmpayCPConfig();
        prop = new Properties();
    }

    @Test
    public void testBaseSetterGetter() throws Exception {
        config.setDriverClassName(MockJDBCDriver.class.getName());
//        config.setConnUrl(MockConstant.MOCK_URL);
        config.setUrl(MockConstant.MOCK_URL);
        config.setUsername("mockuser");
        config.setPassword("mockpasword");

        assertEquals(MockJDBCDriver.class.getName(), config.getDriverClassName());
        assertEquals(MockConstant.MOCK_URL, config.getUrl());
        assertEquals(MockConstant.MOCK_URL, config.getUrl());
        assertEquals("mockuser", config.getUsername());
//        assertEquals("mockpasword", config.getPassword());
        assertEquals("mockuser", config.getConnectionProperties().getProperty("user"));
        assertEquals("mockpasword", config.getConnectionProperties().getProperty("password"));
    }

    @Test
    public void testLoadDriver() throws Exception {
        config.setDriverClassName(MockJDBCDriver.class.getName());
//        config.setConnUrl(MockConstant.MOCK_URL);
        config.setUrl(MockConstant.MOCK_URL);

        UmpayCP cp = new UmpayCP(config);
        assertEquals(config, cp.getConfig());
        cp.shutdown();
    }

    @Test
    public void testLoadDriverFail() throws Exception {
        config.setDriverClassName("some.unknow.driver");
//        config.setConnUrl(MockConstant.MOCK_URL);
        config.setUrl(MockConstant.MOCK_URL);

        try {
            UmpayCP cp = new UmpayCP(config);
            fail("load some.unknow.driver driver ok?");
        } catch (SQLException e) {

        }
    }

    @Test
    public void testBasePropertiesSetter() throws Exception {
        prop.setProperty("jdbc.driver", MockJDBCDriver.class.getName());
        prop.setProperty("jdbc.url", MockConstant.MOCK_URL);
        prop.setProperty("jdbc.username", "mockuser");
        prop.setProperty("jdbc.password", "mockpassword");
        config.setProperties(prop);

        assertEquals(MockConstant.MOCK_URL, config.getUrl());
        assertEquals(MockConstant.MOCK_URL, config.getUrl());
        assertEquals(MockJDBCDriver.class.getName(), config.getDriverClassName());
        assertEquals("mockuser", config.getUsername());
//        assertEquals("mockpassword", config.getPassword());
        assertEquals("mockuser", config.getConnectionProperties().getProperty("user"));
        assertEquals("mockpassword", config.getConnectionProperties().getProperty("password"));
    }

    @Test
    public void testAutoSetCheckStatementByDB2Url() throws Exception {
        prop.setProperty("jdbc.url", "jdbc:db2:");
        config.setProperties(prop);
        assertEquals("values(current timestamp)", config.getCheckStatement());
    }

    @Test
    public void testAutoSetCheckStatementByOraUrl() throws Exception {
        prop.setProperty("jdbc.url", "jdbc:oracle:");
        config.setProperties(prop);
        assertEquals(true, config.isOracle());
        assertEquals("select systimestamp from dual", config.getCheckStatement());
        assertEquals("oracle.jdbc.driver.OracleDriver", config.getDriverClassName());
    }

    @Test
    public void testAutoSetCheckStatementByMyUrl() throws Exception {
        prop.setProperty("jdbc.url", "jdbc:mysql:");
        config.setProperties(prop);
        assertEquals(true, config.isMySQL());
        assertEquals("select now()", config.getCheckStatement());
        assertEquals("com.mysql.jdbc.Driver", config.getDriverClassName());
    }

    @Test
    public void testGetMinConnections() throws Exception {
        config.setMinConnections(100);
        assertEquals(100, config.getMinConnections());
    }

    @Test
    public void testGetMaxConnections() throws Exception {
        config.setMaxConnections(200);
        assertEquals(200, config.getMaxConnections());
    }

    @Test
    public void testIsVerbose() throws Exception {
        config.setVerbose(false);
        assertEquals(false, config.isVerbose());
        config.setVerbose(true);
        assertEquals(true, config.isVerbose());
    }

    @Test
    public void testIsPrintSQL() throws Exception {
        config.setPrintSql(false);
        assertEquals(false, config.isPrintSql());
        config.setPrintSql(true);
        assertEquals(true, config.isPrintSql());
    }

    @Test
    public void testIsLazyInit() throws Exception {
        config.setLazyInit(false);
        assertEquals(false, config.isLazyInit());
        config.setLazyInit(true);
        assertEquals(true, config.isLazyInit());
    }

    @Test
    public void testIsCommitOnClose() throws Exception {
        config.setCommitOnClose(false);
        assertEquals(false, config.isCommitOnClose());
        config.setCommitOnClose(true);
        assertEquals(true, config.isCommitOnClose());
    }

    @Test
    public void testGetIdleTimeoutSec() throws Exception {
        config.setIdleTimeoutSec(1234);
        assertEquals(1234, config.getIdleTimeoutSec());
        assertEquals(1234000, config.getIdleTimeoutMillisec());
    }

    @Test
    public void testGetCheckoutTimeoutMilliSec() throws Exception {
        config.setCheckoutTimeoutMillisec(1235);
        assertEquals(1235, config.getCheckoutTimeoutMillisec());
    }

    @Test
    public void testGetMaxStatements() throws Exception {
        config.setMaxStatements(120);
        assertEquals(120, config.getMaxStatements());
    }

    @Test
    public void testGetMaxPreStatements() throws Exception {
        config.setMaxPreStatements(139);
        assertEquals(139, config.getMaxPreStatements());
    }

    @Test
    public void testInfoSQLThreshold() throws Exception {
        config.setInfoSqlThreshold(139);
        assertEquals(139, config.getInfoSqlThreshold());
    }

    @Test
    public void testWarnSQLThreshold() throws Exception {
        config.setWarnSqlThreshold(1390);
        assertEquals(1390, config.getWarnSqlThreshold());
    }

    @Test
    public void testLoginTimeout() throws Exception {
        config.setLoginTimeout(139);
        assertEquals(139, config.getLoginTimeout());
    }

    @Test
    public void testQueryTimeout() throws Exception {
        config.setQueryTimeout(139);
        assertEquals(139, config.getQueryTimeout());
    }

    @Test
    public void testGetCheckStatement() throws Exception {
        config.setCheckStatement("test");
        assertEquals("test", config.getCheckStatement());
    }

    @Test
    public void testConnectionInfo() throws Exception {
        config.setConnectionInfo("ABC=abc&CC=cc");
        assertEquals("abc", config.getConnectionProperties().getProperty("ABC"));
        assertEquals("cc", config.getConnectionProperties().getProperty("CC"));
    }

    @Test
    public void testIsTransactionMode() throws Exception {
        config.setTransactionMode(true);
        assertEquals(true, config.isTransactionMode());
        config.setTransactionMode(false);
        assertEquals(false, config.isTransactionMode());
    }

    @Test
    public void testUseOracleImplicitPSCache() throws Exception {
        config.setUseOracleImplicitCache(true);
        assertEquals(true, config.isUseOracleImplicitCache());
        config.setUseOracleImplicitCache(false);
        assertEquals(false, config.isUseOracleImplicitCache());
    }

    @Test
    public void testGetJmxLevel() throws Exception {
        config.setJmxLevel(2);
        assertEquals(2, config.getJmxLevel());
    }
    @Test
    public void testSetPassword() throws Exception {
        checkPassword("zhrmghgsb1024bdj");
        checkPassword("zhrmghgsb1024bdjzhrmghgsb1024bdjzhrmghgsb1024bdjzhrmghgsb1024bdj");//64B base64解码后48B，是16的倍数
        checkPassword("1f/CYgg/AZ+O7FbBVqy+kIIxMOp3URjSFomDmp8JlWM=", "aaaabbbbccccdddd");
        checkPassword("1f/CYgg/AZ+O7FbBVqy+kOxapfXZnaXb4wTBKlUPh6k=", "aaaabbbbccccdddd");
        checkPassword("UoUpS4xy0mFS/SA3/rs+kNq4mUHAI+hsCMNpSqxHav8=", "helloIt'sASecret");
        checkPassword("g7CiE+BiDo9Y0Mj4LJRj/6B2n9VR7TJmFZt+5/APeeM=", "helloIt'sSecret");
        checkPassword("O9rR6SBlBsy/UYGrhsvk9w==", "helloIt'sSecret");
        checkPassword(new String(new byte[3]));
        checkPassword("             ");
        checkPassword("zhrmghgsb1024bdj%^&*(");
    }
    private void checkPassword(String password) {
        checkPassword(password, null);
    }
    private void checkPassword(String password, String except) {
        config.setPassword(password);
        if (except == null) {
            except = password;
        }
        assertEquals(except, config.getPassword());
    }
//    private static final String PATTERN_COMMONS_CHARS =  "[\\w\\S]+";
    private static final String PATTERN_COMMONS_CHARS =  "[\u0020-\u007E]+";

    @Test
    public void testRegex(){
        System.out.println("abc$%#weq*+{}:><?>;';".matches(PATTERN_COMMONS_CHARS));
        System.out.println(" ".matches(PATTERN_COMMONS_CHARS));
        System.out.println(new String(new byte[3]).matches(PATTERN_COMMONS_CHARS));

//        String ss = "abc$%#weq*123(&<>>?()*&^%$#@!\r\n";
//        ss = "a1(&<>>?()*&^%$#@!\r\n" + new String(new byte[1]);
//        for (Character c : ss.toCharArray()) {
//            System.out.printf("isMirrored(%s)=%b\n",c,Character.isMirrored(c));
//            System.out.printf("isDefined(%s)=%b\n",c,Character.isDefined(c));
//            System.out.printf("isLetterOrDigit(%s)=%b\n", c, Character.isLetterOrDigit(c));
//            System.out.printf("isSpaceChar(%s)=%b\n", c, Character.isSpaceChar(c));
//            System.out.println("++++++++++++++++++++++++++++++++++++++++++");
//        }
    }

    @Test
    public void testSetProperties() throws Exception {
        prop.setProperty("jdbc.verbose", "false");
        prop.setProperty("jdbc.print-sql", "false");
        prop.setProperty("jdbc.commit-on-close", "false");
        prop.setProperty("jdbc.transaction-mode", "false");
        prop.setProperty("jdbc.lazy-init", "false");

        prop.setProperty("jdbc.min-connections", "100");
        prop.setProperty("jdbc.max-connections", "200");
        prop.setProperty("jdbc.idle-timeout-sec", "1234");
        prop.setProperty("jdbc.checkout-timeout-millisec", "1235");
        prop.setProperty("jdbc.check-statement", "test");
        prop.setProperty("jdbc.max-statements", "120");
        prop.setProperty("jdbc.max-pre-statements", "139");
        prop.setProperty("jdbc.jmx-level", "2");
        config.setProperties(prop);

        assertEquals(false, config.isVerbose());
        assertEquals(false, config.isPrintSql());
        assertEquals(false, config.isCommitOnClose());
        assertEquals(false, config.isTransactionMode());
        assertEquals(false, config.isLazyInit());

        assertEquals(100, config.getMinConnections());
        assertEquals(200, config.getMaxConnections());
        assertEquals(1234, config.getIdleTimeoutSec());
        assertEquals(1234000, config.getIdleTimeoutMillisec());
        assertEquals(1235, config.getCheckoutTimeoutMillisec());
        assertEquals("test", config.getCheckStatement());
        assertEquals(120, config.getMaxStatements());
        assertEquals(139, config.getMaxPreStatements());
        assertEquals(2, config.getJmxLevel());

        prop.setProperty("jdbc.verbose", "true");
        prop.setProperty("jdbc.print-sql", "true");
        prop.setProperty("jdbc.commit-on-close", "true");
        prop.setProperty("jdbc.transaction-mode", "true");
        prop.setProperty("jdbc.lazy-init", "true");
        config.setProperties(prop);
        assertEquals(true, config.isVerbose());
        assertEquals(true, config.isPrintSql());
        assertEquals(true, config.isCommitOnClose());
        assertEquals(true, config.isTransactionMode());
        assertEquals(true, config.isLazyInit());

        prop.setProperty("jdbc.info-sql-threshold", "20");
        prop.setProperty("jdbc.warn-sql-threshold", "200");
        prop.setProperty("jdbc.use-oracle-implicit-cache", "false");
        prop.setProperty("jdbc.connection-info", "abc=ABC&cc=CC");
//        prop.setProperty("jdbc.login_timeout", "10");
        prop.setProperty("jdbc.query-timeout", "20");
        config.setProperties(prop);
        assertEquals(20, config.getInfoSqlThreshold());
        assertEquals(200, config.getWarnSqlThreshold());
        assertEquals(false, config.isUseOracleImplicitCache());
        assertEquals("ABC", config.getConnectionProperties().getProperty("abc"));
        assertEquals("CC", config.getConnectionProperties().getProperty("cc"));
//        assertEquals(10, config.getLoginTimeout());
        assertEquals(20, config.getQueryTimeout());
    }

    @Test
    public void testDefault() throws Exception {
        assertEquals(config.isVerbose(), false);
        assertEquals(config.isPrintSql(), true);
        assertEquals(config.isCommitOnClose(), false);
        assertEquals(config.isTransactionMode(), false);
        assertEquals(0, config.getMinConnections());
        assertEquals(10, config.getMaxConnections());
        assertEquals(300, config.getIdleTimeoutSec());
        assertEquals(300000, config.getIdleTimeoutMillisec());
        assertEquals(10000, config.getCheckoutTimeoutMillisec());
        assertNull(config.getCheckStatement());
        assertEquals(100, config.getMaxStatements());
        assertEquals(10, config.getMaxPreStatements());
        assertEquals(0, config.getJmxLevel());
    }

    @Test
    public void testEmptyProp() throws Exception {
        config.setProperties(prop);
        assertEquals(config.isVerbose(), false);
        assertEquals(config.isPrintSql(), true);
        assertEquals(config.isCommitOnClose(), false);
        assertEquals(config.isTransactionMode(), false);
        assertEquals(0, config.getMinConnections());
        assertEquals(10, config.getMaxConnections());
        assertEquals(300, config.getIdleTimeoutSec());
        assertEquals(300000, config.getIdleTimeoutMillisec());
        assertEquals(10000, config.getCheckoutTimeoutMillisec());
        assertNull(config.getCheckStatement());
        assertEquals(100, config.getMaxStatements());
        assertEquals(10, config.getMaxPreStatements());
        assertEquals(0, config.getJmxLevel());
    }

    @Test
    public void testSetPropertiesLocation() throws Exception {
        config.setPropertiesLocation(new ClassPathResource("jdbc.properties"));
        assertEquals("com.jolbox.bonecp.MockJDBCDriver", config.getDriverClassName());
        assertEquals("jdbc:mysql:url", config.getUrl());
        assertEquals("mockuser", config.getUsername());
//        assertEquals("mockpassword", config.getPassword());
        assertEquals(true, config.isVerbose());
        assertEquals(true, config.isPrintSql());
        assertEquals(true, config.isCommitOnClose());
        assertEquals(1, config.getMinConnections());
        assertEquals(2, config.getMaxConnections());
        assertEquals(20000, config.getIdleTimeoutMillisec());
        assertEquals(20, config.getIdleTimeoutSec());
        assertEquals(60000, config.getCheckoutTimeoutMillisec());
        assertEquals("test", config.getCheckStatement());
        assertEquals(10, config.getMaxStatements());
        assertEquals(5, config.getMaxPreStatements());
        assertEquals(2, config.getJmxLevel());
        assertEquals(false, config.isTransactionMode());
        assertEquals(false, config.isLazyInit());
    }

//    @Test
//    public void test_getSecurityService() throws SQLException {
//        new ClassPathXmlApplicationContext("dubbo-provider.xml");
//        ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext("spring-jdbc-remote-security.xml");
//        DataSource ds = ctx.getBean("dataSource", DataSource.class);
//        Connection conn = ds.getConnection();
//        conn.close();
//    }
//
//    @Test
//    public void test_getKeyManageService() throws SQLException {
//        ClassPathXmlApplicationContext dubbo_ctx = new ClassPathXmlApplicationContext("dubbo-provider.xml");
//        ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext("spring-jdbc-remote-security.xml");
//        DataSource ds = ctx.getBean("dataSource", DataSource.class);
//        Connection conn = ds.getConnection();
//        dubbo_ctx.close();
//        Connection conn1 = ds.getConnection();
//        conn1.close();
//        conn.close();
//    }
}
