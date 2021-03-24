package com.umpay.commons.cp;

public interface UmpayCPConfigMBean {

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

}
