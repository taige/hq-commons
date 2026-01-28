package io.hqwu.commons.cp;

import io.hqwu.commons.cp.util.JdbcUtil;
import io.hqwu.commons.util.ExceptionUtil;
import io.hqwu.commons.util.Formatter;
import io.hqwu.commons.util.JMXUtil;
import io.hqwu.commons.util.Logger;
import org.apache.commons.lang3.ArrayUtils;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.*;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * 数据库连接的包装类，是 {@link Hqcp} 连接池的核心组件。
 * <p>
 * 该类通过动态代理模式封装真实的 {@link java.sql.Connection}，主要职责包括：
 * <ul>
 *   <li>拦截 {@code close()} 调用，将连接归还至连接池而非直接关闭物理连接。</li>
 *   <li>管理并缓存 {@link PooledStatement}、{@link PooledPreparedStatement} 和 {@link PooledCallableStatement}，以提高 SQL 执行效率。</li>
 *   <li>提供连接状态监控与生命周期管理，支持通过 {@link PooledConnectionMBean} 进行 JMX 远程管理。</li>
 *   <li>具备致命异常检测与自动恢复机制，在检测到数据库连接失效时尝试重建物理连接。</li>
 * </ul>
 *
 * @author wuhq, zhangyao, shenjl
 * @since 2011-09-02
 */
public class PooledConnection implements InvocationHandler, PooledConnectionMBean {
    private static final Logger log = new Logger();

    /**
     * 连接编号
     */
    private AtomicInteger statementNo = new AtomicInteger(0);

    /**
     * 归属连接池
     */
    private final Hqcp connectionPool;
    /**
     * 连接Id
     */
    private final int connectionId;
    /**
     * 连接名称
     */
    private final String connectionName;

    /**
     * 封装过的连接
     */
    private Connection connection;
    /**
     * 真实的数据库连接
     */
    private Connection real_connection;

    /**
     * 是否检出（使用中）
     */
    private AtomicBoolean checkOut = new AtomicBoolean(false);
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
     * 当前缓存的语句数
     */
    private AtomicInteger validStatementNum = new AtomicInteger(0);
    /**
     * 空闲语句
     */
    private LinkedBlockingQueue<PooledStatement> idleStatementsPool;
    /**
     * 使用中语句
     */
    private ConcurrentHashMap<Integer, PooledStatement> activeStatementsPool;
    /**
     * 可用的预编译语句
     */
    private final LinkedHashMap<String, PooledPreparedStatement> validPreStatementsPool;
    /**
     * 连接是否关闭
     */
    private AtomicBoolean closed = new AtomicBoolean(true);
    /**
     * 在调用代理的close时是否关闭物理链接
     */
    private boolean fatalExceptionHappened;

    public void setFatalExceptionHappened(boolean fatalExceptionHappened) {
        this.fatalExceptionHappened = fatalExceptionHappened;
    }
    public boolean isFatalExceptionHappened() {
        return this.fatalExceptionHappened;
    }

    /**
     * 操作锁
     */
    private final ReentrantLock operLock = new ReentrantLock();

    private boolean autoCommit = false;

    private boolean dirty = false;

    /**
     * 连接建立时间
     */
    private long timeConnected;

    protected PooledConnection(Hqcp pool, int connId) throws SQLException {
        connectionPool = pool;
        connectionId = connId;
        connectionName = pool.getPoolName() + "#" + connId;
        idleStatementsPool = new LinkedBlockingQueue<PooledStatement>(connectionPool.getConfig().getMaxStatements());
        activeStatementsPool = new ConcurrentHashMap<Integer, PooledStatement>(connectionPool.getConfig().getMaxStatements());
        validPreStatementsPool = new LinkedHashMap<String, PooledPreparedStatement>(connectionPool.getConfig().getMaxPreStatements()+1, 0.75f, true) {
            private static final long serialVersionUID = -5350521942562100031L;
            protected boolean removeEldestEntry(Map.Entry<String, PooledPreparedStatement> eldest) {
                if (size() > connectionPool.getConfig().getMaxPreStatements()) {
                    PooledPreparedStatement ppstmt = eldest.getValue();
                    if (ppstmt.isCheckOut()) {
                        return false;
                    }
                    ppstmt.close();
                    return true;
                }
                return false;
             }
        };
        makeRealConnection();
        connection = buildProxy();
        if (pool.getConfig().getJmxLevel() > 1) {
            JMXUtil.register(this.getClass().getPackage().getName() + ":type=pool-" + pool.getPoolName() + ",name=" + getConnectionName(), this);
        }
    }

