package com.umpay.commons.cp.dialect;

import com.umpay.commons.cp.PooledConnection;
import com.umpay.commons.cp.PooledPreparedStatement;
import com.umpay.commons.cp.util.OracleUtil;
import com.umpay.commons.util.Logger;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

/**
 *
 */
class OraclePooledPreparedStatement extends PooledPreparedStatement {
    private static final Logger log = new Logger();

    private boolean useOracleImplicitCache;

    OraclePooledPreparedStatement(PooledConnection conn, PreparedStatement stmt, int stmtId, String sql,
                                  boolean useOracleImplicitCache) throws SQLException {
        super(conn, stmt, stmtId, sql);
        this.useOracleImplicitCache = useOracleImplicitCache;
    }

    @Override
    public void cleanCache() {
        if (useOracleImplicitCache) {
            OracleUtil.enterImplicitCache(this.getStatement());
        }
    }

    @Override
    public Statement checkOut() throws SQLException {
        if (useOracleImplicitCache) {
            OracleUtil.exitImplicitCacheToActive(this.getStatement());
        }
        return super.checkOut();
    }

    @Override
    public void close() {
        if (useOracleImplicitCache) {
            OracleUtil.exitImplicitCacheToClose(this.getStatement());
        }
        super.close();
    }

}
