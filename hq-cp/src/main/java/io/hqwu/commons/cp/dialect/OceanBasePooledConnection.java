package io.hqwu.commons.cp.dialect;

import io.hqwu.commons.cp.Hqcp;
import io.hqwu.commons.util.Logger;

import java.sql.SQLException;

/**
 * OceanBase 数据库方言的连接池连接实现。
 * <p>
 * 该类继承自 {@link MySQLPooledConnection}，利用 OceanBase 对 MySQL 协议的兼容性，
 * 为 {@link Hqcp} 连接池提供针对 OceanBase 数据库的连接管理与异常处理支持。
 * </p>
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
    public boolean isFatalException(SQLException sqle) {
        if (super.isFatalException(sqle)) {
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
