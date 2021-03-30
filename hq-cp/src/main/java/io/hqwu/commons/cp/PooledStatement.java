package io.hqwu.commons.cp;


import com.umpay.commons.util.Formatter;
import com.umpay.commons.util.Logger;
import io.hqwu.commons.cp.util.JdbcUtil;

import java.lang.reflect.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.atomic.AtomicBoolean;

class PooledStatement implements InvocationHandler {
    private static final Logger log = new Logger();
    
    /**
     * 归属连接
     */
    private final PooledConnection connection;
    /**
     * 语句Id
     */
    private final int statementId;
    /**
     * 语句名称
     */
    private final String statementName;
    
    /**
     * 封装过的语句
     */
    private Statement statement;
    /**
     * 真实的语句
     */
    private Statement real_statement;
    
    /**
     * 是否检出（使用中）
     */
    protected AtomicBoolean checkOut = new AtomicBoolean(false);
    /**
     * 检入时间
     */
    private long timeCheckIn = System.currentTimeMillis();
    /**
     * 检出时间
     */
    private long timeCheckOut;
    /**
     * 检出线程
     */
    private Thread threadCheckOut;
    
    /**
     * 是否默认的结果集类型
     */
    protected boolean isDefaultResultSetType = true;
    /**
     * 语句打开的结果集
     */
    protected ResultSet resultSet;
    /**
     * 连接是否关闭
     */
    private AtomicBoolean closed = new AtomicBoolean(false);
    
    /**
     * 语句是否正在执行
     */
    private long busying = 0;

    /**
     * 更新语句影响的记录数
     */
    private Long updateCount;

    private String methodBusying;

    private String sqlDoing;

    PooledStatement(PooledConnection conn, Statement stmt, int stmtId) throws SQLException {
        connection = conn;
        statementId = stmtId;
        statementName = conn.getConnectionName() + ".STMT#" + stmtId;
        real_statement = stmt;
        if (stmt.getResultSetType() == ResultSet.TYPE_FORWARD_ONLY
                && stmt.getResultSetConcurrency() == ResultSet.CONCUR_READ_ONLY) {
            isDefaultResultSetType = true;
        } else {
            isDefaultResultSetType = false;
        }
        statement = buildProxy();
    }
    
    public Statement checkOut() throws SQLException {
        if (checkOut.getAndSet(true)) {
            if (threadCheckOut.equals(Thread.currentThread())) {
                return statement;
            } else {
                throw new SQLException(statementName + "已经被"+threadCheckOut.getName()+"检出", "60003");
            }
        }
        resultSet = null;
        updateCount = null;
        timeCheckOut = System.currentTimeMillis();
        threadCheckOut = Thread.currentThread();
        return statement;
    }
    