    private void makeRealConnection() throws SQLException {
        if (! closed.get()) {
            return;
        }
        long start = System.nanoTime();

        real_connection = DriverManager.getConnection(connectionPool.getConfig().getUrl(), connectionPool.getConfig().getConnectionProperties());
        timeConnected = System.currentTimeMillis();

        //real_connection.setAutoCommit(autoCommit);
        this.autoCommit = real_connection.getAutoCommit();
        log.info(connectionName, " make new connection to ", connectionPool.getConfig().getUrl(), " use ", Formatter.formatNS(System.nanoTime() - start), " ns");
        closed.set(false);
        setFatalExceptionHappened(false);//默认不关闭链接
    }

    /**
     * 根据连接存活时间，计算销毁连接还需要经过的时间(ms)
     * @return 销毁连接需要等待的毫秒
     */
    public long millisToDestroy() {
        return this.connectionPool.getConfig().getLifetimeSec() <= 0
                ? Long.MAX_VALUE
                : (timeConnected + this.connectionPool.getConfig().getLifetimeMillisec()) - System.currentTimeMillis();
    }

    /**
     * 计算连接应该要被检测需要等待的毫秒
     * @return 需要等待的毫秒
     */
    public long millisToCheckIt() {
        // 检入时间 + 检测间隔 - 当前时间
        return timeCheckIn + this.connectionPool.getConfig().getIdleTimeoutMillisec() - System.currentTimeMillis();
    }

    public boolean recover(SQLException sqle) {
        if (isFatalException(sqle)) {
            close();
            try {
                makeRealConnection();

                log.info("recover ok from exception: ", sqle);

                return true;
            } catch (Exception e) {
                log.info("recover fail");
                log.info(e);
            }
        }
        return false;
    }

    /**
     * 是否是致命的异常，如果是，则需要重建连接
     * @param sqle
     * @return
     */
    public boolean isFatalException(SQLException sqle) {
        if (sqle instanceof SQLRecoverableException) {
            log.debug("consider fetal exception because SQLRecoverableException");
            return true;
        }

        String sqls = sqle.getSQLState();
        if (sqls == null || sqls.startsWith("08")) { // Connection Exception
            log.debug("consider fetal exception because sqlState: %s", sqls);
            return true;
        }

        // SQL-92 says:
        //		 Class values that begin with one of the <digit>s '5', '6', '7',
        //         '8', or '9' or one of the <simple Latin upper case letter>s 'I',
        //         'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V',
        //         'W', 'X', 'Y', or 'Z' are reserved for implementation-specified
        //         conditions.
        // FIXME: We should look into this.connection.getMetaData().getSQLStateType();
        // to determine if we have SQL:92 or X/OPEN sqlstatus codes.

        char firstChar = sqls.charAt(0);
        if (firstChar >= '5' && firstChar <='9') {
            log.debug("consider fetal exception because sqlState[5-9]: %s", sqls);
            return true;
        }
        return false;
    }

    public Connection getProxy() {
        return connection;
    }

    public void lock() throws InterruptedException {
        operLock.lockInterruptibly();
    }

    public void unlock() {
        operLock.unlock();
    }

    public Connection checkOut(boolean autoCommit) throws SQLException {
        try {
            lock();
        } catch (InterruptedException e) {
            //throw new SQLException("lock be interrupted", "60002");
            throw new SQLException("waiting for a free available connection be interrupted", "08001");
        }
        try {
            if (closed.get()) {
                makeRealConnection();
            }
            //20121119 zhangyao 使用代理的connect，以便自动重连
            if (autoCommit != this.autoCommit) {
                connection.setAutoCommit(autoCommit);//
            }

            if (checkOut.getAndSet(true)) {
                //throw new SQLException(connectionName + "已经检出", "60001");
                throw new SQLException("connection of " + connectionName + " had be checkout! ", "08001");
            }
            timeCheckOut = System.currentTimeMillis();
            threadCheckOut = Thread.currentThread();
            //20121119 zhangyao 如果setautocommit存在异常，导致checkout标记无法回滚
            // real_connection.setAutoCommit(autoCommit);

            this.autoCommit = real_connection.getAutoCommit();
            return connection;
        } finally {
            unlock();
        }
    }

