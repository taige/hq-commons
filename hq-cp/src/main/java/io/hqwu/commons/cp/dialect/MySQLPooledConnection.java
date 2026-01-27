package io.hqwu.commons.cp.dialect;

import io.hqwu.commons.cp.Hqcp;
import io.hqwu.commons.cp.PooledConnection;
import io.hqwu.commons.util.Logger;

import java.net.SocketTimeoutException;
import java.sql.SQLException;

/**
 * Created with IntelliJ IDEA
 * User: taige
 * Date: 14-3-25
 * Time: 下午10:19
 */
public class MySQLPooledConnection extends PooledConnection {
    private static final Logger LOGGER = new Logger();

    public static final String CONNECT_TIMEOUT = "connectTimeout";
    public static final String SOCKET_TIMEOUT = "socketTimeout";

    public MySQLPooledConnection(Hqcp pool, int connId) throws SQLException {
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
            LOGGER.debug("consider fetal exception because sqlState: %s", sqlState);
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
                // Access denied
            case 1142: // ER_TABLEACCESS_DENIED_ERROR
            case 1227: // ER_SPECIFIC_ACCESS_DENIED_ERROR
            case 1023: // ER_ERROR_ON_CLOSE
            case 1290: // ER_OPTION_PREVENTS_STATEMENT
                LOGGER.debug("consider fetal exception because errorCode: %d", errorCode);
                return true;
            default:
                break;
        }

        // for oceanbase logic moved to OceanBasePooledConnection

        String className = sqle.getClass().getName();
        if (className.endsWith("CommunicationsException")) {
            LOGGER.debug("consider fetal exception because className: %s", className);
            return true;
        }

        String message = sqle.getMessage();
        if (message != null && message.length() > 0) {
            // 兼容 mysql-connector-j 8.x/9.x (com.mysql.cj.*) 以及旧版本
            if (message.startsWith("Streaming result set")
                    && message.contains("is still active. No statements may be issued when any streaming result sets are open and in use on a given connection.")) {
                LOGGER.debug("consider fetal exception because message: %s", message);
                return true;
            }

            final String errorText = message.toUpperCase();

            if ((errorCode == 0 && (errorText.contains("COMMUNICATIONS LINK FAILURE")) //
                    || errorText.contains("COULD NOT CREATE CONNECTION")) //
                    || errorText.contains("NO DATASOURCE") //
                    || errorText.contains("NO ALIVE DATASOURCE")) {
                LOGGER.debug("consider fetal exception because errorText: %s", errorText);
                return true;
            }
        }

        Throwable cause = sqle.getCause();
        for (int i = 0; i < 5 && cause != null; ++i) {
            if (cause instanceof SocketTimeoutException) {
                LOGGER.debug("consider fetal exception because SocketTimeoutException[%d]", i);
                return true;
            }

            className = cause.getClass().getName();
            if (className.endsWith("CommunicationsException")
                    || className.endsWith("ConnectionIsClosedException")
                    || className.endsWith("StatementIsClosedException")) {
                LOGGER.debug("consider fetal exception because className[%d]: %s", i, className);
                return true;
            }

            cause = cause.getCause();
        }
        return false;
    }
}
