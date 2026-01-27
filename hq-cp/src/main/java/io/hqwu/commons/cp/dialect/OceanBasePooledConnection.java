package io.hqwu.commons.cp.dialect;

import io.hqwu.commons.cp.Hqcp;
import io.hqwu.commons.util.Logger;

import java.sql.SQLException;

/**
 * OceanBase Dialect implementation.
 * OceanBase is compatible with MySQL protocol, so we extend MySQLPooledConnection.
 *
 * @author Wu, Hongqiang
 * @since 2026-01-27
 */
public class OceanBasePooledConnection extends MySQLPooledConnection {
    private static final Logger LOGGER = new Logger();

    public OceanBasePooledConnection(Hqcp pool, int connId) throws SQLException {
        super(pool, connId);
    }

    @Override
    public boolean isFetalException(SQLException sqle) {
        if (super.isFetalException(sqle)) {
            return true;
        }

        int errorCode = sqle.getErrorCode();
        // for oceanbase
        if (errorCode >= -10000 && errorCode <= -9000) {
            LOGGER.debug("consider fetal exception because errorCode: %d", errorCode);
            return true;
        }
        return false;
    }
}
