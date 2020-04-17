package com.umpay.commons.cp.dialect;

import com.umpay.commons.cp.PooledConnection;
import com.umpay.commons.cp.UmpayCP;

import java.sql.SQLException;

/**
 * Created with IntelliJ IDEA
 * User: taige
 * Date: 14-3-25
 * Time: 下午10:19
 */
public class MySQLPooledConnection extends PooledConnection {

    public static final String CONNECT_TIMEOUT = "connectTimeout";
    public static final String SOCKET_TIMEOUT = "socketTimeout";

    public MySQLPooledConnection(UmpayCP pool, int connId) throws SQLException {
        super(pool, connId);
    }

    @Override
    public boolean isFetalException(SQLException sqle) {
        if (super.isFetalException(sqle)) {
            return true;
        }
        String sqlState = sqle.getSQLState();
        if (sqlState == null || sqlState.equals("40001")) {
            // sqlState == 40001 is mysql specific triggered when a deadlock is detected
            return true;
        }

        int errorCode = sqle.getErrorCode();
        switch (errorCode) {
            // Communications Errors
            case 1040: // ER_CON_COUNT_ERROR
            case 1042: // ER_BAD_HOST_ERROR
            case 1043: // ER_HANDSHAKE_ERROR
            case 1047: // ER_UNKNOWN_COM_ERROR
            case 1081: // ER_IPSOCK_ERROR
            case 1129: // ER_HOST_IS_BLOCKED
            case 1130: // ER_HOST_NOT_PRIVILEGED
                // Authentication Errors
            case 1045: // ER_ACCESS_DENIED_ERROR
                // Resource errors
            case 1004: // ER_CANT_CREATE_FILE
            case 1005: // ER_CANT_CREATE_TABLE
            case 1015: // ER_CANT_LOCK
            case 1021: // ER_DISK_FULL
            case 1041: // ER_OUT_OF_RESOURCES
                // Out-of-memory errors
            case 1037: // ER_OUTOFMEMORY
            case 1038: // ER_OUT_OF_SORTMEMORY
                return true;
            default:
                break;
        }

        if (errorCode >= -10000 && errorCode <= -9000) {
            return true;
        }

        String message = sqle.getMessage();
        if (message != null && message.length() > 0) {
            final String errorText = message.toUpperCase();

            if ((errorCode == 0 && (errorText.indexOf("COMMUNICATIONS LINK FAILURE") > -1) //
                    || errorText.indexOf("COULD NOT CREATE CONNECTION") > -1) //
                    || errorText.indexOf("NO DATASOURCE") > -1 //
                    || errorText.indexOf("NO ALIVE DATASOURCE") > -1) {
                return true;
            }
        }
        return false;
    }
}
