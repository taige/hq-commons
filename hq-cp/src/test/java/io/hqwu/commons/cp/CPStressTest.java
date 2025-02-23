package io.hqwu.commons.cp;


import com.jolbox.bonecp.BoneCP;
import com.jolbox.bonecp.BoneCPConfig;
import com.jolbox.bonecp.MockJDBCDriver;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import com.mchange.v2.c3p0.DataSources;
import com.umpay.commons.util.Logger;

import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

public class CPStressTest {
    private static final Logger LOGGER = new Logger();

    private static AtomicInteger loopCount;
    private static CountDownLatch startFlag;
    private static CountDownLatch doneFlag;

    private static String jdbcUrl = "jdbc:mock:";
    private static String username = "test";
    private static String passwd = "testPwd";
    private static int minConn = 1;
    private static int maxConn = 50;

    private static boolean nothing = false;
    private static boolean simple = false;

    /**
     * @param args
     */
    public static void main(String[] args) throws Exception {
        MockJDBCDriver driver = MockJDBCDriver.getInstance();

        if (new File("loop_nothing").exists()) {
            nothing = true;
        } else if (new File("simple_test").exists()) {
            simple = true;
        }

        String test = "bonecp"; //args[0];
        int workerNum = 200; //Integer.parseInt(args[1]);
        int count = 1000000; //Integer.parseInt(args[2]);

        LOGGER.info("start test: " + test);
        loopCount = new AtomicInteger(count);
        startFlag = new CountDownLatch(workerNum+1);
        doneFlag = new CountDownLatch(workerNum);
        Runner r = null;
        for (int i = 1; i <= workerNum; i++) {
            if (test.equals("umpaycf")) {
                r = new Runner(i);
            } else if (test.equals("bonecp")) {
                r = new Runner_bonecp(i);
            } else if (test.equals("c3p0")) {
                r = new Runner_c3p0(i);
            } else{
                System.err.println("unknow conn pool: " + test);
                return;
            }
            r.start();
        }
        long start = System.currentTimeMillis();
        startFlag.countDown();
        doneFlag.await();
        r.shutdown();
        long end = System.currentTimeMillis() - start;
        LOGGER.info("done test: "+test+"(loop:"+count+"/worker:"+workerNum+") use " + end + " ms");
    }

    private static class Runner extends Thread {
        protected int idx;
        protected int cc = 0;
        static Hqcp fc;
        public Runner() {}
        public Runner(int idx) throws SQLException {
            this.idx = idx;
            if (fc == null) {
                HqcpConfig config = new HqcpConfig();
                config.setUrl(jdbcUrl);
                config.setUsername(username);
                config.setPassword(passwd);
                config.setMinConnections(minConn);
                config.setMaxConnections(maxConn);
                config.setVerbose(false);
                config.setPrintSql(false);
                fc = new Hqcp(config);
            }
        }
        public void run() {
            startFlag.countDown();
            try {
                startFlag.await();
            } catch (InterruptedException e1) {
            }
            try {
                LOGGER.info("start runner #" + idx);
                for (; loopCount.decrementAndGet() >= 0 ; ) {
                    //for (int i = 0; i < count.get() ; i++) {
                    loopit();
                    cc++;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            LOGGER.info("#" + idx + ": cc = " + cc);
            doneFlag.countDown();
        }

        protected void loopit() throws Exception {
            Connection conn = fc.getConnection(true);
            if (conn != null) {
                doBusiness(conn);
                conn.close();
            } else {
                LOGGER.error("conn is null");
            }
        }

        protected void doBusiness(Connection conn) throws SQLException {
            if (nothing) {
                // do nothing
            } else if (simple) {
                doBusiness_simple(conn);
            } else {
                Statement stmt = conn.createStatement();
                String orderid = "1234#"+idx+"#"+cc;
                try {
                    stmt.executeUpdate("insert into umpay.t_gorder(merid, orderid, orderdate) values('1234','"+orderid+"','20120612')");
                    ResultSet rs = stmt.executeQuery("select * from umpay.t_gorder where orderid='"+orderid+"'");
                    if (rs.next()) {
                        stmt.executeUpdate("update umpay.t_gorder set amount=123 where orderid='"+orderid+"'");
                    }
                    rs.close();
                } catch (SQLException e) {
                    System.err.println(orderid + "-----" + e.getMessage());
                    //e.printStackTrace();
                } finally {
                    stmt.close();
                }
            }
        }
        protected void doBusiness_simple(Connection conn) throws SQLException {
            Statement stmt = conn.createStatement();
            String orderid = "12340001";
            try {
                ResultSet rs = stmt.executeQuery("select * from umpay.t_gorder where orderid='"+orderid+"'");
                while (rs.next()) {

                }
                rs.close();
            } catch (SQLException e) {
                System.err.println(orderid + "-----" + e.getMessage());
                //e.printStackTrace();
            } finally {
                stmt.close();
            }
        }

        protected void emptyTable(Connection conn) throws SQLException {
            if (nothing || simple) return;
            Statement stmt = conn.createStatement();
            stmt.executeUpdate("ALTER TABLE umpay.t_gorder ACTIVATE NOT LOGGED INITIALLY WITH EMPTY TABLE");
            stmt.close();
            conn.close();
        }

        protected void shutdown() throws SQLException {
            Connection conn = fc.getConnection(true);
            emptyTable(conn);
            ConnectionFactory.shutdown("jdbc");
            fc = null;
        }
    }

    private static class Runner_bonecp extends Runner {
        static BoneCP connectionPool;
        public Runner_bonecp(int idx) throws SQLException {
            super();
            super.idx = idx;
            if (connectionPool != null) {
                return;
            }
            BoneCPConfig config = new BoneCPConfig();
            config.setJdbcUrl(jdbcUrl);
            config.setUsername(username);
            config.setPassword(passwd);
            config.setMinConnectionsPerPartition(minConn);
            config.setMaxConnectionsPerPartition(maxConn);
            config.setPartitionCount(1);
            connectionPool = new BoneCP(config);
        }
        protected void loopit() throws Exception {
            Connection conn = connectionPool.getConnection(); // fetch a connection
            if (conn != null) {
                doBusiness(conn);
                conn.close();
            } else {
                LOGGER.error("conn2 is null");
            }
        }
        protected void shutdown() throws SQLException {
            Connection conn = connectionPool.getConnection();
            emptyTable(conn);
            connectionPool.shutdown();
            connectionPool = null;
        }
    }

    private static class Runner_c3p0 extends Runner {
        static ComboPooledDataSource cpds;
        public Runner_c3p0(int idx) throws Exception {
            super();
            super.idx = idx;
            if (cpds != null) {
                return;
            }
            cpds = new ComboPooledDataSource();
            //cpds.setDriverClass( "com.ibm.db2.jcc.DB2Driver" );
            cpds.setJdbcUrl(jdbcUrl);
            cpds.setUser(username);
            cpds.setPassword(passwd);
            cpds.setAcquireIncrement(1);
            cpds.setMinPoolSize(minConn);
            cpds.setMaxPoolSize(maxConn);
        }
        protected void loopit() throws Exception {
            Connection conn = cpds.getConnection(); // fetch a connection
            if (conn != null) {
                doBusiness(conn);
                conn.close();
            } else {
                LOGGER.error("conn3 is null");
            }
        }
        protected void shutdown() throws SQLException {
            Connection conn = cpds.getConnection();
            emptyTable(conn);
            DataSources.destroy(Runner_c3p0.cpds);
            cpds = null;
        }
    }
}
