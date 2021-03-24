package io.hqwu.commons.cp;

import java.sql.SQLException;

public interface HqcpMBean {

    public String getPoolName();
    public void setPoolName(String poolName);
    
    public int getActiveConnectionsCount();
    public int getIdleConnectionsCount();
    
    public void reloadProperties() throws SQLException;
    public void shutdown();
}
