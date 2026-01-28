package io.hqwu.commons.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test cases for ClassHelper
 */
public class ClassHelperTest {

    @Test
    public void testForNameWithThreadContextClassLoader() throws Exception {
        // Test loading a standard class
        Class<?> clazz = ClassHelper.forNameWithThreadContextClassLoader("java.lang.String");
        assertEquals(String.class, clazz);

        // Test loading primitive type
        Class<?> intClass = ClassHelper.forNameWithThreadContextClassLoader("int");
        assertEquals(int.class, intClass);

        // Test loading array type with [] suffix
        Class<?> stringArrayClass = ClassHelper.forNameWithThreadContextClassLoader("java.lang.String[]");
        assertEquals(String[].class, stringArrayClass);

        // Test class not found
        assertThrows(ClassNotFoundException.class, () -> {
            ClassHelper.forNameWithThreadContextClassLoader("com.nonexistent.ClassName");
        });
    }

    @Test
    public void testForNameWithCallerClassLoader() throws Exception {
        // Test loading a standard class
        Class<?> clazz = ClassHelper.forNameWithCallerClassLoader("java.lang.String", ClassHelperTest.class);
        assertEquals(String.class, clazz);

        // Test loading primitive type
        Class<?> booleanClass = ClassHelper.forNameWithCallerClassLoader("boolean", ClassHelperTest.class);
        assertEquals(boolean.class, booleanClass);

        // Test loading array type
        Class<?> intArrayClass = ClassHelper.forNameWithCallerClassLoader("int[]", ClassHelperTest.class);
        assertEquals(int[].class, intArrayClass);
    }

    @Test
    public void testGetCallerClassLoader() {
        ClassLoader cl = ClassHelper.getCallerClassLoader(ClassHelperTest.class);
        assertNotNull(cl);
    }

    @Test
    public void testGetClassLoaderWithClass() {
        ClassLoader cl = ClassHelper.getClassLoader(ClassHelperTest.class);
        assertNotNull(cl);
    }

    @Test
    public void testGetClassLoader() {
        ClassLoader cl = ClassHelper.getClassLoader();
        assertNotNull(cl);
    }

    @Test
    public void testForNameWithDefaultClassLoader() throws Exception {
        // Test loading a standard class
        Class<?> clazz = ClassHelper.forName("java.lang.String");
        assertEquals(String.class, clazz);

        // Test loading primitive type
        Class<?> longClass = ClassHelper.forName("long");
        assertEquals(long.class, longClass);
    }

    @Test
    public void testForNameWithPrimitiveTypes() throws Exception {
        // Test all primitive types
        assertEquals(boolean.class, ClassHelper.forName("boolean", null));
        assertEquals(byte.class, ClassHelper.forName("byte", null));
        assertEquals(char.class, ClassHelper.forName("char", null));
        assertEquals(double.class, ClassHelper.forName("double", null));
        assertEquals(float.class, ClassHelper.forName("float", null));
        assertEquals(int.class, ClassHelper.forName("int", null));
        assertEquals(long.class, ClassHelper.forName("long", null));
        assertEquals(short.class, ClassHelper.forName("short", null));
    }

    @Test
    public void testForNameWithPrimitiveArrays() throws Exception {
        // Test primitive array types
        assertEquals(boolean[].class, ClassHelper.forName("boolean[]", null));
        assertEquals(byte[].class, ClassHelper.forName("byte[]", null));
        assertEquals(char[].class, ClassHelper.forName("char[]", null));
        assertEquals(double[].class, ClassHelper.forName("double[]", null));
        assertEquals(float[].class, ClassHelper.forName("float[]", null));
        assertEquals(int[].class, ClassHelper.forName("int[]", null));
        assertEquals(long[].class, ClassHelper.forName("long[]", null));
        assertEquals(short[].class, ClassHelper.forName("short[]", null));
    }

    @Test
    public void testForNameWithObjectArrays() throws Exception {
        // Test object array with [] suffix
        Class<?> stringArrayClass = ClassHelper.forName("java.lang.String[]", null);
        assertEquals(String[].class, stringArrayClass);

        // Test nested arrays
        Class<?> nestedArrayClass = ClassHelper.forName("java.lang.String[][]", null);
        assertEquals(String[][].class, nestedArrayClass);
    }