    private Connection buildProxy() {
        return (Connection) Proxy.newProxyInstance(real_connection.getClass().getClassLoader(), new Class[]{Connection.class}, this);
    }

    public String toString() {
        return connectionName + "{" + real_connection + "}";
    }

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        String mname = method.getName();
        if (mname.equals("toString") && args == null) {
            return toString();
        }
        if (closed.get() && !mname.equals("close") && !mname.equals("isClosed")) {
            makeRealConnection();
        }
        try {
            return _invoke(proxy, method, args);
        } catch (SQLException e) {
            if (recover(e)) {
                //连接异常，并且重连成功
                if (autoCommit) {
                    //非事务模式，则重新尝试调用
                    return _invoke(proxy, method, args);
                }
                //事务模式，则抛出异常
            }
            throw e;
        }
    }

    @SuppressWarnings("unchecked")
    private Object _invoke(Object proxy, Method method, Object[] args) throws Throwable {
        long invokeStart = System.nanoTime();
        Object ret = null;
        String mname = method.getName();
        try {
            if (mname.equals("close")) {
                checkIn();
                if (isFatalExceptionHappened()) {//Statement执行发生错误，需要关闭物理链接，shenjl
                    close();
                }
                if (isPrintSQL() || isVerbose()) {
                    log.debug(connectionName, ".close()[" , isFatalExceptionHappened() ,"] use ", Formatter.formatNS(System.nanoTime() - invokeStart), " ns");
                }
            } else if (mname.equals("createStatement")) {
                ret = createStatement(method, args);
                if (isVerbose()) {
                    log.trace(connectionName, ".", mname, "(...) use ", Formatter.formatNS(System.nanoTime() - invokeStart), " ns");
                }
            } else if (mname.equals("prepareStatement")) {
                ret = prepareStatement(method, args);
                if (isVerbose()) {
                    log.trace(connectionName, ".", mname, "(...) use ", Formatter.formatNS(System.nanoTime() - invokeStart), " ns");
                }
            } else if (mname.equals("prepareCall")) {
                ret = prepareCall(method, args);
                if (isVerbose()) {
                    log.trace(connectionName, ".", mname, "(...) use ", Formatter.formatNS(System.nanoTime() - invokeStart), " ns");
                }
            } else if (mname.equals("commit") || mname.equals("rollback")) {
                ret = method.invoke(real_connection, args);
                dirty = false;
                if (isVerbose()) {
                    log.debug(connectionName, ".", mname, "() use ", Formatter.formatNS(System.nanoTime() - invokeStart), " ns");
                }
            } else if (mname.equals("setAutoCommit")) {
                ret = method.invoke(real_connection, args);
                this.autoCommit = real_connection.getAutoCommit();
                if (isVerbose()) {
                    log.debug(connectionName, ".", mname, "(", args[0], ") use ", Formatter.formatNS(System.nanoTime() - invokeStart), " ns");
                }
            } else if (mname.equals("isWrapperFor")) {
                ret = JdbcUtil.isWrapperFor((Class<?>) args[0], this, proxy, real_connection);
                if (isVerbose()) {
                    log.debug(connectionName, ".", mname, "(", args[0], ") use ", Formatter.formatNS(System.nanoTime() - invokeStart), " ns");
                }
            } else if (mname.equals("unwrap")) {
                ret = JdbcUtil.unwrap((Class<?>) args[0], this, proxy, real_connection);
                if (isVerbose()) {
                    log.debug(connectionName, ".", mname, "(", args[0], ") use ", Formatter.formatNS(System.nanoTime() - invokeStart), " ns");
                }
            } else {
                if (mname.equals("isClosed")) {
                    // 如果未检出(!isCheckOut)，或内部已标记关闭，或物理连接已关闭，则返回 true
                    ret = !isCheckOut() || closed.get() || (real_connection != null && real_connection.isClosed());
                } else {
                    ret = method.invoke(real_connection, args);
                }
                if (isVerbose()) {
                    log.trace(connectionName, ".", mname, "(...) use ", Formatter.formatNS(System.nanoTime() - invokeStart), " ns");
                }
            }
        } catch (Throwable t) {
            throw ExceptionUtil.unwrapThrowable(t);
        }
        return ret;
    }

    /**
     * 回收连接
     * @throws SQLException
     */
    protected void checkIn() throws SQLException {
        if (! checkOut.getAndSet(false)) {
            return;
        }
        timeCheckIn = System.currentTimeMillis();
        //if (! real_connection.getAutoCommit() && dirty) {
        if (! autoCommit && dirty) {
            //不是自动提交的时候，做关闭前的提交或回滚
            try {
                if (connectionPool.getConfig().isCommitOnClose()) {
                    real_connection.commit();
                    if (isVerbose()) {
                        log.debug(connectionName, ".commit() on close");
                    }
                } else {
                    real_connection.rollback();
                    if (isVerbose()) {
                        log.debug(connectionName, ".rollback() on close");
                    }
                }
            } catch (SQLException e) {
                log.warn(e);
            }
            dirty = false;
        }
        //回收活动的语句
        for (Object obj : activeStatementsPool.entrySet().toArray()) {
            Map.Entry<Integer, PooledStatement> entry = (Map.Entry<Integer, PooledStatement>) obj;
            PooledStatement pstmt = entry.getValue();
            pstmt.getProxy().close();
            if (isVerbose()) { //add by wuhq 2011.09.02
                log.debug(pstmt.getStatementName(), " force to close.");
            }
        }
        connectionPool.checkIn(this);
    }

    public Statement createStatement(Method method, Object[] args) throws Throwable {
        PooledStatement pstmt = null;
        if (args == null) {
            //没有参数的createStatement才试图从池中获取取
            pstmt = idleStatementsPool.poll();
//        } else {
//            return (Statement) method.invoke(real_connection, args);
        }
        if (pstmt == null) {
            //没有空闲连接
            if (validStatementNum.incrementAndGet() <= connectionPool.getConfig().getMaxStatements()) {
                long invokeStart = System.nanoTime();
                Statement stmt = (Statement) method.invoke(real_connection, args);
                if (connectionPool.getConfig().getQueryTimeout() > 0) {
                    stmt.setQueryTimeout(connectionPool.getConfig().getQueryTimeout());
                }
                pstmt = new PooledStatement(this, stmt, statementNo.getAndIncrement());
                if (isVerbose() && log.isInfoEnabled()) {
                    log.info(Arrays.stream(args == null ? new Object[] {} : args).map(String::valueOf)
                            .collect(Collectors.joining(";", connectionName + " * createStatement(",
                            ")[" + validStatementNum.get() + "], use " + Formatter.formatNS(System.nanoTime() - invokeStart) + " ns"
                    )));
                }
            } else {
                validStatementNum.decrementAndGet();
            }
        }
        if (pstmt == null) {
            throw new SQLException("statements of " + connectionName + " exceed max value[" + connectionPool.getConfig().getMaxStatements() + "]", "60000");
        }
        Statement stmt = pstmt.checkOut();
        activeStatementsPool.put(pstmt.getStatementId(), pstmt);
        return stmt;
    }

    public PreparedStatement prepareStatement(Method method, Object[] args) throws Throwable {
        PooledPreparedStatement ppstmt = null;
        synchronized (validPreStatementsPool) {
            if (args.length == 1) {
                //没有额外参数的prepareStatement才试图从池中获取取
                ppstmt = validPreStatementsPool.get(args[0]);
//            } else {
//                return (PreparedStatement) method.invoke(real_connection, args);
            }
            if (ppstmt == null) {
                long invokeStart = System.nanoTime();
                PreparedStatement pstmt = (PreparedStatement) method.invoke(real_connection, args);
                if (connectionPool.getConfig().getQueryTimeout() > 0) {
                    pstmt.setQueryTimeout(connectionPool.getConfig().getQueryTimeout());
                }
                ppstmt = getPooledPreparedStatement(pstmt, statementNo.getAndIncrement(), args);
                if (ppstmt.isDefaultResultSetType()) {
                    validPreStatementsPool.put((String) args[0], ppstmt);
                }
                if (isVerbose() && log.isInfoEnabled()) {
                    log.info(Arrays.stream(args).skip(1).map(arg -> {
                        if (arg.getClass().isArray()) {
                            return ArrayUtils.toString(arg);
                        } else {
                            return String.valueOf(arg);
                        }
                    }).collect(Collectors.joining(";",
                            connectionName + " * prepareStatement(" + ppstmt.getPreparedSql() + (args.length > 1 ? ";" : ""),
                            ")[" + validPreStatementsPool.size() + "], use " + Formatter.formatNS(System.nanoTime() - invokeStart) + " ns"
                    )));
                }
            }
        }
        PreparedStatement pstmt = (PreparedStatement) ppstmt.checkOut();
        activeStatementsPool.put(ppstmt.getStatementId(), ppstmt);
        return pstmt;
    }

    /**
     * Generates PooledPreparedStatement based on the connection type.
     */
    protected PooledPreparedStatement getPooledPreparedStatement(PreparedStatement stmt, int stmtId, Object[] args) throws SQLException {
        return new PooledPreparedStatement(this, stmt, stmtId, args);
    }

    public CallableStatement prepareCall(Method method, Object[] args) throws Throwable {
        PooledCallableStatement pcstmt = null;
        synchronized (validPreStatementsPool) {
            if (args.length == 1) {
                pcstmt = (PooledCallableStatement) validPreStatementsPool.get(args[0]);
//            } else {
//                return (CallableStatement) method.invoke(real_connection, args);
            }
            if (pcstmt == null) {
                long invokeStart = System.nanoTime();
                CallableStatement cstmt = (CallableStatement) method.invoke(real_connection, args);
                if (connectionPool.getConfig().getQueryTimeout() > 0) {
                    cstmt.setQueryTimeout(connectionPool.getConfig().getQueryTimeout());
                }
                pcstmt = new PooledCallableStatement(this, cstmt, statementNo.getAndIncrement(), args);
                if (pcstmt.isDefaultResultSetType()) {
                    validPreStatementsPool.put((String) args[0], pcstmt);
                }
                if (isVerbose() && log.isInfoEnabled()) {
                    log.info(Arrays.stream(args).skip(1).map(String::valueOf).collect(Collectors.joining(";",
                            connectionName + " * prepareCall(" + pcstmt.getPreparedSql() + (args.length > 1 ? "," : ""),
                            ")[" + validPreStatementsPool.size() + "], use " + Formatter.formatNS(System.nanoTime() - invokeStart) + " ns"
                    )));
                }
            }
        }
        CallableStatement pstmt = (CallableStatement) pcstmt.checkOut();
        activeStatementsPool.put(pcstmt.getStatementId(), pcstmt);
        return pstmt;
    }

    /**
     * 回收statement
     * @param pstmt
     */
    public void checkIn(PooledStatement pstmt) {
        activeStatementsPool.remove(pstmt.getStatementId());
        if (pstmt.isDefaultResultSetType()) {
            if (! pstmt.isClosed()) {
                idleStatementsPool.offer(pstmt);
            }
        } else {
            pstmt.close();
            validStatementNum.decrementAndGet();
        }
    }

    /**
     * 回收PreparedStatement，与回收statement分开调用
     * @param ppstmt
     */
    public void checkIn(PooledPreparedStatement ppstmt) {
        activeStatementsPool.remove(ppstmt.getStatementId());
        if (ppstmt.isDefaultResultSetType()) {
            ppstmt.cleanCache();
        } else {
            ppstmt.close();
        }
    }

    public long getCheckOutTime() {
        if (checkOut.get()) {
            return System.currentTimeMillis() - timeCheckOut;
        }
        return 0L;
    }

    public boolean isBusying() {
        boolean b = false;
        for (Map.Entry<Integer, PooledStatement> e : activeStatementsPool.entrySet()) {
            if (e.getValue().isBusying()) {
                b = true;
            }
        }
        return b;
    }

    public void doCheck() throws Exception {
        Statement stmt = null;
        ResultSet rs = null;
        String checkStmt = connectionPool.getConfig().getCheckStatement();
        if (checkStmt == null) {
            log.info("check statement is NULL, skip connection check...");
            return;
        }
        try {
            if (closed.get()) {
                makeRealConnection();
            }
            stmt = connection.createStatement();
//            int to = stmt.getQueryTimeout();
//            stmt.setQueryTimeout((int) connectionPool.getConfig().getIdleTimeoutSec());
            rs = stmt.executeQuery(checkStmt);
            rs.next();
//            stmt.setQueryTimeout(to);
        } finally {
            // modify by shenjl 修改资源泄露问题
            JdbcUtil.closeQuietly(rs);
            JdbcUtil.closeQuietly(stmt);
        }
    }

    private void commitOnClose() throws SQLException {
        if (connectionPool.getConfig().isCommitOnClose()) {
            real_connection.commit();
            log.warn(connectionName, ".commit() on close");
        } else {
            real_connection.rollback();
            log.warn(connectionName, ".rollback() on close");
        }
    }

    public void close() {
        if (closed.getAndSet(true)) {
            return;
        }
        validStatementNum.set(0);
        idleStatementsPool.clear();
        activeStatementsPool.clear();
        validPreStatementsPool.clear();
        if (! autoCommit && dirty) {
            try {
                commitOnClose();
            } catch (SQLException e) {
                log.error(connectionName, ".commitOnClose() error: ", e);
            }
            dirty = false;
        }
        try {
            try {
                real_connection.close();
            } catch (SQLException e) {
                //add by wuhq 2010.11.10
                log.info("close real_connection[", connectionName, "] error: ", e);
                try {
                    real_connection.rollback();
                } catch (SQLException ignr) {}
                real_connection.close();
            }
            log.info(connectionName, " real closed.");
        } catch (SQLException e) {
            log.warn(connectionName, " real_connection close error: ", e);
            connectionPool.offerUnclosedConnection(real_connection, connectionName);
        }
    }

     void unregisterJMX() {
         try {
             if (connectionPool.getConfig().getJmxLevel() > 1) {
                 JMXUtil.unregister(this.getClass().getPackage().getName() + ":type=pool-" + connectionPool.getPoolName() + ",name=" + getConnectionName());
             }
         } catch (Exception ignore) {}
     }

    /**
     * Return connectionId.
     * @return connectionId
     */
    public int getConnectionId() {
        return connectionId;
    }

    /**
     * Return connectionPool.
     * @return connectionPool
     */
    public Hqcp getConnectionPool() {
        return connectionPool;
    }

    /**
     * Return connectionName.
     * @return connectionName
     */
    public String getConnectionName() {
        return connectionName;
    }

    /**
     * Return checkOut.
     * @return checkOut
     */
    public boolean isCheckOut() {
        return checkOut.get();
    }

    public boolean isVerbose() {
        return connectionPool.getConfig().isVerbose();
    }

    public boolean isPrintSQL() {
        return connectionPool.getConfig().isPrintSql();
    }

    /**
     * Return timeCheckIn.
     * @return timeCheckIn
     */
    public long getTimeCheckIn() {
        return timeCheckIn;
    }

    /**
     * Return connection connected time.
     * @return
     */
    public long getTimeConnected() {
        return timeConnected;
    }

    /**
     * Return threadCheckOut.
     * @return threadCheckOut
     */
    public Thread getThreadCheckOut() {
        return threadCheckOut;
    }

    /**
     * the dirty to set
     */
    public void setDirty() {
        this.dirty = true;
    }

    public boolean isClosed() {
        return closed.get();
    }

    public int getCachedStatementsCount() {
        return this.validStatementNum.get();
    }

    public int getCachedPreStatementsCount() {
        return this.validPreStatementsPool.size();
    }

    public String[] getCachedPreStatementsSQLs() {
        return validPreStatementsPool.keySet().toArray(new String[validPreStatementsPool.size()]);
    }

    public String getCheckOutThreadName() {
        if (isCheckOut()) {
            return getThreadCheckOut().getName();
        }
        return "";
    }

}
