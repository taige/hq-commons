package io.hqwu.commons.cp.util;

import io.hqwu.commons.util.Logger;
import oracle.jdbc.OraclePreparedStatement;

import java.sql.SQLException;
import java.sql.Statement;

/**
 * Oracle 数据库工具类。
 * <p>
 * 本类提供与 Oracle JDBC 驱动（适配 ojdbc17+ 及 Oracle 12c+）集成的常用常量和操作方法。
 * 主要功能包括：
 * <ul>
 *   <li>管理 Oracle 特有的连接属性，如读取超时（ReadTimeout）和连接超时（ConnectTimeout）。</li>
 *   <li>提供对 {@link oracle.jdbc.OraclePreparedStatement} 隐式缓存（Implicit Cache）的操作支持。</li>
 *   <li>适配新版驱动的自动内存管理机制，简化 Statement 的生命周期管理。</li>
 * </ul>
 * 相关类引用：{@link java.sql.Statement}, {@link oracle.jdbc.OraclePreparedStatement}。 <br/>
 * 2025-03-04: 升级到 ojdbc17+ (12c+) 后，隐式缓存管理已由驱动自动处理
 *
 * @author jianbin
 * @since 2014-02-14
 */
public class OracleUtil {
    private static final Logger log = new Logger();

    // 2025-03-04: 升级到 ojdbc17+ 后，不再需要针对 Oracle 10 的特殊缓存配置。
    public final static String ORACLE_FREECACHE_PROPERTY_NAME = "oracle.jdbc.FreeMemoryOnEnterImplicitCache";
    public final static String ORACLE_FREECACHE_PROPERTY_VALUE_TRUE = "true";

    public final static String SOCKET_TIMEOUT = "oracle.jdbc.ReadTimeout";
    public final static String SOCKET_TIMEOUT_LOW_VER = "oracle.net.READ_TIMEOUT";

    public final static String CONNECT_TIMEOUT = "oracle.net.CONNECT_TIMEOUT";

    /**
     * Will clear the PS caches for this statement in driver.
     * Not working on version 10 driver. Working on v11.
     */
    public static void enterImplicitCache(Statement statement) {
        try {
            OraclePreparedStatement oraclePreparedStatement = unwrapInternal(statement);

            if (oraclePreparedStatement != null) {
                // oraclePreparedStatement.enterImplicitCache();
                // 2025-03-04: 升级到 ojdbc17+ (12c+) 后，隐式缓存管理已由驱动自动处理。
                // 显式的 enterImplicitCache/exitImplicitCache 方法在公共 API 中已被移除或不再推荐使用。
                // 强行调用内部 API 会导致兼容性问题，故在此注释掉。
                // 现在的最佳实践是依赖驱动自身的内存管理，或者仅使用标准的 close()。
            }
        } catch(SQLException e) {
            log.warn(e);
        }
    }

    /**
     * Call when the PreparedStatement re-used.
     */
    public static void exitImplicitCacheToActive(Statement statement) {
        try {
            OraclePreparedStatement oraclePreparedStatement = unwrapInternal(statement);

            if (oraclePreparedStatement != null) {
                // oraclePreparedStatement.exitImplicitCacheToActive();
                // 2025-03-04: 同 enterImplicitCache，ojdbc17+ 已不再需要手动干预隐式缓存状态。
            }
        } catch (SQLException e) {
            log.warn(e);
        }
    }

    /**
     * Call when the prepared statement needs to be removed.
     */
    public static void exitImplicitCacheToClose(Statement statement) {
        try {
        OraclePreparedStatement oraclePreparedStatement = unwrapInternal(statement);

        if (oraclePreparedStatement != null) {
            // oraclePreparedStatement.exitImplicitCacheToClose();
            // 2025-03-04: 同 enterImplicitCache，ojdbc17+ 已不再需要手动干预隐式缓存状态。
        }
        } catch(SQLException e) {
            log.warn(e);
        }
    }

    /**
     * Unwrap Statement to get the internal OraclePreparedStatement.
     */
    private static OraclePreparedStatement unwrapInternal(Statement stmt) throws SQLException {
        if (stmt instanceof OraclePreparedStatement) {
            return (OraclePreparedStatement) stmt;
        }

        if (stmt.isWrapperFor(OraclePreparedStatement.class)) {
             return stmt.unwrap(OraclePreparedStatement.class);
        }

        return null;
    }

}