    @Test
    public void testForNameWithInternalArrayNotation() throws Exception {
        // Test internal array notation "[Ljava.lang.String;"
        Class<?> stringArrayClass = ClassHelper.forName("[Ljava.lang.String;", null);
        assertEquals(String[].class, stringArrayClass);

        // Test internal array notation for primitive types
        Class<?> intArrayClass = ClassHelper.forName("[I", null);
        assertEquals(int[].class, intArrayClass);

        // Test 2D array with internal notation
        Class<?> twoDArrayClass = ClassHelper.forName("[[Ljava.lang.String;", null);
        assertEquals(String[][].class, twoDArrayClass);
    }

    @Test
    public void testForNameWithInternalArrayNotationAtPosition() throws Exception {
        // Test when INTERNAL_ARRAY_PREFIX is at position 0
        Class<?> arrayClass = ClassHelper.forName("[Ljava.util.ArrayList;", null);
        assertEquals(ArrayList[].class, arrayClass);
    }

    @Test
    public void testForNameWithNullClassLoader() throws Exception {
        // Test that null classLoader falls back to default
        Class<?> clazz = ClassHelper.forName("java.lang.String", null);
        assertEquals(String.class, clazz);

        Class<?> hashMapClass = ClassHelper.forName("java.util.HashMap", null);
        assertEquals(HashMap.class, hashMapClass);
    }

    @Test
    public void testForNameWithNonExistentClass() {
        // Test class not found exception
        assertThrows(ClassNotFoundException.class, () -> {
            ClassHelper.forName("com.nonexistent.FakeClass", null);
        });
    }

    @Test
    public void testResolvePrimitiveClassName() {
        // Test primitive types
        assertEquals(boolean.class, ClassHelper.resolvePrimitiveClassName("boolean"));
        assertEquals(byte.class, ClassHelper.resolvePrimitiveClassName("byte"));
        assertEquals(char.class, ClassHelper.resolvePrimitiveClassName("char"));
        assertEquals(double.class, ClassHelper.resolvePrimitiveClassName("double"));
        assertEquals(float.class, ClassHelper.resolvePrimitiveClassName("float"));
        assertEquals(int.class, ClassHelper.resolvePrimitiveClassName("int"));
        assertEquals(long.class, ClassHelper.resolvePrimitiveClassName("long"));
        assertEquals(short.class, ClassHelper.resolvePrimitiveClassName("short"));
    }

    @Test
    public void testResolvePrimitiveClassNameWithArrays() {
        // Test primitive array types using JVM internal notation (what getName() returns)
        // For primitive arrays, getName() returns the JVM internal notation like "[B" for byte[]
        assertEquals(boolean[].class, ClassHelper.resolvePrimitiveClassName("[Z"));
        assertEquals(byte[].class, ClassHelper.resolvePrimitiveClassName("[B"));
        assertEquals(char[].class, ClassHelper.resolvePrimitiveClassName("[C"));
        assertEquals(double[].class, ClassHelper.resolvePrimitiveClassName("[D"));
        assertEquals(float[].class, ClassHelper.resolvePrimitiveClassName("[F"));
        assertEquals(int[].class, ClassHelper.resolvePrimitiveClassName("[I"));
        assertEquals(long[].class, ClassHelper.resolvePrimitiveClassName("[J"));
        assertEquals(short[].class, ClassHelper.resolvePrimitiveClassName("[S"));

        // The "[]" suffix notation is not in the map, so these return null
        assertNull(ClassHelper.resolvePrimitiveClassName("byte[]"));
        assertNull(ClassHelper.resolvePrimitiveClassName("int[]"));
    }

    @Test
    public void testResolvePrimitiveClassNameWithNull() {
        // Test null input
        assertNull(ClassHelper.resolvePrimitiveClassName(null));
    }

    @Test
    public void testResolvePrimitiveClassNameWithNonPrimitive() {
        // Test non-primitive class name (length check should prevent lookup)
        assertNull(ClassHelper.resolvePrimitiveClassName("java.lang.String"));
        assertNull(ClassHelper.resolvePrimitiveClassName("verylongclassname"));
    }

    @Test
    public void testResolvePrimitiveClassNameWithShortNonPrimitive() {
        // Test short name that is not a primitive
        assertNull(ClassHelper.resolvePrimitiveClassName("abc"));
        assertNull(ClassHelper.resolvePrimitiveClassName("x"));
    }

    @Test
    public void testResolvePrimitiveClassNameEdgeCases() {
        // Test edge case: exactly 8 characters but not primitive
        assertNull(ClassHelper.resolvePrimitiveClassName("notprim"));

        // Test edge case: exactly 9 characters (too long)
        assertNull(ClassHelper.resolvePrimitiveClassName("toolong12"));
    }

