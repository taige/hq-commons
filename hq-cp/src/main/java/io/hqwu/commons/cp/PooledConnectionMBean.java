package io.hqwu.commons.cp;


/**
 * 池化连接的管理接口 (MBean)。
 * <p>
 * 该接口用于通过 JMX 暴露 {@link PooledConnection} 的运行状态，支持监控连接的检出状态、
 * 占用线程、连接存活状态以及语句缓存（Statement Cache）的统计信息。
 * 同时提供手动触发连接校验和强制关闭的管理方法。
 * </p>
 *
 * @see PooledConnection
 */
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
