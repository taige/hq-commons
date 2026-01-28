package io.hqwu.commons.cp;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.RefAddr;
import javax.naming.Reference;
import javax.naming.spi.ObjectFactory;
import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * HQCP (HQ Connection Pool) 数据源实现类。
 *
 * <p>该类扩展了 {@link HqcpConfig} 并实现了 {@link DataSource} 接口，作为连接池的核心入口。
 * 它负责管理内部 {@link Hqcp} 实例的生命周期，支持延迟初始化以及通过 {@link ObjectFactory} 进行 JNDI 资源查找。</p>
 *
 * @see Hqcp
 * @see HqcpConfig
 * @see javax.sql.DataSource
 */
public class HqcpDataSource extends HqcpConfig implements DataSource, ObjectFactory {

    private boolean initOnStartup = false;

    private volatile Hqcp pool = null;
    private PrintWriter logWriter = null;

    public PrintWriter getLogWriter() throws SQLException {
        return logWriter;
    }

    public void setLogWriter(PrintWriter out) throws SQLException {
        logWriter = out;
    }

    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        throw new SQLFeatureNotSupportedException("getParentLogger is unsupported");
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        if (iface == null) {
            throw new SQLException("Interface argument cannot be null");
        }
        if (iface.isInstance(this)) {
            return iface.cast(this);
        }

        if (iface.isInstance(this.pool)) {
            return iface.cast(this.pool);
        }

        throw new SQLException("Cannot unwrap to " + iface.getName());
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        if (iface == null) {
            return false;
        }
        return iface.isInstance(this) || (iface.isInstance(this.pool));
    }

    public Object getObjectInstance(Object object, Name name, Context nameCtx, Hashtable<?, ?> environment) throws Exception {

        Reference ref = (Reference) object;
        Enumeration<RefAddr> addrs = ref.getAll();
        Properties props = new Properties();
        while (addrs.hasMoreElements()) {
            RefAddr addr = addrs.nextElement();
            if (addr.getType().equals("driverClassName") || addr.getType().equals("driver")) {
                //TODO test the logical is correct?
                Class.forName((String) addr.getContent());
            } else {
                props.put(addr.getType(), addr.getContent());
            }
        }
        HqcpDataSource ds = new HqcpDataSource();
        ds.setProperties(props);
        return ds;
    }

    public Connection getConnection() throws SQLException {
        if (this.pool == null) {
            maybeInit();
        }
        return this.pool.getConnection();
    }

    public Connection getConnection(String username, String password) throws SQLException {
        throw new UnsupportedOperationException("getConnection(username, password) is unsupported.");
    }

    public void init() throws SQLException {
        if (this.initOnStartup && this.pool == null) {
            maybeInit();
        }
    }

    public void shutdown() {
        Hqcp currentPool = this.pool;
        if (currentPool != null) {
            synchronized (this) {
                if (this.pool != null) {
                    this.pool.shutdown();
                    this.pool = null;
                }
            }
        }
    }

    public void setInitOnStartup(boolean initOnStartup) {
        this.initOnStartup = initOnStartup;
    }

    private void maybeInit() throws SQLException {
        if (this.pool == null) {
            synchronized (this) {
                if (this.pool == null) {
                    this.pool = new Hqcp(this);
                }
            }
        }
    }

}
