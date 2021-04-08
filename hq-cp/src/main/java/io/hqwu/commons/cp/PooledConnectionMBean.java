package io.hqwu.commons.cp;


public interface PooledConnectionMBean {

    public String getConnectionName();
    public boolean isCheckOut();
    public boolean isBusying();
    public boolean isClosed();
    
    public String getCheckOutThreadName();
    
    public int getCachedStatementsCount();
    public int getCachedPreStatementsCount();
    public String[] getCachedPreStatementsSQLs();

    public void doCheck() throws Exception;
    public void close();
    
}
