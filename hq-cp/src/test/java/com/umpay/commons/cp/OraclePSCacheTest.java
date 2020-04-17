package com.umpay.commons.cp;

import com.umpay.commons.cp.util.OracleUtil;
import oracle.jdbc.OracleConnection;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * Created by jianbin on 2/14/14.
 */
public class OraclePSCacheTest {

        public final static int stmntCount = 300;

        public final static void main(String[] args) throws Exception {

            System.in.read();
            System.out.println("......");

            test();

        }

        public final static void test() throws Exception {

            System.out.println("--------- start -------");
            System.in.read();

            Class.forName("oracle.jdbc.driver.OracleDriver");
            String jdbcUrl = "jdbc:oracle:thin:@192.168.201.41:1521:payment";
            String user = "b2cbill";
            String password = "b2cbill";

            Connection conn = DriverManager.getConnection(jdbcUrl, user, password);

            OracleConnection oracleConn = (OracleConnection) conn;
            oracleConn.setImplicitCachingEnabled(false);
            oracleConn.setStatementCacheSize(250);

            printAndWait("initialization finished. Waiting for test 1 starting");

            for (int i = 0; i < stmntCount; i++) {
                PreparedStatement ps = oracleConn.prepareStatement("select a.*, " + i + " from B2C_BANKSHOP a where rownum < ?");
                ps.setInt(1, 20);
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {}
                rs.close();
                ps.close();
            }

            printAndWait("test 1 finished. waiting for the 2nd");

            oracleConn.setImplicitCachingEnabled(true);

            for (int i = 0; i < stmntCount; i++) {
                PreparedStatement ps = oracleConn.prepareStatement("select a.*, " + i + " from B2C_BANKSHOP a where rownum < ?");
                ps.setInt(1, 20);
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {}
                rs.close();
                ps.close();
            }

            printAndWait("test 2 finished. waiting for the 3rd");

            PreparedStatement[] PSs = new PreparedStatement[stmntCount];
            for (int i = 0; i < stmntCount; i++) {
                PSs[i] = oracleConn.prepareStatement("select a.*, " + i + " from B2C_BANKSHOP a where rownum < ?");
                PSs[i].setInt(1, 20);
                ResultSet rs = PSs[i].executeQuery();
                while (rs.next()) {}
                rs.close();
            }

            printAndWait("query finished, waiting to call oracle driver");

            for (int i = 0; i < stmntCount; i++)
                OracleUtil.enterImplicitCache(PSs[i]);

            printAndWait("enterImplicitCache finished. Check the memory");

            for (int i = 0; i < stmntCount; i++)
                OracleUtil.exitImplicitCacheToActive(PSs[i]);

            printAndWait("exitImplicitCacheToActive finished. Check the memory");

            for (int i = 0; i < stmntCount; i++)
                OracleUtil.exitImplicitCacheToClose(PSs[i]);

            printAndWait("exitImplicitCacheToClose finished. 3rd test finished. waiting for the 4th");

            for (int i = 0; i < stmntCount; i++) {
                PreparedStatement ps = oracleConn.prepareStatement("select a.*, " + i + " from B2C_BANKSHOP a where rownum < ?");
                ps.setInt(1, 20);
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {}
                rs.close();
                ps.close();
            }

            printAndWait("4th test finished. Check the memory");

            oracleConn.close();

        }

        private static void printAndWait(String info) throws IOException {
            System.out.println("-------- " + info + " --------");
            System.in.read();
        }

}
