package io.hqwu.commons.util;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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

    @Test
    public void test_getAllFields() {
        // 测试 getAllFields 方法
        List<Field> fields = new ArrayList<>();

        // 测试子类获取所有字段（包括继承的字段）
        ClassUtil.getAllFields(ChildClass.class, fields);

        // 验证包含子类和父类的字段
        assertTrue(fields.stream().anyMatch(f -> f.getName().equals("childField")));
        assertTrue(fields.stream().anyMatch(f -> f.getName().equals("parentField")));
        assertTrue(fields.stream().anyMatch(f -> f.getName().equals("grandparentField")));

        // 验证字段数量（3个字段）
        assertEquals(3, fields.size());
    }

    @Test
    public void test_getAllFields_withDuplicateFieldNames() {
        // 测试字段名重复的情况（子类覆盖父类字段）
        List<Field> fields = new ArrayList<>();

        ClassUtil.getAllFields(ChildWithOverride.class, fields);

        // 应该只包含一个 "name" 字段（子类的字段优先）
        long nameCount = fields.stream().filter(f -> f.getName().equals("name")).count();
        assertEquals(1, nameCount);

        // 验证是子类的字段
        Field nameField = fields.stream().filter(f -> f.getName().equals("name")).findFirst().orElse(null);
        assertNotNull(nameField);
        assertEquals(ChildWithOverride.class, nameField.getDeclaringClass());
    }

    @Test
    public void test_getAllFields_singleClass() {
        // 测试单个类（没有继承）
        List<Field> fields = new ArrayList<>();

        ClassUtil.getAllFields(SimpleClass.class, fields);

        // 验证字段
        assertEquals(2, fields.size());
        assertTrue(fields.stream().anyMatch(f -> f.getName().equals("field1")));
        assertTrue(fields.stream().anyMatch(f -> f.getName().equals("field2")));
    }

    // 测试用的类层次结构
    static class GrandparentClass {
        private String grandparentField;
    }

    static class ParentClass extends GrandparentClass {
        private String parentField;
    }

    static class ChildClass extends ParentClass {
        private String childField;
    }

    static class ParentWithName {
        protected String name;
    }

    static class ChildWithOverride extends ParentWithName {
        private String name; // 覆盖父类的字段
        private int age;
    }

    static class SimpleClass {
        private String field1;
        private int field2;
    }
}
