package io.hqwu.commons.cp;

import com.umpay.commons.SecurityService;
import com.umpay.commons.SecurityServiceLocalImpl;
import com.umpay.commons.util.Logger;
import com.umpay.commons.util.StringUtil;
import org.apache.commons.codec.binary.Base64;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.DriverManager;
import java.util.Map;
import java.util.Properties;

public class HqcpConfig implements HqcpConfigMBean, ApplicationContextAware {
    private static final Logger LOGGER = new Logger();
    /**
     * 连接URL
     */
    private String url;
    /**
     * jdbc驱动类
     */
    private String driverClassName;

    /**
     * 数据库用户名
     */
    private String username;
    /**
     * 数据库用户口令
     */
    private String password;

    /**
     * 池中最小连接数
     */
    private int minConnections = 0;
    /**
     * 池中最大连接数
     */
    private int maxConnections = 10;
    /**
     * 池中最大语句数
     */
    private int maxStatements = 100;
    /**
     * 池中最大语句数
     */
    private int maxPreStatements = 10;
    /**
     * 连接最大空闲时间(milli sec)(空闲超过该时间的连接将被检测或回收)
     */
    private long idleTimeoutMillisec = 300 * 1000;
    /**
     * 等待空闲连接时的超时时间(ms)
     */
    private long checkoutTimeoutMillisec = 10000;

    /**
     * 关闭连接时自动提交事务
     */
    private boolean commitOnClose = false;

    /**
     * 记录除SQL语句及执行时间外的其他信息
     */
    private boolean verbose = false;

    /**
     * 记录SQL语句及执行时间 //add by wuhq 2011.09.02
     */
    private boolean printSql = true;

    /**
     * 检测连接是否可用的查询语句
     */
    private String checkStatement;

    /**
     * 0 - no jmx
     * 1 - manage ConnectionFactory instance
     * 2 - manage PooledConnection instance
     */
    private int jmxLevel = 0;

    /**
     * 默认获取的连接的事务模式（true-事务模式，即autocommit=false）
     */
    private boolean transactionMode = false;

    /**
     * lazy init pool
     * true: init min connections in Monitor thread, else do it in new UmpayCP/getConnection() thread
     */
    private boolean lazyInit = false;

    /**
     * printSQL == true时，打印INFO级别的SQL的耗时阈值(ms)
     */
    private long infoSqlThreshold = 10;

    /**
     * printSQL == true时，打印WARN级别的SQL的耗时阈值(ms)
     */
    private long warnSqlThreshold = 100;

    /**
     * Indicates if this is for Oracle.
     */
    private boolean isOracle = false;

    /**
     * Indicates if this is MySQL cp
     */
    private boolean isMySQL = false;

    /**
     * Indicates if this is DB2 cp
     */
    private boolean isDB2 = false;

    /**
     * Indicates if oracle implicit preparedstatement cache needed.
     */
    private boolean useOracleImplicitCache = true;

    /**
     * connection properties on DriverManager.getConnection(url,info)<br/>
     * 用 & 分割属性
     */
    private Properties connectionProperties = new Properties();

    /**
     * query timeout (seconds)
     */
    private int queryTimeout = 60;

    private String passwordKey = null;

    private SecurityService securityService;

    private ApplicationContext applicationContext;

    /*
     * default login timeout 10 seconds
     */
    static {
        DriverManager.setLoginTimeout(10);
    }

    public int getLoginTimeout() {
        return DriverManager.getLoginTimeout();
    }

    public void setLoginTimeout(int loginTimeout) {
        DriverManager.setLoginTimeout(loginTimeout);
    }

    public int getQueryTimeout() {
        return queryTimeout;
    }

    public void setQueryTimeout(int queryTimeout) {
        this.queryTimeout = queryTimeout;
    }

    public Properties getConnectionProperties() {
        if (! connectionProperties.containsKey("password")) {
            String password = this.getPassword();
            if (password != null) {
                this.connectionProperties.setProperty("password", password);
            }
        }
        return connectionProperties;
    }

