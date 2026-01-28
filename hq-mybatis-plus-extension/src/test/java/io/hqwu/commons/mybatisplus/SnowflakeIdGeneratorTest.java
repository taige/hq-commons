package io.hqwu.commons.mybatisplus;

import com.baomidou.mybatisplus.core.toolkit.Sequence;
import io.hqwu.commons.mybatisplus.annotation.TableIdPrefix;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test cases for {@link SnowflakeIdGenerator}
 *
 * <p>本测试只关注 SnowflakeIdGenerator 自己的代码：
 * <ul>
 *   <li>构造函数：无参构造和带 Sequence 参数的构造</li>
 *   <li>nextUUID()：根据不同 entity 类型生成不同格式的 ID</li>
 * </ul>
 *
 * @author Wu, Hongqiang
 * @since 2026/1/26
 */
public class SnowflakeIdGeneratorTest {

    /**
     * 测试用实体类 - 没有注解
     */
    static class TestEntity {
        private String name;

        public TestEntity(String name) {
            this.name = name;
        }
    }

    /**
     * 测试用实体类 - 有 TableIdPrefix 注解且 value 不为空
     */
    @TableIdPrefix("USER_")
    static class UserEntity {
        private String username;

        public UserEntity(String username) {
            this.username = username;
        }
    }

    /**
     * 测试用实体类 - 有 TableIdPrefix 注解但 value 为空字符串
     */
    @TableIdPrefix("")
    static class EmptyPrefixEntity {
        private String data;
    }

    /**
     * 测试用实体类 - 有 TableIdPrefix 注解但 value 为空白字符串
     */
    @TableIdPrefix("   ")
    static class BlankPrefixEntity {
        private String data;
    }

    /**
     * 测试用实体类 - 类名较长
     */
    static class VeryLongEntityName {
        private String data;
    }

    // ========== 构造函数测试 ==========

    /**
     * 测试无参构造函数
     */
    @Test
    public void testDefaultConstructor() {
        // When
        SnowflakeIdGenerator generator = new SnowflakeIdGenerator();

        // Then - 不应该抛出异常
        assertNotNull(generator);
    }

    /**
     * 测试带 Sequence 参数的构造函数
     */
    @Test
    public void testConstructorWithSequence() {
        // Given
        Sequence sequence = new Sequence(null);

        // When
        SnowflakeIdGenerator generator = new SnowflakeIdGenerator(sequence);

        // Then
        assertNotNull(generator);
    }

    /**
     * 测试构造函数创建的 generator 能正常生成 ID
     */
    @Test
    public void testConstructorGeneratesValidId() {
        // Given
        SnowflakeIdGenerator generator = new SnowflakeIdGenerator();

        // When
        String id = generator.nextUUID(null);

        // Then
        assertNotNull(id);
        assertFalse(id.isEmpty());
        // 雪花 ID 应该是纯数字
        assertTrue(id.matches("\\d+"));
    }

    // ========== nextUUID() 方法测试 - entity 为 null ==========

    /**
     * 测试 nextUUID() - entity 为 null 时返回纯数字 ID
     */
    @Test
    public void testNextUUIDWithNullEntity() {
        // Given
        SnowflakeIdGenerator generator = new SnowflakeIdGenerator();

        // When
        String id = generator.nextUUID(null);

        // Then
        assertNotNull(id);
        // 应该是纯数字字符串
        assertTrue(id.matches("\\d+"), "ID should be numeric: " + id);
    }

    /**
     * 测试 nextUUID() - entity 为 null 时生成的 ID 不重复
     */
    @Test
    public void testNextUUIDWithNullEntityGeneratesUniqueIds() {
        // Given
        SnowflakeIdGenerator generator = new SnowflakeIdGenerator();

        // When
        String id1 = generator.nextUUID(null);
        String id2 = generator.nextUUID(null);

        // Then
        assertNotEquals(id1, id2, "Generated IDs should be unique");
    }

    // ========== nextUUID() 方法测试 - entity 为 String ==========

    /**
     * 测试 nextUUID() - entity 为 String 时，ID = String + 雪花ID
     */
    @Test
    public void testNextUUIDWithStringEntity() {
        // Given
        SnowflakeIdGenerator generator = new SnowflakeIdGenerator();
        String prefix = "PREFIX_";

        // When
        String id = generator.nextUUID(prefix);

        // Then
        assertNotNull(id);
        assertTrue(id.startsWith(prefix), "ID should start with: " + prefix);
        // 去掉前缀后应该是纯数字
        String numericPart = id.substring(prefix.length());
        assertTrue(numericPart.matches("\\d+"), "Numeric part should be digits: " + numericPart);
    }

