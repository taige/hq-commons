package io.hqwu.commons.cp;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 早期非spring管理模式下的连接池工厂实现，已不推荐使用
 *
 * @deprecated
 */
@Deprecated
public class ConnectionFactory {
    private static ConcurrentMap<String, Hqcp> poolCache = new ConcurrentHashMap<>();

    public static Hqcp getHqcpInstance() throws SQLException {
        return getHqcpInstance("jdbc");
    }

    public static Hqcp getHqcpInstance(String jdbc) throws SQLException {
        try {
            return poolCache.computeIfAbsent(jdbc, j ->
                    {
                        try {
                            return newHqcpInstance(j);
                        } catch (SQLException e) {
                            throw new ConnectionFactoryInitialException(e);
                        }
                    }
            );
        } catch (ConnectionFactoryInitialException e) {
            throw e.sqlException;
        }
    }

    static Hqcp newHqcpInstance(String jdbc) throws SQLException {
        return new Hqcp(jdbc);
    }

    /**
     * 获取默认的连接池
     * @return
     * @throws SQLException
     */
    public static Connection getConnection() throws SQLException {
        return getConnection("jdbc" /*, false*/);
    }
    
    @Deprecated
    public static Connection getConnection(Connection conn) throws SQLException {
        return getConnection(conn, false);
    }
    
    @Deprecated
    public static Connection getConnection(Connection conn, boolean autoCommit) throws SQLException {
        conn.setAutoCommit(autoCommit);
        return conn;
    }
    
    public static Connection getConnection(String jdbc) throws SQLException {
        Hqcp cp = getHqcpInstance(jdbc);
        return cp.getConnection();
    }
    
    public static Connection getConnection(boolean autoCommit) throws SQLException {
        return getConnection("jdbc", autoCommit);
    }
    
    public static Connection getConnection(String jdbc, boolean autoCommit) throws SQLException {
        Hqcp cp = getHqcpInstance(jdbc);
        return cp.getConnection(autoCommit);
    }

    /**
     * 关闭默认的连接池
     */
    public static void shutdown() {
        shutdown("jdbc");
    }

    /**
     * 关闭指定的连接池
     * @param jdbc
     */
    public static synchronized void shutdown(String jdbc) {
        Hqcp cp = remove(jdbc);
        if (cp != null) {
            cp.shutdown();
        }
    }
    
    static Hqcp remove(String jdbc) {
        return poolCache.remove(jdbc);
    }

    private static class ConnectionFactoryInitialException extends RuntimeException {
        final SQLException sqlException;
        public ConnectionFactoryInitialException(SQLException e) {
            sqlException = e;
        }
    }
}