    @SuppressWarnings("unchecked")
    protected Statement buildProxy() {
        Class[] intfs = real_statement.getClass().getInterfaces();
        boolean impled = false; //是否实现了Connection接口
        for (Class intf: intfs) {
            if (intf.getName().equals(Statement.class.getName())) {
                impled = true;
                break;
            }
        }
        if (!impled) {
            //没有实现Connection接口，则强制增加
            Class[] tmp = intfs;
            intfs = new Class[tmp.length + 1];
            System.arraycopy(tmp, 0, intfs, 0, tmp.length);
            intfs[tmp.length] = Statement.class;
        }
        return (Statement) Proxy.newProxyInstance(real_statement.getClass().getClassLoader(), intfs, this);
    }

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        busying = System.nanoTime();
        sqlDoing = null;
        methodBusying = method.getName();
        if ("toString".equals(methodBusying) && (args == null || args.length == 0)) {
            return toString();//不代理
        }
        String methodDoing = methodBusying;
        try {
            Object obj = _invoke(proxy, method, args);
            if (methodDoing.startsWith("execute") && ! methodDoing.startsWith("executeQuery")) {
                if (! methodDoing.equals("execute") || ! (Boolean) obj) {
                    connection.setDirty();
                }
            }
            return obj;
        } catch (Exception e) {
            if (methodDoing.startsWith("execute")) {
                if (e instanceof SQLException) {
                    log.info(e.toString(), "[ErrorCode=", ((SQLException) e).getErrorCode(), ";SQLState=", ((SQLException) e).getSQLState(), "] on ", methodDoing, "(", getSqlDoing(), ")");
                } else {
                    // MySQL unexpected exception with mysql-connector-java:8.0.19:
                    // java.lang.NullPointerException: null
                    //	at com.mysql.cj.AbstractQuery.stopQueryTimer(AbstractQuery.java:206)
                    //	at com.mysql.cj.jdbc.StatementImpl.stopQueryTimer(StatementImpl.java:643)
                    //	at com.mysql.cj.jdbc.StatementImpl.executeQuery(StatementImpl.java:1182)
                    log.error("unexpected exception occurs on ", methodDoing, "(", getSqlDoing(), ")", e);
                }
            }
            if (! (e instanceof SQLException) || connection.isFetalException((SQLException) e)) {
                close();
                //sql执行失败不新建连接,直接关闭物理链接,等待Connection的close调用,modify by shenjl at 2015-03-09
                connection.setFatalExceptionHappened(true);
                //connection.recover(e);
            }
            throw e;
        } finally {
            busying = 0;
            methodBusying = null;
        }
    }
    
    protected Object _invoke(Object proxy, Method method, Object[] args) throws Throwable {
        long start = System.nanoTime();
        Object ret = null;
        try {
            String methodDoing = method.getName();
            if (methodDoing.equals("close")) {
                if (! checkOut.getAndSet(false)) {
                    return null;
                }
                timeCheckIn = System.currentTimeMillis();
                if (resultSet != null) {
                    try {
                        resultSet.close();
                    } catch (SQLException ignored) {
                    } finally {
                        resultSet = null;
                    }
                }
                if (updateCount != null) {
                    updateCount = null;
                }
                if (this instanceof PooledPreparedStatement) {
                    connection.checkIn((PooledPreparedStatement) this);
                } else {
                    connection.checkIn(this);
                }
                if (isVerbose()) {
                    log.trace(statementName, ".close() use ", Formatter.formatNS(System.nanoTime() - start), " ns");
                }
            } else if (methodDoing.equals("addBatch") && args != null && args.length == 1) {
                sqlDoing = (String) args[0];
                real_statement.addBatch((String) args[0]);
                if (isPrintSQL()) {
                    printSQL(log, methodDoing, (System.nanoTime() - start));
                }
            } else if (methodDoing.equals("executeBatch") || methodDoing.equals("executeLargeBatch")) {
                ret = real_statement.executeBatch();
                if (isPrintSQL()) {
                    printSQL(log, methodDoing, (System.nanoTime() - start), "[", Array.getLength(ret), "]");
                }
            } else if (methodDoing.equals("executeQuery") && args != null && args.length == 1) {
                sqlDoing = (String) args[0];
                resultSet = real_statement.executeQuery((String) args[0]);
                ret = resultSet;
                if (isPrintSQL()) {
                    printSQL(log, methodDoing, (System.nanoTime() - start));
                }
            } else if (methodDoing.startsWith("execute") && args != null && args.length > 0) {
                sqlDoing = (String) args[0];
                ret = method.invoke(real_statement, args);
                Object ret4log = onExecuteMethodDone(methodDoing, ret);
                if (isPrintSQL()) {
                    printSQL(log, methodDoing, (System.nanoTime() - start), "[", ret4log, "]");
                }
            } else {
                if (methodDoing.equals("getResultSet") && resultSet != null) {
                    // maybe incorrect
                    ret = resultSet;
                } else if (methodDoing.equals("getUpdateCount") && updateCount != null) {
                    ret = updateCount;
                } else {
                    ret = method.invoke(real_statement, args);
                }
                if (isVerbose()) {
                    log.trace(statementName, ".", methodDoing, "(...) use ", Formatter.formatNS(System.nanoTime() - start), " ns");
                }
            }
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
        return ret;
    }

    protected Object onExecuteMethodDone(String methodDoing, Object ret) throws SQLException {
//        boolean b = real_statement.execute("");
//        int n = real_statement.executeUpdate("");
//        long l = real_statement.executeLargeUpdate("");
        if (methodDoing.equals("execute")) {
            // true if the first result is a ResultSet object;
            // false if it is an update count or there are no results
            if ((Boolean) ret) {
                resultSet = real_statement.getResultSet();
                return "rs=" + (resultSet != null);
            } else {
                updateCount = (long) real_statement.getUpdateCount();
            }
        } else {
            if (int.class.isAssignableFrom(ret.getClass())
                    || Integer.class.isAssignableFrom(ret.getClass())) {
                updateCount = (long) (int) ret;
            } else if (long.class.isAssignableFrom(ret.getClass())
                    || Long.class.isAssignableFrom(ret.getClass())) {
                updateCount = (long) ret;
            } else {
                return ret;
            }
        }
        return updateCount;
    }

    /**
     * 根据执行时间，打印不同级别的SQL日志
     * @param logger
     * @param methodDoing
     * @param usedNS
     * @param infos
     */
    protected void printSQL(Logger logger, String methodDoing, long usedNS, Object... infos) {
        if (! isPrintSQL()) {
            return;
        }
        if (usedNS/1000 <= connection.getInfoSQLThreshold()*1000) {
            if (logger.isDebugEnabled()) {
                logger.debug(getStatementName(), ".", methodDoing, "(", getSqlDoing(), ")", infos, " use ", Formatter.formatNS(usedNS), " ns");
            }
        } else if (usedNS/1000 <= connection.getWarnSQLThreshold()*1000) {
            if (logger.isInfoEnabled()) {
                logger.info(getStatementName(), ".", methodDoing, "(", getSqlDoing(), ")", infos, " use ", Formatter.formatNS(usedNS), " ns");
            }
        } else if (logger.isWarnEnabled()) {
            logger.warn(getStatementName(), ".", methodDoing, "(", getSqlDoing(), ")", infos, " use ",  Formatter.formatNS(usedNS), " ns");
        }
    }

    protected String getSqlDoing() {
        sqlDoing = sqlDoing == null ? "" : JdbcUtil.removeBreakingWhitespace(sqlDoing);
        return sqlDoing;
    }

    @Override
    public String toString() {
        return statementName + "{" + real_statement + "}";
    }

    public Statement getProxy() {
        return statement;
    }
    
    public Statement getStatement() {
        return real_statement;
    }

    /**
     * Return statementId.
     * @return statementId
     */
    public int getStatementId() {
        return statementId;
    }

    /**
     * Return checkOut.
     * @return checkOut
     */
    public boolean isCheckOut() {
        return checkOut.get();
    }

    /**
     * Return isDefaultResultSetType.
     * @return isDefaultResultSetType
     */
    public boolean isDefaultResultSetType() {
        return isDefaultResultSetType;
    }
    
    public void close() {
        if (closed.getAndSet(true)) {
            return;
        }
        try {
            real_statement.close();
        } catch (SQLException e) {
        }
        log.debug(statementName, " real closed.");
    }

    /**
     * Return statementName.
     * @return statementName
     */
    public String getStatementName() {
        return statementName;
    }
    
    public boolean isVerbose() {
        return connection.isVerbose();
    }

    public boolean isPrintSQL() {
        return connection.isPrintSQL();
    }

    public long getCheckOutTime() {
        if (checkOut.get()) {
            return System.currentTimeMillis() - timeCheckOut;
        }
        return 0L;
    }
    
    public boolean isBusying() {
        if (checkOut.get() && busying > 0) {
            long usedNS = System.nanoTime() - busying;
            if (usedNS/1000 <= connection.getInfoSQLThreshold()*1000 && log.isDebugEnabled()) {
                log.debug(statementName, " invoking ", methodBusying, "(", getSqlDoing(), ")", " use ", Formatter.formatNS(usedNS), " ns");
            } else if (usedNS/1000 <= connection.getWarnSQLThreshold()*1000 && log.isInfoEnabled()) {
                log.info(statementName, " invoking ", methodBusying, "(", getSqlDoing(), ")", " use ", Formatter.formatNS(usedNS), " ns");
            } else if (log.isWarnEnabled()) {
                log.warn(statementName, " invoking ", methodBusying, "(", getSqlDoing(), ")", " use ", Formatter.formatNS(usedNS), " ns");
            }
            return true;
        }
        return false;
    }
    
    public boolean isClosed() {
        return closed.get();
    }

    /**
     * Return timeCheckIn.
     * @return timeCheckIn
     */
    public long getTimeCheckIn() {
        return timeCheckIn;
    }
    
}
