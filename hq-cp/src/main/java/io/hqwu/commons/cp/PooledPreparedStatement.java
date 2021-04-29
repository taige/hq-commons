package io.hqwu.commons.cp;

import com.umpay.commons.util.ClassUtil;
import com.umpay.commons.util.ExceptionUtil;
import com.umpay.commons.util.Logger;
import io.hqwu.commons.cp.util.JdbcUtil;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

public class PooledPreparedStatement extends PooledStatement {
    private static final Logger LOGGER = new Logger();
    
    private PreparedStatement pstmt;
    private PreparedStatement real_pstmt;
    
    private final String preparedSql;
    private final Object paras[];

    private String sqlDoing;

    protected PooledPreparedStatement(PooledConnection conn, PreparedStatement stmt, int stmtId, Object[] args) throws SQLException {
        super(conn, stmt, stmtId);
        if (args.length != 1 && args.length != 3) {
            super.isDefaultResultSetType = false;
        }
        String sql = (String) args[0];
        real_pstmt = (PreparedStatement) getStatement();
        this.preparedSql = sql == null ? "" : JdbcUtil.removeBreakingWhitespace(sql);
        this.paras = new Object[getQMCount()];
    }

    protected String getPreparedSql() {
        return preparedSql;
    }

    /*
    * Do nothing for generic PooledPreparedStatement. For subclass implementation.
    */
    public void cleanCache() {}

    @SuppressWarnings("unchecked")
    protected PreparedStatement buildProxy() {
        Statement stmt = getStatement();
        Class[] intfs = stmt.getClass().getInterfaces();
        boolean impled = false; //是否实现了Connection接口
        for (Class intf: intfs) {
            if (intf.getName().equals(PreparedStatement.class.getName())) {
                impled = true;
                break;
            }
        }
        if (!impled) {
            //没有实现Connection接口，则强制增加
            Class[] tmp = intfs;
            intfs = new Class[tmp.length + 1];
            System.arraycopy(tmp, 0, intfs, 0, tmp.length);
            intfs[tmp.length] = PreparedStatement.class;
        }
        pstmt = (PreparedStatement) Proxy.newProxyInstance(stmt.getClass().getClassLoader(), intfs, this);
        return pstmt;
    }

    protected Object _invoke(Object proxy, Method method, Object[] args) throws Throwable {
        long start = System.nanoTime();
        Object ret = null;
        sqlDoing = null;
        try {
            String methodDoing = method.getName();
            if (methodDoing.equals("addBatch") && (args == null || args.length == 0)) {
                real_pstmt.addBatch();
                if (isPrintSQL()) {
                    printSQL(LOGGER, methodDoing, (System.nanoTime() - start));
                }
            } else if (methodDoing.equals("execute") && (args == null || args.length == 0)) {
                ret = real_pstmt.execute();
                Object ret4log = onExecuteMethodDone(methodDoing, ret);
                if (isPrintSQL()) {
                    printSQL(LOGGER, methodDoing, (System.nanoTime() - start), "[", ret4log, "]");
                }
            } else if (methodDoing.equals("executeQuery") && (args == null || args.length == 0)) {
                LoggableResultSet lrs = LoggableResultSet.newInstance(this, real_pstmt.executeQuery());
                ret = resultSet = lrs == null ? null : lrs.getResultSet();
                if (isPrintSQL()) {
                    printSQL(LOGGER, methodDoing, (System.nanoTime() - start), "[", (lrs == null ? "rs=null" : "rs=#" + lrs.getRsId()), "]");
                }
            } else if (methodDoing.equals("executeUpdate") && (args == null || args.length == 0)) {
                ret = real_pstmt.executeUpdate();
                if (isPrintSQL()) {
                    printSQL(LOGGER, methodDoing, (System.nanoTime() - start), "[", ret, "]");
                }
            } else {
                if (methodDoing.startsWith("set") && args.length >= 2 && args[0] instanceof Integer) {
                    int idx = (Integer) args[0];
                    if (paras.length >= idx) {
                        // myBatis会先调用setBNull 2017.1.18
                        if (methodDoing.equals("setNull")) {
                            paras[idx - 1] = null;
                        } else {
                            paras[idx - 1] = args[1];
                        }
                    }
                }
                ret = super._invoke(proxy, method, args);
            }
        } catch (Throwable t) {
            throw ExceptionUtil.unwrapThrowable(t);
        }
        return ret;
    }

    private int getQMCount() {
        int c = 0;
        int idx = 0;
        while (true) {
            idx = preparedSql.indexOf("?", idx + 1);
            if (idx == -1) {
                break;
            }
            c++;
        }
        return c;
    }

    protected String getSqlDoing() {
        if (sqlDoing != null) {
            return sqlDoing;
        }
        if (paras.length == 0) {
            return preparedSql;
        }
        StringBuilder sb = new StringBuilder(preparedSql.length() + paras.length * 16);
        int idx = 0;
        for (int i = 0; i < paras.length; i++) {
            Object p = paras[i];
            int idxNext = preparedSql.indexOf("?", idx);
//            if (p == null) {
//                idx = idxNext+1;
//                continue;
//            }
            if (idxNext < 0) {
                break;
            }
            sb.append(preparedSql, idx, idxNext);
            if (p == null) {
                sb.append("NULL");
            } else if (ClassUtil.isPrimitiveOrWrapper(p.getClass()) && p.getClass() != char.class && p.getClass() != Character.class) {
                sb.append(p);
            } else {
                sb.append('\'').append(p).append('\'');
            }
            idx = idxNext+1;
        }
        sb.append(preparedSql.substring(idx));
        sqlDoing = sb.toString();
        return sqlDoing;
    }
}