    /**
     * 测试 nextUUID() - entity 为空字符串
     */
    @Test
    public void testNextUUIDWithEmptyStringEntity() {
        // Given
        SnowflakeIdGenerator generator = new SnowflakeIdGenerator();
        String emptyPrefix = "";

        // When
        String id = generator.nextUUID(emptyPrefix);

        // Then
        assertNotNull(id);
        // 应该是纯数字（空字符串 + 数字）
        assertTrue(id.matches("\\d+"));
    }

    /**
     * 测试 nextUUID() - entity 为带特殊字符的字符串
     */
    @Test
    public void testNextUUIDWithSpecialCharStringEntity() {
        // Given
        SnowflakeIdGenerator generator = new SnowflakeIdGenerator();
        String prefix = "ABC-123_";

        // When
        String id = generator.nextUUID(prefix);

        // Then
        assertTrue(id.startsWith(prefix));
        String numericPart = id.substring(prefix.length());
        assertTrue(numericPart.matches("\\d+"));
    }

    // ========== nextUUID() 方法测试 - entity 有 TableIdPrefix 注解 ==========

    /**
     * 测试 nextUUID() - entity 有 TableIdPrefix 注解且 value 不为空
     */
    @Test
    public void testNextUUIDWithTableIdPrefixAnnotation() {
        // Given
        SnowflakeIdGenerator generator = new SnowflakeIdGenerator();
        UserEntity entity = new UserEntity("test");

        // When
        String id = generator.nextUUID(entity);

        // Then
        assertNotNull(id);
        assertTrue(id.startsWith("USER_"), "ID should start with 'USER_': " + id);
        String numericPart = id.substring("USER_".length());
        assertTrue(numericPart.matches("\\d+"), "Numeric part should be digits: " + numericPart);
    }

    /**
     * 测试 nextUUID() - entity 有 TableIdPrefix 注解但 value 为空字符串
     * 此时应该fallback到使用类名前2个字符
     */
    @Test
    public void testNextUUIDWithEmptyTableIdPrefix() {
        // Given
        SnowflakeIdGenerator generator = new SnowflakeIdGenerator();
        EmptyPrefixEntity entity = new EmptyPrefixEntity();

        // When
        String id = generator.nextUUID(entity);

        // Then
        assertNotNull(id);
        // 应该使用类名前2个字符："Em"
        assertTrue(id.startsWith("Em"), "ID should start with 'Em': " + id);
        String numericPart = id.substring(2);
        assertTrue(numericPart.matches("\\d+"));
    }

    /**
     * 测试 nextUUID() - entity 有 TableIdPrefix 注解但 value 为空白字符串
     * 此时应该fallback到使用类名前2个字符
     */
    @Test
    public void testNextUUIDWithBlankTableIdPrefix() {
        // Given
        SnowflakeIdGenerator generator = new SnowflakeIdGenerator();
        BlankPrefixEntity entity = new BlankPrefixEntity();

        // When
        String id = generator.nextUUID(entity);

        // Then
        assertNotNull(id);
        // 应该使用类名前2个字符："Bl"
        assertTrue(id.startsWith("Bl"), "ID should start with 'Bl': " + id);
        String numericPart = id.substring(2);
        assertTrue(numericPart.matches("\\d+"));
    }

    // ========== nextUUID() 方法测试 - 默认情况（使用类名前2个字符） ==========

    /**
     * 测试 nextUUID() - 没有注解的普通实体，使用类名前2个字符
     */
    @Test
    public void testNextUUIDWithDefaultPrefix() {
        // Given
        SnowflakeIdGenerator generator = new SnowflakeIdGenerator();
        TestEntity entity = new TestEntity("test");

        // When
        String id = generator.nextUUID(entity);

        // Then
        assertNotNull(id);
        // TestEntity 的前2个字符是 "Te"
        assertTrue(id.startsWith("Te"), "ID should start with 'Te': " + id);
        String numericPart = id.substring(2);
        assertTrue(numericPart.matches("\\d+"), "Numeric part should be digits: " + numericPart);
    }