    public void setConnectionInfo(String connectionInfo) {
        if (connectionInfo == null || connectionInfo.trim().length() == 0) {
            return;
        }

        String[] entries = connectionInfo.split("&");
        for (int i = 0; i < entries.length; i++) {
            String entry = entries[i];
            if (entry.length() > 0) {
                int index = entry.indexOf('=');
                if (index > 0) {
                    String name = entry.substring(0, index);
                    String value = entry.substring(index + 1);
                    connectionProperties.setProperty(name, value);
                } else {
                    // no value is empty string which is how java.util.Properties works
                    connectionProperties.setProperty(entry, "");
                }
            }
        }
    }

    private void _setUrl(String url) {
        this.url = url;
        if (this.url != null && this.url.trim().length() != 0) {
            String[] buf = this.url.split(":");
            if (buf.length < 2) {
                return;
            }
            String dbf = buf[1];
            if (dbf.compareToIgnoreCase("oracle") == 0) {
                isOracle = true;
            } else if (dbf.compareToIgnoreCase("mysql") == 0) {
                isMySQL = true;
            } else if (dbf.compareToIgnoreCase("db2") == 0) {
                isDB2 = true;
            }
            if (this.checkStatement == null || this.checkStatement.trim().length() == 0) {
                //if checkStatement NOT be set, auto-set by url
                if (isDB2) {
                    checkStatement = "values(current timestamp)";
                } else if (isOracle) {
                    checkStatement = "select systimestamp from dual";
                } else if (isMySQL) {
                    checkStatement = "select now()";
                }
            }
            if (this.driverClassName == null || this.driverClassName.trim().length() == 0) {
                //if driver NOT be set, auto-set by url
                if (isOracle) {
                    driverClassName = "oracle.jdbc.driver.OracleDriver";
                } else if (isMySQL) {
                    driverClassName = "com.mysql.cj.jdbc.Driver";
                }
            }
        }
    }

    /*
     * 2012-11-12 zhangyao 支持url参数的注入，保持一其它数据库连接池一致
     */
    public String getUrl() {
        return this.url;
    }

    public void setUrl(String url) {
        _setUrl(url);
    }

    public long getWarnSqlThreshold() {
        return warnSqlThreshold;
    }

    public void setWarnSqlThreshold(long warnSqlThreshold) {
        this.warnSqlThreshold = warnSqlThreshold;
    }

    public long getInfoSqlThreshold() {
        return infoSqlThreshold;
    }

    public void setInfoSqlThreshold(long infoSqlThreshold) {
        this.infoSqlThreshold = infoSqlThreshold;
    }

    public String getDriverClassName() {
        return driverClassName;
    }

