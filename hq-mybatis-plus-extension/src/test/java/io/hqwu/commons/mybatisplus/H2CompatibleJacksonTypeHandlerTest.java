package io.hqwu.commons.mybatisplus;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test cases for {@link H2CompatibleJacksonTypeHandler}
 *
 * <p>本测试只关注 H2CompatibleJacksonTypeHandler 自己的代码：
 * <ul>
 *   <li>构造函数：正确调用父类构造函数</li>
 *   <li>parse() 方法：处理被双引号包裹的 JSON 字符串</li>
 * </ul>
 *
 * @author Wu, Hongqiang
 * @since 2026/1/26
 */
public class H2CompatibleJacksonTypeHandlerTest {

    /**
     * 测试用 POJO 类
     */
    static class TestObject {
        private String name;
        private Integer age;

        public TestObject() {
        }

        public TestObject(String name, Integer age) {
            this.name = name;
            this.age = age;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Integer getAge() {
            return age;
        }

        public void setAge(Integer age) {
            this.age = age;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TestObject that = (TestObject) o;
            return (name != null ? name.equals(that.name) : that.name == null) &&
                   (age != null ? age.equals(that.age) : that.age == null);
        }

        @Override
        public int hashCode() {
            int result = name != null ? name.hashCode() : 0;
            result = 31 * result + (age != null ? age.hashCode() : 0);
            return result;
        }
    }

    // ========== 构造函数测试 ==========

    /**
     * 测试构造函数 - 正确传递类型给父类
     */
    @Test
    public void testConstructor() {
        // When
        H2CompatibleJacksonTypeHandler handler = new H2CompatibleJacksonTypeHandler(TestObject.class);

        // Then - 构造函数不抛出异常
        assertNotNull(handler);
    }

    /**
     * 测试构造函数 - 使用 Map 类型
     */
    @Test
    public void testConstructorWithMapType() {
        // When
        H2CompatibleJacksonTypeHandler handler = new H2CompatibleJacksonTypeHandler(Map.class);

        // Then
        assertNotNull(handler);
    }

    // ========== parse() 方法测试 - 被双引号包裹的 JSON ==========

    /**
     * 测试 parse() - 处理被双引号包裹的简单对象 JSON
     * 这是 H2CompatibleJacksonTypeHandler 的核心功能
     */
    @Test
    public void testParseWithQuotedJson() {
        // Given
        H2CompatibleJacksonTypeHandler handler = new H2CompatibleJacksonTypeHandler(TestObject.class);
        // H2 数据库可能返回被双引号包裹的 JSON
        String quotedJson = "\"{\\\"name\\\":\\\"Alice\\\",\\\"age\\\":25}\"";

        // When
        Object result = handler.parse(quotedJson);

        // Then
        assertNotNull(result);
        assertTrue(result instanceof TestObject);
        TestObject obj = (TestObject) result;
        assertEquals("Alice", obj.getName());
        assertEquals(25, obj.getAge());
    }

    /**
     * 测试 parse() - 处理被双引号包裹的复杂 JSON
     */
    @Test
    public void testParseWithQuotedComplexJson() {
        // Given
        H2CompatibleJacksonTypeHandler handler = new H2CompatibleJacksonTypeHandler(TestObject.class);
        // 包含特殊字符的 JSON
        String quotedJson = "\"{\\\"name\\\":\\\"Bob\\\\nSmith\\\",\\\"age\\\":30}\"";

        // When
        Object result = handler.parse(quotedJson);

        // Then
        assertNotNull(result);
        TestObject obj = (TestObject) result;
        assertEquals("Bob\nSmith", obj.getName());
        assertEquals(30, obj.getAge());
    }

    /**
     * 测试 parse() - 处理被双引号包裹的空对象 JSON
     */
    @Test
    public void testParseWithQuotedEmptyObjectJson() {
        // Given
        H2CompatibleJacksonTypeHandler handler = new H2CompatibleJacksonTypeHandler(TestObject.class);
        String quotedJson = "\"{}\"";

        // When
        Object result = handler.parse(quotedJson);

        // Then
        assertNotNull(result);
        assertTrue(result instanceof TestObject);
        TestObject obj = (TestObject) result;
        assertNull(obj.getName());
        assertNull(obj.getAge());
    }

    // ========== parse() 方法测试 - 未被双引号包裹的 JSON ==========

    /**
     * 测试 parse() - 处理未被双引号包裹的正常 JSON
     * 验证不会影响正常的 JSON 解析
     */
    @Test
    public void testParseWithNormalJson() {
        // Given
        H2CompatibleJacksonTypeHandler handler = new H2CompatibleJacksonTypeHandler(TestObject.class);
        // 正常的 JSON（未被双引号包裹）
        String normalJson = "{\"name\":\"Charlie\",\"age\":35}";

        // When
        Object result = handler.parse(normalJson);

        // Then
        assertNotNull(result);
        assertTrue(result instanceof TestObject);
        TestObject obj = (TestObject) result;
        assertEquals("Charlie", obj.getName());
        assertEquals(35, obj.getAge());
    }