    /**
     * 测试 nextUUID() - 类名很长的实体，也只取前2个字符
     */
    @Test
    public void testNextUUIDWithLongClassName() {
        // Given
        SnowflakeIdGenerator generator = new SnowflakeIdGenerator();
        VeryLongEntityName entity = new VeryLongEntityName();

        // When
        String id = generator.nextUUID(entity);

        // Then
        assertNotNull(id);
        // VeryLongEntityName 的前2个字符是 "Ve"
        assertTrue(id.startsWith("Ve"), "ID should start with 'Ve': " + id);
        String numericPart = id.substring(2);
        assertTrue(numericPart.matches("\\d+"));
    }

    // ========== 综合测试 ==========

    /**
     * 测试不同类型的实体生成的 ID 格式都正确
     */
    @Test
    public void testNextUUIDWithVariousEntityTypes() {
        // Given
        SnowflakeIdGenerator generator = new SnowflakeIdGenerator();

        // When & Then - null
        String nullId = generator.nextUUID(null);
        assertTrue(nullId.matches("\\d+"));

        // When & Then - String
        String stringId = generator.nextUUID("STR_");
        assertTrue(stringId.startsWith("STR_"));

        // When & Then - 有注解的实体
        String annotatedId = generator.nextUUID(new UserEntity("test"));
        assertTrue(annotatedId.startsWith("USER_"));

        // When & Then - 无注解的实体
        String plainId = generator.nextUUID(new TestEntity("test"));
        assertTrue(plainId.startsWith("Te"));

        // 所有 ID 都不应该相同
        assertNotEquals(nullId, stringId.substring(4));
        assertNotEquals(annotatedId, plainId);
    }

    /**
     * 测试同一个 generator 生成的 ID 是递增的（雪花算法特性）
     */
    @Test
    public void testNextUUIDGeneratesIncreasingIds() {
        // Given
        SnowflakeIdGenerator generator = new SnowflakeIdGenerator();

        // When
        String id1 = generator.nextUUID(null);
        String id2 = generator.nextUUID(null);
        String id3 = generator.nextUUID(null);

        // Then - 转换为 Long 比较（雪花 ID 应该递增）
        long numId1 = Long.parseLong(id1);
        long numId2 = Long.parseLong(id2);
        long numId3 = Long.parseLong(id3);

        assertTrue(numId1 < numId2, "ID should increase: " + numId1 + " < " + numId2);
        assertTrue(numId2 < numId3, "ID should increase: " + numId2 + " < " + numId3);
    }

    /**
     * 测试使用自定义 Sequence 的 generator
     */
    @Test
    public void testNextUUIDWithCustomSequence() {
        // Given
        Sequence customSequence = new Sequence(1L, 1L);
        SnowflakeIdGenerator generator = new SnowflakeIdGenerator(customSequence);

        // When
        String id1 = generator.nextUUID(null);
        String id2 = generator.nextUUID(null);

        // Then
        assertNotNull(id1);
        assertNotNull(id2);
        assertNotEquals(id1, id2);
        assertTrue(id1.matches("\\d+"));
        assertTrue(id2.matches("\\d+"));
    }

    /**
     * 测试生成的 ID 长度合理（雪花 ID 是 19 位数字）
     */
    @Test
    public void testNextUUIDGeneratesReasonableLengthIds() {
        // Given
        SnowflakeIdGenerator generator = new SnowflakeIdGenerator();

        // When
        String id = generator.nextUUID(null);

        // Then - 雪花 ID 通常是 19 位数字
        assertTrue(id.length() >= 18 && id.length() <= 20,
                "ID length should be reasonable (18-20 digits): " + id.length());
    }

    /**
     * 测试带前缀的 ID 包含完整的雪花 ID
     */
    @Test
    public void testNextUUIDWithPrefixContainsFullSnowflakeId() {
        // Given
        SnowflakeIdGenerator generator = new SnowflakeIdGenerator();
        UserEntity entity = new UserEntity("test");

        // When
        String id = generator.nextUUID(entity);

        // Then
        assertTrue(id.startsWith("USER_"));
        String snowflakePart = id.substring("USER_".length());
        // 雪花 ID 部分应该是有效的长整数
        assertDoesNotThrow(() -> Long.parseLong(snowflakePart));
        assertTrue(snowflakePart.length() >= 18 && snowflakePart.length() <= 20);
    }
}
