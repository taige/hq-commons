package io.hqwu.commons.cp;

public interface HqcpMBean {

    public String getPoolName();
    public void setPoolName(String poolName);
    
    public int getActiveConnectionsCount();
    public int getIdleConnectionsCount();
    
    public void shutdown();
}