    /**
     * 测试 parse() - 处理空 JSON 对象
     */
    @Test
    public void testParseWithEmptyObject() {
        // Given
        H2CompatibleJacksonTypeHandler handler = new H2CompatibleJacksonTypeHandler(TestObject.class);
        String emptyJson = "{}";

        // When
        Object result = handler.parse(emptyJson);

        // Then
        assertNotNull(result);
        assertTrue(result instanceof TestObject);
        TestObject obj = (TestObject) result;
        assertNull(obj.getName());
        assertNull(obj.getAge());
    }

    // ========== parse() 方法测试 - 边界情况 ==========

    /**
     * 测试 parse() - 只有双引号开头但没有结尾
     * 验证 startsWith 和 endsWith 都要满足才处理
     */
    @Test
    public void testParseWithOnlyStartQuote() {
        // Given
        H2CompatibleJacksonTypeHandler handler = new H2CompatibleJacksonTypeHandler(TestObject.class);
        // 只有开头的双引号（这不应该被特殊处理）
        String json = "\"{\"name\":\"David\",\"age\":40}";

        // When & Then - 这会导致解析失败，因为不是有效的 JSON
        assertThrows(Exception.class, () -> handler.parse(json));
    }

    // ========== parse() 方法测试 - 使用 Map 类型 ==========

    /**
     * 测试 parse() - 使用 Map 类型解析被引号包裹的 JSON
     */
    @Test
    public void testParseMapWithQuotedJson() {
        // Given
        H2CompatibleJacksonTypeHandler handler = new H2CompatibleJacksonTypeHandler(Map.class);
        String quotedJson = "\"{\\\"key1\\\":\\\"value1\\\",\\\"key2\\\":\\\"value2\\\"}\"";

        // When
        Object result = handler.parse(quotedJson);

        // Then
        assertNotNull(result);
        assertTrue(result instanceof Map);
        @SuppressWarnings("unchecked")
        Map<String, String> map = (Map<String, String>) result;
        assertEquals("value1", map.get("key1"));
        assertEquals("value2", map.get("key2"));
    }

    /**
     * 测试 parse() - 使用 Map 类型解析正常 JSON
     */
    @Test
    public void testParseMapWithNormalJson() {
        // Given
        H2CompatibleJacksonTypeHandler handler = new H2CompatibleJacksonTypeHandler(Map.class);
        String normalJson = "{\"key1\":\"value1\",\"key2\":\"value2\"}";

        // When
        Object result = handler.parse(normalJson);

        // Then
        assertNotNull(result);
        assertTrue(result instanceof Map);
        @SuppressWarnings("unchecked")
        Map<String, String> map = (Map<String, String>) result;
        assertEquals("value1", map.get("key1"));
        assertEquals("value2", map.get("key2"));
    }

    // ========== parse() 方法测试 - 特殊转义字符 ==========

    /**
     * 测试 parse() - 处理包含换行符的 JSON
     */
    @Test
    public void testParseWithNewlineCharacter() {
        // Given
        H2CompatibleJacksonTypeHandler handler = new H2CompatibleJacksonTypeHandler(TestObject.class);
        // 包含 \n 的 JSON
        String quotedJson = "\"{\\\"name\\\":\\\"Line1\\\\nLine2\\\",\\\"age\\\":55}\"";

        // When
        Object result = handler.parse(quotedJson);

        // Then
        TestObject obj = (TestObject) result;
        assertEquals("Line1\nLine2", obj.getName());
    }

    /**
     * 测试 parse() - 处理包含制表符的 JSON
     */
    @Test
    public void testParseWithTabCharacter() {
        // Given
        H2CompatibleJacksonTypeHandler handler = new H2CompatibleJacksonTypeHandler(TestObject.class);
        // 包含 \t 的 JSON
        String quotedJson = "\"{\\\"name\\\":\\\"Col1\\\\tCol2\\\",\\\"age\\\":60}\"";

        // When
        Object result = handler.parse(quotedJson);

        // Then
        TestObject obj = (TestObject) result;
        assertEquals("Col1\tCol2", obj.getName());
    }

    /**
     * 测试 parse() - 处理包含反斜杠的 JSON
     */
    @Test
    public void testParseWithBackslash() {
        // Given
        H2CompatibleJacksonTypeHandler handler = new H2CompatibleJacksonTypeHandler(TestObject.class);
        // 包含 \\ 的 JSON
        String quotedJson = "\"{\\\"name\\\":\\\"C:\\\\\\\\Users\\\\\\\\Test\\\",\\\"age\\\":65}\"";

        // When
        Object result = handler.parse(quotedJson);

        // Then
        TestObject obj = (TestObject) result;
        assertEquals("C:\\Users\\Test", obj.getName());
    }
}
