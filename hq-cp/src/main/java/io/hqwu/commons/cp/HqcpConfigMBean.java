package io.hqwu.commons.cp;

/**
 * Hqcp 数据库连接池配置的 MBean 接口。
 * <p>
 * 该接口定义了连接池的核心配置属性，包括数据库连接信息、池容量控制、超时设置、
 * SQL 日志与脱敏策略等。通过此接口可实现对 {@link HqcpConfig} 实例的 JMX 动态管理。
 * </p>
 *
 * @see HqcpConfig
 * @author taige
 */
public interface HqcpConfigMBean {

    public String getUrl();
    public void setUrl(String url);
    
    public String getDriverClassName();
    public void setDriverClassName(String driver);
    
    public String getUsername();
    public void setUsername(String username);
    
//    public String getPassword();
    public void setPassword(String password);
    
    public int getMinConnections();
    public void setMinConnections(int min);
    
    public int getMaxConnections();
    public void setMaxConnections(int max);
    
    public boolean isVerbose();
    public void setVerbose(boolean vb);
    
    public boolean isPrintSql();
    public void setPrintSql(boolean ps);

    public boolean isMaskSql();
    public void setMaskSql(boolean maskSql);

    public boolean isCommitOnClose();
    public void setCommitOnClose(boolean cc);
    
    public long getIdleTimeoutSec();
    public void setIdleTimeoutSec(long idle);
    
    public long getCheckoutTimeoutMillisec();
    public void setCheckoutTimeoutMillisec(long checkout);

    public int getMaxStatements();
    public void setMaxStatements(int num);

    public int getMaxPreStatements();
    public void setMaxPreStatements(int num);

    public String getCheckStatement();
    public void setCheckStatement(String stmt);
    
    public boolean isTransactionMode();
    public void setTransactionMode(boolean mode);
    
    public int getJmxLevel();
    public void setJmxLevel(int jmx);

    public boolean isLazyInit();
    public void setLazyInit(boolean lazyInit);

    long getLifetimeSec();
    void setLifetimeSec(long lifetimeSec);

    void reloadProperties();
}