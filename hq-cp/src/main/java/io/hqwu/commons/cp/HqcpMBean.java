package io.hqwu.commons.cp;

/**
 * HqcpMBean 接口定义了 HQ 连接池的管理和监控规范。
 * <p>
 * 该接口通过 JMX (Java Management Extensions) 暴露连接池的关键运行指标（如活跃与空闲连接数）及管理操作，
 * 旨在支持外部工具对连接池状态的实时监控与生命周期管理。
 * </p>
 *
 * @see HqcpDataSource
 */
public interface HqcpMBean {

    public String getPoolName();
    public void setPoolName(String poolName);
    
    public int getActiveConnectionsCount();
    public int getIdleConnectionsCount();
    
    public void shutdown();
}