    public void setDriverClassName(String driver) {
        this.driverClassName = driver;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
        if (username != null && username.trim().length() > 0) {
            this.connectionProperties.setProperty("user", username);
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    String getPassword() {
        if (StringUtil.isEmpty(this.password)) {
            return null;
        }
        final String password = this.password;
        final boolean isBase64 = Base64.isBase64(password);
        if (isBase64) { // 如果是Base64的尝试解码
            try {
                byte[] base64DecodedPwd = Base64.decodeBase64(password);
                boolean isBlock16 = (base64DecodedPwd.length % 16 == 0);  //对称加密数据一定是16的倍数(AES128)
                if (isBlock16 && !new String(base64DecodedPwd, StandardCharsets.UTF_8).matches(PATTERN_COMMONS_CHARS)) { //不可见字符，是加密密码
                    byte[] decodePwd = this.getSecurityService().decryptByAES(base64DecodedPwd, this.getPasswordKey(), "CBC");
                    String plainPwd = StringUtil.trimToEmpty(new String(decodePwd, StandardCharsets.UTF_8));//明文密码
                    if (plainPwd.matches(PATTERN_COMMONS_CHARS)) { // 解密结果是可见字符
                        this.password = plainPwd;
                    }
                }
            } catch (Exception e) {
                LOGGER.debug(e);
                LOGGER.info("使用明文密码: ", e.getMessage());
            }
        }
        return this.password;
    }

    private static final String PATTERN_COMMONS_CHARS =  "[\u0020-\u007E]+";
    public void setPassword(String password) {
        if (StringUtil.isEmpty(password)) {
            return;
        }
        this.password = password;
    }

    public int getMinConnections() {
        return minConnections;
    }

    public void setMinConnections(int minConnections) {
        this.minConnections = minConnections;
    }

    public int getMaxConnections() {
        return maxConnections;
    }

    public void setMaxConnections(int maxConnections) {
        this.maxConnections = maxConnections;
    }

    public boolean isVerbose() {
        return verbose;
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    public boolean isPrintSql() {
        return printSql;
    }

    public void setPrintSql(boolean printSql) {
        this.printSql = printSql;
    }

    public boolean isCommitOnClose() {
        return commitOnClose;
    }

    public void setCommitOnClose(boolean commitOnClose) {
        this.commitOnClose = commitOnClose;
    }

    public long getIdleTimeoutSec() {
        return idleTimeoutMillisec / 1000;
    }

    public long getIdleTimeoutMillisec() {
        return idleTimeoutMillisec;
    }

    public void setIdleTimeoutSec(long idleTimeoutSec) {
        this.idleTimeoutMillisec = idleTimeoutSec * 1000;
    }

    public long getCheckoutTimeoutMillisec() {
        return checkoutTimeoutMillisec;
    }

    public void setCheckoutTimeoutMillisec(long checkoutTimeoutMilliSec) {
        this.checkoutTimeoutMillisec = checkoutTimeoutMilliSec;
    }

    public int getMaxStatements() {
        return maxStatements;
    }

    public void setMaxStatements(int maxStatements) {
        this.maxStatements = maxStatements;
    }

    public int getMaxPreStatements() {
        return maxPreStatements;
    }

    public void setMaxPreStatements(int maxPreStatements) {
        this.maxPreStatements = maxPreStatements;
    }

    public String getCheckStatement() {
        return checkStatement;
    }

    public void setCheckStatement(String checkStatement) {
        this.checkStatement = checkStatement;
    }

    public boolean isTransactionMode() {
        return transactionMode;
    }

    public void setTransactionMode(boolean transactionMode) {
        this.transactionMode = transactionMode;
    }

    public int getJmxLevel() {
        return jmxLevel;
    }

    public void setJmxLevel(int jmxLevel) {
        this.jmxLevel = jmxLevel;
    }

    public boolean isLazyInit() {
        return lazyInit;
    }

    public void setLazyInit(boolean lazyInit) {
        this.lazyInit = lazyInit;
    }

    public boolean isMySQL() {
        return isMySQL;
    }

    public boolean isOracle() {
        return isOracle;
    }

    public boolean isDB2() {
        return isDB2;
    }

    public boolean isUseOracleImplicitCache() {
        return useOracleImplicitCache;
    }

    public void setUseOracleImplicitCache(boolean useOracleImplicitPSCache) {
        this.useOracleImplicitCache = useOracleImplicitPSCache;
    }

    public void setPasswordKey(String passwordAesKey) {
        if (passwordAesKey != null && passwordAesKey.length() > 0) {
            this.passwordKey = passwordAesKey;
        }
    }

    String getPasswordKey() {
        if (this.passwordKey != null) {
            return this.passwordKey;
        }
        String masteryKey = System.getProperty("JDBC_SECRET_KEY");
        if (masteryKey == null) {
            masteryKey = System.getenv("JDBC_SECRET_KEY");
        }
        if (masteryKey == null) {
            masteryKey = "eyGDTxVGdmJW7YMYclvWzPMGct4j0syHAdWEB3luYCk=";
        }
        this.passwordKey = masteryKey;
        return this.passwordKey;
    }

    public void setSecurityService(SecurityService securityService) {
        this.securityService = securityService;
    }

    SecurityService getSecurityService() {
        if (this.securityService == null) {
            if (this.applicationContext != null) {
                try {
                    Map<String, SecurityService> map = this.applicationContext.getBeansOfType(SecurityService.class);
                    for (Map.Entry<String, SecurityService> entry: map.entrySet()) {
                        LOGGER.debug("getBean(SecurityService.class): [", entry.getKey(), "]=", entry.getValue().getClass().getName());
                        if (entry.getValue() instanceof SecurityServiceLocalImpl) {
                            this.securityService = entry.getValue();
                        } else {
                            //尽量选远程实现
                            this.securityService = entry.getValue();
                            break;
                        }
                    }
                } catch (BeansException e) {
                    LOGGER.warn("getBean(SecurityService.class) error: ", e);
                    this.securityService = new SecurityServiceLocalImpl();
                }
            }
            if (this.securityService == null) {
                this.securityService = new SecurityServiceLocalImpl();
            }
        }
        return this.securityService;
    }
    
    public HqcpConfig() {
//        this.poolName = poolName;
    }

    /**
     * Set the location of the jdbc properties file.
     */
    public void setPropertiesLocation(org.springframework.core.io.Resource configLocation) throws IOException {
        InputStream is = configLocation.getInputStream();
        Properties prop = new Properties();
        prop.load(is);
        this.setProperties(prop);
        is.close();
    }

    public void setProperties(Properties prop) {
        setUrl(prop.getProperty("jdbc.url"));
        setUsername(prop.getProperty("jdbc.username", null));
        setPassword(prop.getProperty("jdbc.password", null));
        driverClassName = prop.getProperty("jdbc.driver", driverClassName);
        driverClassName = prop.getProperty("jdbc.driver-class-name", driverClassName);
        verbose = Boolean.parseBoolean(prop.getProperty("jdbc.verbose", String.valueOf(verbose)));
        printSql = Boolean.parseBoolean(prop.getProperty("jdbc.print-sql", String.valueOf(printSql)));
        commitOnClose = Boolean.parseBoolean(prop.getProperty("jdbc.commit-on-close", String.valueOf(commitOnClose)));
        minConnections = Integer.parseInt(prop.getProperty("jdbc.min-connections", String.valueOf(minConnections)));
        maxConnections = Integer.parseInt(prop.getProperty("jdbc.max-connections", String.valueOf(maxConnections)));
        idleTimeoutMillisec = Long.parseLong(prop.getProperty("jdbc.idle-timeout-sec", String.valueOf(idleTimeoutMillisec / 1000))) * 1000;
        checkoutTimeoutMillisec = Long.parseLong(prop.getProperty("jdbc.checkout-timeout-millisec", String.valueOf(checkoutTimeoutMillisec)));
        checkStatement = prop.getProperty("jdbc.check-statement", checkStatement);
        maxStatements = Integer.parseInt(prop.getProperty("jdbc.max-statements", String.valueOf(maxStatements)));
        maxPreStatements = Integer.parseInt(prop.getProperty("jdbc.max-pre-statements", String.valueOf(maxPreStatements)));
        jmxLevel = Integer.parseInt(prop.getProperty("jdbc.jmx-level", String.valueOf(jmxLevel)));
        transactionMode = Boolean.parseBoolean(prop.getProperty("jdbc.transaction-mode", String.valueOf(transactionMode)));
        lazyInit = Boolean.parseBoolean(prop.getProperty("jdbc.lazy-init", String.valueOf(lazyInit)));
        infoSqlThreshold = Long.parseLong(prop.getProperty("jdbc.info-sql-threshold", String.valueOf(infoSqlThreshold)));
        warnSqlThreshold = Long.parseLong(prop.getProperty("jdbc.warn-sql-threshold", String.valueOf(warnSqlThreshold)));
        useOracleImplicitCache = Boolean.parseBoolean(prop.getProperty("jdbc.use-oracle-implicit-cache", String.valueOf(useOracleImplicitCache)));
//        setLoginTimeout(Integer.parseInt(prop.getProperty("jdbc.login-timeout", String.valueOf(0))));
        setQueryTimeout(Integer.parseInt(prop.getProperty("jdbc.query-timeout", String.valueOf(queryTimeout))));
        setConnectionInfo(prop.getProperty("jdbc.connection-info"));
        setPasswordKey(prop.getProperty("jdbc.password-key", null));
        //set isOracle in setConnUrl method.
//        if (connUrl != null) {
//            isOracle = JdbcUtil.checkOracle(connUrl);
//        }
    }

    static final String[] PROPERTIES = new String[]{
            "jdbc.driver-class-name", "jdbc.url", "jdbc.username", "jdbc.password", "jdbc.check_statement",
            "jdbc.verbose", "jdbc.print-sql", "jdbc.commit-on-close", "jdbc.transaction-mode", "jdbc.lazy-init",
            "jdbc.min-connections", "jdbc.max-connections", "jdbc.max-statements", "jdbc.max-pre-statements",
            "jdbc.idle-timeout-sec", "jdbc.checkout-timeout-millisec",
            "jdbc.jmx-level", "jdbc.info-sql-threshold", "jdbc.warn-sql-threshold", "jdbc.use-oracle-implicit-cache", "jdbc.connection-info",
            "jdbc.query-timeout", "jdbc.password-key"
    };
    
}
