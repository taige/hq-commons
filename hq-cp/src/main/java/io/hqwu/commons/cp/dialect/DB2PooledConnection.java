package io.hqwu.commons.cp.dialect;

import io.hqwu.commons.cp.Hqcp;
import io.hqwu.commons.cp.PooledConnection;

import java.sql.SQLException;

/**
 * DB2 数据库连接池连接实现类。
 *
 * <p>该类扩展了 {@link PooledConnection}，专门用于处理 DB2 数据库特定的连接逻辑。
 * 其核心功能是根据 DB2 的错误代码识别致命异常（Fatal Exception），从而协助 {@link Hqcp}
 * 连接池管理连接的有效性，确保在发生不可恢复的数据库错误时能够正确回收并替换失效连接。</p>
 *
 * @author taige
 * @since 2014-03-25
 */
public class DB2PooledConnection extends PooledConnection {

    public DB2PooledConnection(Hqcp pool, int connId) throws SQLException {
        super(pool, connId);
    }

    @Override
    public boolean isFatalException(SQLException sqle) {
        if (super.isFatalException(sqle)) {
            return true;
        }

        int errorCode = sqle.getErrorCode();
        switch (errorCode) {
            case -512: // STATEMENT REFERENCE TO REMOTE OBJECT IS INVALID
            case -514: // THE CURSOR IS NOT IN A PREPARED STATE
            case -516: // THE DESCRIBE STATEMENT DOES NOT SPECIFY A PREPARED STATEMENT
            case -518: // THE EXECUTE STATEMENT DOES NOT IDENTIFY A VALID PREPARED STATEMENT
            case -525: // THE SQL STATEMENT CANNOT BE EXECUTED BECAUSE IT WAS IN ERROR AT BIND TIME FOR SECTION = sectno
                // PACKAGE = pkgname CONSISTENCY TOKEN = contoken
            case -909: // THE OBJECT HAS BEEN DELETED OR ALTERED
            case -918: // THE SQL STATEMENT CANNOT BE EXECUTED BECAUSE A CONNECTION HAS BEEN LOST
            case -924: // DB2 CONNECTION INTERNAL ERROR, function-code,return-code,reason-code
                return true;
            default:
                break;
        }
        return false;
    }
}
