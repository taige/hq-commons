package io.hqwu.commons.cp;

import java.lang.reflect.Proxy;
import java.sql.CallableStatement;
import java.sql.SQLException;
import java.sql.Statement;


/**
 * {@link CallableStatement} 的池化包装实现。
 * <p>
 * 继承自 {@link PooledPreparedStatement}，专门用于在连接池环境中管理和复用存储过程调用语句。
 * 通过动态代理机制封装底层驱动的语句对象，协调其与 {@link PooledConnection} 的交互。
 * </p>
 *
 * @see PooledConnection
 * @see PooledPreparedStatement
 * @see CallableStatement
 */
public class PooledCallableStatement extends PooledPreparedStatement {

    PooledCallableStatement(PooledConnection conn, CallableStatement stmt, int stmtId, Object[] args) throws SQLException {
        super(conn, stmt, stmtId, args);
    }

    protected CallableStatement buildProxy() {
        Statement stmt = getStatement();
        return (CallableStatement) Proxy.newProxyInstance(stmt.getClass().getClassLoader(), new Class[] {CallableStatement.class}, this);
    }
}
