package com.umpay.commons.cp;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ConnectionFactory {
    private static Map<String, UmpayCP> poolCache = new HashMap<String, UmpayCP>();
   
    private static ReadWriteLock rwl = new ReentrantReadWriteLock();
    
    public static UmpayCP getUmpayCPInstance() throws SQLException {
        return getUmpayCPInstance("jdbc");
    }
    
    public static UmpayCP getUmpayCPInstance(String jdbc) throws SQLException {
        UmpayCP cp = poolCache.get(jdbc);
        if (cp == null) {
            cp = maybeInit(jdbc);
        }
        return cp;
    }
    
    private static UmpayCP maybeInit(String jdbc) throws SQLException {
        rwl.readLock().lock();
        UmpayCP cp = poolCache.get(jdbc);
        try {

            if (cp == null) { // this.pool is protected in getConnection
                rwl.readLock().unlock();
                rwl.writeLock().lock();
                cp = poolCache.get(jdbc);
                try {
                    if (cp == null) { // read might have passed, write
                        // might not
                        cp = new UmpayCP(jdbc);
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
        UmpayCP cp = getUmpayCPInstance(jdbc);
        return cp.getConnection();
    }
    
    public static Connection getConnection(boolean autoCommit) throws SQLException {
        return getConnection("jdbc", autoCommit);
    }
    
    public static Connection getConnection(String jdbc, boolean autoCommit) throws SQLException {
        UmpayCP cp = getUmpayCPInstance(jdbc);
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
        UmpayCP cp = remove(jdbc);
        if (cp != null) {
            cp.shutdown();
        }
    }
    
    static UmpayCP remove(String jdbc) {
        return poolCache.remove(jdbc);
    }

}
