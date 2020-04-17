package com.umpay.commons.cp;


import com.umpay.commons.cp.util.JdbcUtil;
import com.umpay.commons.util.Formatter;
import com.umpay.commons.util.Logger;

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
    private boolean isDefaultResultSetType = true;
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

    protected String methodDoing;

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
        methodDoing = method.getName();
        if ("toString".equals(methodDoing) && (args == null || args.length == 0)) {
            return toString();//不代理
        }
        try {
            Object obj = _invoke(proxy, method, args);
            if (methodDoing.startsWith("execute") && ! methodDoing.startsWith("executeQuery")) {
                connection.setDirty();
            }
            return obj;
        } catch (SQLException e) {
            if (methodDoing.startsWith("execute")) {
                log.error(e.toString(), "[ErrorCode=", e.getErrorCode(), ";SQLState=", e.getSQLState(), "] on ", methodDoing, "(", getSqlDoing(), ")");
            }
            if (connection.isFetalException(e)) {
                close();
                //sql执行失败不新建连接,直接关闭物理链接,等待Connection的close调用,modify by shenjl at 2015-03-09
                connection.setFatalExceptionHappened(true);
                //connection.recover(e);
            }
            throw e;
        } finally {
            busying = 0;
        }
    }
    
    protected Object _invoke(Object proxy, Method method, Object[] args) throws Throwable {
        long start = System.nanoTime();
        Object ret = null;
        try {
            if (methodDoing.equals("close")) {
                if (! checkOut.getAndSet(false)) {
                    return null;
                }
                timeCheckIn = System.currentTimeMillis();
                if (resultSet != null) {
                    try {
                        resultSet.close();
                    } catch (SQLException e) {
                    }
                    resultSet = null;
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
                    printSQL(log, (System.nanoTime() - start));
                }
            } else if (methodDoing.equals("executeBatch")) {
                ret = real_statement.executeBatch();
                if (isPrintSQL()) {
                    printSQL(log, (System.nanoTime() - start), "[", Array.getLength(ret), "]");
                }
            } else if (methodDoing.equals("executeQuery") && args != null && args.length == 1) {
                sqlDoing = (String) args[0];
                resultSet = real_statement.executeQuery((String) args[0]);
                ret = resultSet;
                if (isPrintSQL()) {
                    printSQL(log, (System.nanoTime() - start));
                }
            } else if (methodDoing.startsWith("execute") && args != null && args.length > 0) {
                sqlDoing = (String) args[0];
                ret = method.invoke(real_statement, args);
                if (isPrintSQL()) {
                    printSQL(log, (System.nanoTime() - start), "[", ret, "]");
                }
            } else {
                ret = method.invoke(real_statement, args);
                if (isVerbose()) {
                    log.trace(statementName, ".", methodDoing, "(...) use ", Formatter.formatNS(System.nanoTime() - start), " ns");
                }
            }
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
        return ret;
    }

    /**
     * 根据执行时间，打印不同级别的SQL日志
     * @param logger
     * @param usedNS
     * @param infos
     */
    protected void printSQL(Logger logger, long usedNS, Object... infos) {
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
        sqlDoing = sqlDoing == null ? "" : JdbcUtil.multiLinesToOneLine(sqlDoing, " ");
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
                log.debug(statementName, " invoking ", methodDoing, "(", getSqlDoing(), ")", " use ", Formatter.formatNS(usedNS), " ns");
            } else if (usedNS/1000 <= connection.getWarnSQLThreshold()*1000 && log.isInfoEnabled()) {
                log.info(statementName, " invoking ", methodDoing, "(", getSqlDoing(), ")", " use ", Formatter.formatNS(usedNS), " ns");
            } else if (log.isWarnEnabled()) {
                log.warn(statementName, " invoking ", methodDoing, "(", getSqlDoing(), ")", " use ", Formatter.formatNS(usedNS), " ns");
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
