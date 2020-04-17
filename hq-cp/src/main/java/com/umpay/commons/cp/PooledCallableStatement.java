package com.umpay.commons.cp;

import java.lang.reflect.Proxy;
import java.sql.CallableStatement;
import java.sql.SQLException;
import java.sql.Statement;

class PooledCallableStatement extends PooledPreparedStatement {

    PooledCallableStatement(PooledConnection conn, CallableStatement stmt, int stmtId, String sql) throws SQLException {
        super(conn, stmt, stmtId, sql);
    }

    @SuppressWarnings("unchecked")
    protected CallableStatement buildProxy() {
        Statement stmt = getStatement();
        Class[] intfs = stmt.getClass().getInterfaces();
        boolean impled = false; //是否实现了Connection接口
        for (Class intf: intfs) {
            if (intf.getName().equals(CallableStatement.class.getName())) {
                impled = true;
                break;
            }
        }
        if (!impled) {
            //没有实现Connection接口，则强制增加
            Class[] tmp = intfs;
            intfs = new Class[tmp.length + 1];
            System.arraycopy(tmp, 0, intfs, 0, tmp.length);
            intfs[tmp.length] = CallableStatement.class;
        }
        return (CallableStatement) Proxy.newProxyInstance(stmt.getClass().getClassLoader(), intfs, this);
    }
}
