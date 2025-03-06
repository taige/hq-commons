package io.hqwu.commons.util;


import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 
 * @author wyliangxiaowu
 * @date   2013年10月28日 下午2:57:24
 */
public class JMXUtilTest {

    @Test
    public void test_1() {
        String objectName = this.getClass().getPackage().getName() + ":type=Hello";
        Hello mbean = new Hello();
        JMXUtil.register(objectName, mbean);
        JMXUtil.register(objectName, mbean);
    }
    
    @Test
    public void test_2() {
        String objectName = this.getClass().getPackage().getName() + ":type=Hello";
        try {
            JMXUtil.register(objectName, this);
        } catch (Exception e) {
            assertEquals("java.lang.IllegalArgumentException", e.getClass().getName());
        }
    }
    
    @Test
    public void test_3() {
        String objectName = this.getClass().getPackage().getName();
        try {
            JMXUtil.unregister(objectName);
        } catch (Exception e) {
            assertEquals("java.lang.IllegalArgumentException", e.getClass().getName());
        }
    }
    
    public static class Hello implements HelloMBean {
        private String message = "Hello World";
        @Override
        public String getMessage() {
            return this.message;
        }
           
        @Override
        public void sayHello() {
            System.out.println(message);
        }
           
        @Override
        public void setMessage(String message) {
            this.message = message;
        }
       
    }
   
    public static interface HelloMBean {
        public void sayHello();
        public String getMessage();
        public void setMessage(String message);
       
    }
}
