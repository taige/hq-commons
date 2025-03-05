package io.hqwu.commons.util;

import org.junit.jupiter.api.Test;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Created with IntelliJ IDEA.
 * User: wyshenjianlin
 * Date: 13-9-11
 * Time: 下午3:35
 */
public class ClassUtilTest {
    @Test
    public void testGetPackageAsPath() throws Exception {
        String actual = ClassUtil.getPackageAsPath(null);
        assertEquals("", actual);

        actual = ClassUtil.getPackageAsPath(ClassUtilTest.class);
        assertEquals("io/hqwu/commons/util", actual);

        actual = ClassUtil.getPackageAsPath(ClassUtilTest.class, "test.xml");
        assertEquals("io/hqwu/commons/util/test.xml", actual);
        
        actual = ClassUtil.getPackageAsPath(ClassUtilTest.class, null);
        assertEquals("io/hqwu/commons/util", actual);
    }

    @Test
    public void testIsSdkPackage() throws Exception {
        assertTrue(ClassUtil.isSdkPackage(String.class));
        assertTrue(ClassUtil.isSdkPackage(HashMap.class));
        assertFalse(ClassUtil.isSdkPackage(ClassUtilTest.class));
        assertTrue(ClassUtil.isSdkPackage(long.class));

    }
}
