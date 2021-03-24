package io.hqwu.commons.cp;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ConnectionFactory {
    private static Map<String, Hqcp> poolCache = new HashMap<String, Hqcp>();
   
    private static ReadWriteLock rwl = new ReentrantReadWriteLock();
    
    public static Hqcp getUmpayCPInstance() throws SQLException {
        return getUmpayCPInstance("jdbc");
    }
    
    public static Hqcp getUmpayCPInstance(String jdbc) throws SQLException {
        Hqcp cp = poolCache.get(jdbc);
        if (cp == null) {
            cp = maybeInit(jdbc);
        }
        return cp;
    }
    
    private static Hqcp maybeInit(String jdbc) throws SQLException {
        rwl.readLock().lock();
        Hqcp cp = poolCache.get(jdbc);
        try {

            if (cp == null) { // this.pool is protected in getConnection
                rwl.readLock().unlock();
                rwl.writeLock().lock();
                cp = poolCache.get(jdbc);
                try {
                    if (cp == null) { // read might have passed, write
                        // might not
                        cp = new Hqcp(jdbc);
                        poolCache.put(jdbc, cp);
                    }
                } finally {
                    rwl.readLock().lock();
                    rwl.writeLock().unlock();
                }
            }
        } finally {
            rwl.readLock().unlock();
        }


        return cp;
    }
    
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
        Hqcp cp = getUmpayCPInstance(jdbc);
        return cp.getConnection();
    }
    
    public static Connection getConnection(boolean autoCommit) throws SQLException {
        return getConnection("jdbc", autoCommit);
    }
    
    public static Connection getConnection(String jdbc, boolean autoCommit) throws SQLException {
        Hqcp cp = getUmpayCPInstance(jdbc);
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

}
