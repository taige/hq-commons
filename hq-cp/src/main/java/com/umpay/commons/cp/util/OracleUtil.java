package com.umpay.commons.cp.util;

import com.umpay.commons.util.Logger;
import oracle.jdbc.internal.OraclePreparedStatement;

import java.sql.SQLException;
import java.sql.Statement;

/**
 * Some Oracle specific com.umpay.commons.util.utils.
 *
 * Created by jianbin on 2/14/14.
 */
public class OracleUtil {
    private static final Logger log = new Logger();

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
                oraclePreparedStatement.enterImplicitCache();
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
                oraclePreparedStatement.exitImplicitCacheToActive();
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
            oraclePreparedStatement.exitImplicitCacheToClose();
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

        OraclePreparedStatement unwrapped = stmt.unwrap(OraclePreparedStatement.class);

        if (unwrapped == null) {
            log.error("can not unwrap statement : " + stmt.getClass());
        }

        return unwrapped;
    }

}
