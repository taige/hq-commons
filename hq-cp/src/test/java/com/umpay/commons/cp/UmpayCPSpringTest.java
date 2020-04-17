package com.umpay.commons.cp;

import com.jolbox.bonecp.MockJDBCDriver;
import com.umpay.commons.util.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.sql.SQLException;


/**
 * Created with IntelliJ IDEA for plat-arch-svn
 * User: taige
 * Date: 13-10-31
 * Time: 上午10:18
 */
public class UmpayCPSpringTest {
    private static final Logger LOGGER = new Logger();

    private static ClassPathXmlApplicationContext ctx = null;

    @BeforeAll //表示在所以测试方法之前执行，且只执行一次。
    public static void onlyOnce() throws SQLException {
        new MockJDBCDriver("jdbc:mysql");
        ctx = new ClassPathXmlApplicationContext("spring-jdbc.xml");
    }

//    @AfterClass
//    public static void shutdown() {
//        ctx.close();
//    }

    @Test
    public void test(){
        TestDao testDao=ctx.getBean("testDao", TestDao.class);
        testDao.execute("select * from aa");
    }

    @Test
    @Disabled
    public void test2() throws InterruptedException {
        try {
            TestDao testDao = ctx.getBean("testDao", TestDao.class);
            testDao.execute(null);
        } catch (Exception e) {
            LOGGER.warn(e);
        }

        TestDao testDao = ctx.getBean("testDao", TestDao.class);
        testDao.execute("select * from aa");

        Thread[] t = new Thread[2];

        for (int i = 0; i < t.length; i++) {
            t[i] = new Thread() {
                public void run() {
                    TestDao testDao = ctx.getBean("testDao", TestDao.class);
                    testDao.execute(null);
                }
            };
            t[i].start();
        }
        for (int i = 0; i < t.length; i++) {
            t[i].join();
        }
//        for (int i = 0; i < 2; i++) {
//            new Thread() {
//                public void run() {
//                    //for (int i = 0; i < 100; i++) {
//                        TestDao testDao = ctx.getBean("testDao", TestDao.class);
//                        testDao.execute("select * from a");
////                    }
//                }
//            }.start();
//        }
        LOGGER.debug("sleep 10000ms");
        Thread.sleep(10000);
        LOGGER.info("ok? -- 10000");
    }

    @Test
    @Disabled
    public void testReal() throws Exception {
        {
            TestDao testDao = ctx.getBean("testDao", TestDao.class);
            for (int i = 0; i < 1; i++) {
                try {
//                testDao.execute("SELECT COUNT(1) FROM trade_base WHERE card_pan = 'dc477765b52d2473d9e8b4ba9e41b318' and trade_date ='2015-01-29' and trade_type_code='03' and sub_trade_type_code='01' and payment_code = '02' and seller_merchant_no IN ('22792279', '22809981', '22932972', '22933075', '22948700', '22948717')");
                    testDao.execute("SELECT NOW()");
                } catch (Exception e) {
                    LOGGER.error(e);
                }
            }
        }
        Thread.sleep(3000);

        {
            TestDao2 testDao2 = ctx.getBean("testDao2", TestDao2.class);
            for (int i = 0; i < 1; i++) {
                try {
//                testDao.execute("SELECT COUNT(1) FROM trade_base WHERE card_pan = 'dc477765b52d2473d9e8b4ba9e41b318' and trade_date ='2015-01-29' and trade_type_code='03' and sub_trade_type_code='01' and payment_code = '02' and seller_merchant_no IN ('22792279', '22809981', '22932972', '22933075', '22948700', '22948717')");
                    testDao2.execute("SELECT NOW()");
                } catch (Exception e) {
                    LOGGER.error(e);
                }
            }
        }

        LOGGER.debug("sleep 10001ms");

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(100000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                LOGGER.debug("quit ....");
            }
        }).start();

        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                LOGGER.debug("hook quit ....");

            }
        }));


        Thread.sleep(1000001);
        LOGGER.info("ok? -- 10001");
    }
}