    @Test
    public void testToShortString() {
        // Test with null
        assertEquals("null", ClassHelper.toShortString(null));

        // Test with a string object
        String str = "test";
        String result = ClassHelper.toShortString(str);
        assertTrue(result.startsWith("String@"));
        assertTrue(result.contains("@"));

        // Test with an integer object
        Integer num = 42;
        result = ClassHelper.toShortString(num);
        assertTrue(result.startsWith("Integer@"));

        // Test with a custom object
        ClassHelperTest test = new ClassHelperTest();
        result = ClassHelper.toShortString(test);
        assertTrue(result.startsWith("ClassHelperTest@"));

        // Test with ArrayList
        ArrayList<String> list = new ArrayList<>();
        result = ClassHelper.toShortString(list);
        assertTrue(result.startsWith("ArrayList@"));
    }

    @Test
    public void testToShortStringWithDifferentObjects() {
        // Test that different objects have different identity hash codes
        Object obj1 = new Object();
        Object obj2 = new Object();

        String str1 = ClassHelper.toShortString(obj1);
        String str2 = ClassHelper.toShortString(obj2);

        assertTrue(str1.startsWith("Object@"));
        assertTrue(str2.startsWith("Object@"));
        assertNotEquals(str1, str2); // Different objects should have different identity hash codes
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "boolean", "byte", "char", "short",
            "int", "long", "float", "double"
    })
    public void testAllPrimitiveTypesParameterized(String primitiveName) throws Exception {
        // Verify all primitive types can be loaded
        Class<?> clazz = ClassHelper.forName(primitiveName, null);
        assertNotNull(clazz);
        assertTrue(clazz.isPrimitive());
    }

    @Test
    public void testForNameWithPrimitiveArrayInternalNotation() throws Exception {
        // Test all primitive array types with internal notation
        assertEquals(boolean[].class, ClassHelper.forName("[Z", null));
        assertEquals(byte[].class, ClassHelper.forName("[B", null));
        assertEquals(char[].class, ClassHelper.forName("[C", null));
        assertEquals(double[].class, ClassHelper.forName("[D", null));
        assertEquals(float[].class, ClassHelper.forName("[F", null));
        assertEquals(int[].class, ClassHelper.forName("[I", null));
        assertEquals(long[].class, ClassHelper.forName("[J", null));
        assertEquals(short[].class, ClassHelper.forName("[S", null));
    }

    @Test
    public void testForNameWithComplexArrayTypes() throws Exception {
        // Test multi-dimensional arrays
        Class<?> threeDArray = ClassHelper.forName("java.lang.String[][][]", null);
        assertEquals(String[][][].class, threeDArray);

        // Test array of arrays with recursive forName calls ([][] suffix notation)
        Class<?> intTwoDArray = ClassHelper.forName("int[][]", null);
        assertEquals(int[][].class, intTwoDArray);

        // Test 2D object array with internal notation
        Class<?> stringTwoDArray = ClassHelper.forName("[[Ljava.lang.String;", null);
        assertEquals(String[][].class, stringTwoDArray);
    }

    @Test
    public void testForNameBranchCoverage() throws Exception {
        // Test the branch where name ends with ARRAY_SUFFIX
        Class<?> arrayClass = ClassHelper.forName("java.util.HashMap[]", null);
        assertTrue(arrayClass.isArray());
        assertEquals(HashMap.class, arrayClass.getComponentType());

        // Test the branch where internalArrayMarker is at position 0
        Class<?> internalArray = ClassHelper.forName("[Ljava.util.HashMap;", null);
        assertTrue(internalArray.isArray());
        assertEquals(HashMap.class, internalArray.getComponentType());

        // Test the branch where name starts with "[" but internalArrayMarker is not at 0
        // This covers the "else if (name.startsWith("["))" branch
        Class<?> primitiveArray = ClassHelper.forName("[I", null);
        assertTrue(primitiveArray.isArray());
        assertEquals(int.class, primitiveArray.getComponentType());
    }

    @Test
    public void testForNameNotEndingWithSemicolon() throws Exception {
        // Test a case where the name contains "[L" but doesn't end with ";"
        // This should fall through to the regular classLoader.loadClass
        // Since this is not a valid internal array notation, it should throw ClassNotFoundException
        assertThrows(ClassNotFoundException.class, () -> {
            ClassHelper.forName("[Ljava.lang.String", null);
        });
    }

    @Test
    public void testForNameWithEmptyArrayNotation() {
        // Test edge case with just "[]"
        assertThrows(ClassNotFoundException.class, () -> {
            ClassHelper.forName("[]", null);
        });
    }

    @Test
    public void testConstantValues() {
        // Test the constant values are as expected
        assertEquals("[]", ClassHelper.ARRAY_SUFFIX);
    }
}
