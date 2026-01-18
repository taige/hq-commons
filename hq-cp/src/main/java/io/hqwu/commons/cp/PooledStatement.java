package io.hqwu.commons.cp;


import io.hqwu.commons.cp.util.JdbcUtil;
import io.hqwu.commons.cp.util.LogUtil;
import io.hqwu.commons.cp.util.SqlMasker;
import io.hqwu.commons.util.ExceptionUtil;
import io.hqwu.commons.util.Formatter;
import io.hqwu.commons.util.Logger;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.*;
import java.util.HashSet;
import java.util.Set;
import java.util.StringJoiner;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

class PooledStatement implements InvocationHandler {
    private static final Logger LOGGER = new Logger();
    
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
     * result set counter
     */
    private int resultSetCounter = 0;

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
        sqlDoing = null;
        String methodDoing = method.getName();
        if ("toString".equals(methodDoing) && (args == null || args.length == 0)) {
            return toString();//不代理
        }
        try {
            busying = System.nanoTime();
            methodBusying = methodDoing;
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
                    LOGGER.info(e.toString(), "[ErrorCode=", ((SQLException) e).getErrorCode(), ";SQLState=", ((SQLException) e).getSQLState(), "] on ", methodDoing, "(", getMaskedSql(), ")");
                } else {
                    // MySQL unexpected exception with mysql-connector-java:8.0.19:
                    // java.lang.NullPointerException: null
                    //	at com.mysql.cj.AbstractQuery.stopQueryTimer(AbstractQuery.java:206)
                    //	at com.mysql.cj.jdbc.StatementImpl.stopQueryTimer(StatementImpl.java:643)
                    //	at com.mysql.cj.jdbc.StatementImpl.executeQuery(StatementImpl.java:1182)
                    LOGGER.error("unexpected exception occurs on ", methodDoing, "(", getMaskedSql(), ")", e);
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
                    LOGGER.trace(statementName, ".close() use ", Formatter.formatNS(System.nanoTime() - start), " ns");
                }
            } else if (methodDoing.equals("addBatch") && args != null && args.length == 1) {
                sqlDoing = (String) args[0];
                real_statement.addBatch((String) args[0]);
                if (isPrintSQL()) {
                    printSQL(LOGGER, methodDoing, (System.nanoTime() - start));
                }
            } else if (methodDoing.equals("executeBatch") || methodDoing.equals("executeLargeBatch")) {
                ret = real_statement.executeBatch();
                if (isPrintSQL()) {
                    printSQL(LOGGER, methodDoing, this::getPreparedSql, (System.nanoTime() - start), "[", Array.getLength(ret), "]");
                }
            } else if (methodDoing.equals("executeQuery") && args != null && args.length == 1) {
                sqlDoing = (String) args[0];
                LoggableResultSet lrs = LoggableResultSet.newInstance(this, real_statement.executeQuery((String) args[0]));
                ret = resultSet = lrs == null ? null : lrs.getResultSet();
                if (isPrintSQL()) {
                    printSQL(LOGGER, methodDoing, (System.nanoTime() - start), "[", (lrs == null ? "rs=null" : "rs=#" + lrs.getRsId()), "]");
                }
            } else if (methodDoing.startsWith("execute") && args != null && args.length > 0) {
                sqlDoing = (String) args[0];
                ret = method.invoke(real_statement, args);
                Object ret4log = onExecuteMethodDone(methodDoing, ret);
                if (isPrintSQL()) {
                    printSQL(LOGGER, methodDoing, (System.nanoTime() - start), "[", ret4log, "]");
                }
            } else if (methodDoing.equals("getMoreResults")) {
                ret = method.invoke(real_statement, args);
                Object ret4log = onExecuteMethodDone(methodDoing, ret);
                if (isVerbose() || isPrintSQL()) {
                    LOGGER.debug(statementName, ".", methodDoing, "(...)", "[", ret4log, "] use ", Formatter.formatNS(System.nanoTime() - start), " ns");
                }
            } else {
                if (methodDoing.equals("getResultSet") && resultSet != null) {
                    // maybe incorrect
                    ret = resultSet;
                } else if (methodDoing.equals("getUpdateCount") && updateCount != null) {
                    ret = updateCount.intValue();
                } else if (methodDoing.equals("getLargeUpdateCount") && updateCount != null) {
                    ret = updateCount;
                } else {
                    ret = method.invoke(real_statement, args);
                }
                if (isVerbose()) {
                    LOGGER.trace(statementName, ".", methodDoing, "(...) use ", Formatter.formatNS(System.nanoTime() - start), " ns");
                }
            }
        } catch (Throwable t) {
            throw ExceptionUtil.unwrapThrowable(t);
        }
        return ret;
    }

    protected Object onExecuteMethodDone(String methodDoing, Object ret) throws SQLException {
//        boolean b = real_statement.execute("");
//        int n = real_statement.executeUpdate("");
//        long l = real_statement.executeLargeUpdate("");
        if (methodDoing.equals("execute") || methodDoing.equals("getMoreResults")) {
            // true if the first result is a ResultSet object;
            // false if it is an update count or there are no results
            if ((Boolean) ret) {
                LoggableResultSet lrs = LoggableResultSet.newInstance(this, real_statement.getResultSet());
                resultSet = lrs == null ? null : lrs.getResultSet();
                return lrs == null ? "rs=null" : "rs=#" + lrs.getRsId();
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

    private long getInfoSQLThreshold() {
        return connection.getConnectionPool().getInfoSQLThreshold();
    }

    private long getWarnSQLThreshold() {
        return connection.getConnectionPool().getWarnSQLThreshold();
    }

    /**
     * 根据执行时间，打印不同级别的SQL日志
     * @param logger
     * @param methodDoing
     * @param usedNS
     * @param infos
     */
    protected void printSQL(Logger logger, String methodDoing, long usedNS, Object... infos) {
        printSQL(logger, methodDoing, this::getMaskedSql, usedNS, infos);
    }

    protected void printSQL(Logger logger, String methodDoing, Supplier<String> sqlSupplier, long usedNS, Object... infos) {
        if (! isPrintSQL()) {
            return;
        }
        if (! LogUtil.isEnabled(logger, usedNS/1000000, getInfoSQLThreshold(), getWarnSQLThreshold())) {
            return;
        }
        LogUtil.logBasedOnThreshold(
                logger, usedNS/1000000, getInfoSQLThreshold(), getWarnSQLThreshold(),
                getStatementName(), ".", methodDoing, "(", sqlSupplier.get(), ")", infos, " use ", Formatter.formatNS(usedNS), " ns"
        );
    }

    protected String getPreparedSql() {
        return "";
    }

    String getMaskedSql() {
        HqcpConfig config = connection.getConnectionPool().getConfig();
        SqlMasker sqlMasker = connection.getConnectionPool().getSqlMasker();
        if (config.isMaskSql() && sqlMasker != null) {
            return sqlMasker.maskSensitiveFields(getSqlDoing());
        }
        return getSqlDoing();
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
        if (isVerbose()) {
            LOGGER.debug(statementName, " real closed.");
        }
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
            if (LogUtil.isEnabled(LOGGER, usedNS/1000000, getInfoSQLThreshold(), getWarnSQLThreshold())) {
                LogUtil.logBasedOnThreshold(
                        LOGGER, usedNS/1000000, getInfoSQLThreshold(), getWarnSQLThreshold(),
                        getStatementName(), " invoking ", methodBusying, "(", getMaskedSql(), ")", " use ", Formatter.formatNS(usedNS), " ns"
                );
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

    /**
     * Created with IntelliJ IDEA for hq-commons-parent
     *
     * @author taige (Wu, Hongqiang)
     * Date: 2021-03-30
     * Time: 2:33 p.m.
     */
    static class LoggableResultSet implements InvocationHandler {
        private static final Set<Integer> BLOB_TYPES = new HashSet<>();
        private boolean first = true;
        private int rows;
        private final Set<Integer> blobColumns = new HashSet<>();

        private final PooledStatement pooledStatement;
        private final ResultSet resultSet;
        private final ResultSet rsProxy;
        private final int rsId;

        static {
            BLOB_TYPES.add(Types.BINARY);
            BLOB_TYPES.add(Types.BLOB);
            BLOB_TYPES.add(Types.CLOB);
            BLOB_TYPES.add(Types.LONGNVARCHAR);
            BLOB_TYPES.add(Types.LONGVARBINARY);
            BLOB_TYPES.add(Types.LONGVARCHAR);
            BLOB_TYPES.add(Types.NCLOB);
            BLOB_TYPES.add(Types.VARBINARY);
        }

        private LoggableResultSet(PooledStatement pooledStatement, ResultSet resultSet) {
            this.pooledStatement = pooledStatement;
            this.resultSet = resultSet;
            this.rsId = pooledStatement.resultSetCounter++;
            this.rsProxy =  (ResultSet) Proxy.newProxyInstance(resultSet.getClass().getClassLoader(), new Class[]{ResultSet.class}, this);
        }

        public static LoggableResultSet newInstance(PooledStatement pooledStatement, ResultSet resultSet) {
            if (resultSet == null) {
                return null;
            }
            return new LoggableResultSet(pooledStatement, resultSet);
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] params) throws Throwable {
            try {
                Object o = method.invoke(resultSet, params);
                if ("next".equals(method.getName())) {
                    if ((Boolean) o) {
                        rows++;
                        if (pooledStatement.isVerbose() && LOGGER.isTraceEnabled()) {
                            ResultSetMetaData rsmd = resultSet.getMetaData();
                            final int columnCount = rsmd.getColumnCount();
                            if (first) {
                                first = false;
                                printColumnHeaders(rsmd, columnCount);
                            }
                            printColumnValues(columnCount);
                        }
                    } else if (pooledStatement.isVerbose() || pooledStatement.isPrintSQL()) {
                        LOGGER.debug("%s.rs#%d.Total: %d", pooledStatement.getStatementName(), this.rsId, rows);
                    }
                }
                return o;
            } catch (Throwable t) {
                throw ExceptionUtil.unwrapThrowable(t);
            }
        }

        private void printColumnHeaders(ResultSetMetaData rsmd, int columnCount) throws SQLException {
            StringJoiner row = new StringJoiner(", ");
            for (int i = 1; i <= columnCount; i++) {
                if (BLOB_TYPES.contains(rsmd.getColumnType(i))) {
                    blobColumns.add(i);
                }
                row.add(rsmd.getColumnLabel(i));
            }
            LOGGER.trace("%s.rs#%d.Columns: %s", pooledStatement.getStatementName(), this.rsId, row);
        }

        private void printColumnValues(int columnCount) {
            StringJoiner row = new StringJoiner(", ");
            for (int i = 1; i <= columnCount; i++) {
                try {
                    if (blobColumns.contains(i)) {
                        row.add("<<BLOB>>");
                    } else {
                        row.add(resultSet.getString(i));
                    }
                } catch (SQLException e) {
                    // generally can't call getString() on a BLOB column
                    row.add("<<Cannot Display>>");
                }
            }
            LOGGER.trace("%s.rs#%d.Row: %s", pooledStatement.getStatementName(), this.rsId, row);
        }

        /**
         * Get the wrapped result set.
         *
         * @return the resultSet
         */
        public ResultSet getResultSet() {
            return rsProxy;
        }

        public int getRsId() {
            return rsId;
        }

    }
}
