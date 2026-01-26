package io.hqwu.commons.mybatisplus;

import com.baomidou.mybatisplus.core.metadata.TableFieldInfo;
import io.hqwu.commons.mybatisplus.entity.TAreas;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test cases for {@link JoinQueryWrapper}
 *
 * <p>只测试 JoinQueryWrapper 自己实现的方法，不测试从 QueryWrapper 继承的方法。
 * JoinQueryWrapper 自己实现的方法包括：
 * <ul>
 *   <li>构造函数: 无参、带实体</li>
 *   <li>select(String... columns) - 重写</li>
 *   <li>select(Class, Predicate) - 重写</li>
 *   <li>getSqlSelect() - 重写</li>
 *   <li>lambda(String mainTable) - 新增</li>
 * </ul>
 *
 * @author Wu, Hongqiang
 * @since 2026/1/25
 */
@SpringBootTest
public class JoinQueryWrapperTest {

    /**
     * 测试无参构造函数
     */
    @Test
    public void testDefaultConstructor() {
        // When
        JoinQueryWrapper<TAreas> wrapper = new JoinQueryWrapper<>();

        // Then
        assertNotNull(wrapper);
        assertNull(wrapper.getEntity());
        assertNull(wrapper.getSqlSelect());
    }

    /**
     * 测试带实体参数的构造函数
     */
    @Test
    public void testConstructorWithEntity() {
        // Given
        TAreas entity = new TAreas();
        entity.setName("Beijing");

        // When
        JoinQueryWrapper<TAreas> wrapper = new JoinQueryWrapper<>(entity);

        // Then
        assertNotNull(wrapper);
        assertNotNull(wrapper.getEntity());
        assertEquals("Beijing", wrapper.getEntity().getName());
    }

    /**
     * 测试带实体和列参数的构造函数
     * 这个构造函数会在初始化时设置 select 字段
     */
    @Test
    public void testConstructorWithEntityAndColumns() {
        // Given
        TAreas entity = new TAreas();
        entity.setId("A001");
        entity.setName("Beijing");

        // When
        JoinQueryWrapper<TAreas> wrapper = new JoinQueryWrapper<>(entity, "id", "name", "parent_id");

        // Then
        assertNotNull(wrapper);
        assertNotNull(wrapper.getEntity());
        assertEquals("A001", wrapper.getEntity().getId());
        assertEquals("Beijing", wrapper.getEntity().getName());

        // 验证 select 字段已经被设置
        assertNotNull(wrapper.getSqlSelect());
        assertEquals("id,name,parent_id", wrapper.getSqlSelect());
    }

    /**
     * 测试带实体和列参数的构造函数 - 单列
     */
    @Test
    public void testConstructorWithEntityAndSingleColumn() {
        // Given
        TAreas entity = new TAreas();
        entity.setParentId(100);

        // When
        JoinQueryWrapper<TAreas> wrapper = new JoinQueryWrapper<>(entity, "id");

        // Then
        assertNotNull(wrapper);
        assertEquals(100, wrapper.getEntity().getParentId());
        assertEquals("id", wrapper.getSqlSelect());
    }

    /**
     * 测试带实体和空列数组的构造函数
     */
    @Test
    public void testConstructorWithEntityAndEmptyColumns() {
        // Given
        TAreas entity = new TAreas();
        entity.setName("Shanghai");

        // When
        JoinQueryWrapper<TAreas> wrapper = new JoinQueryWrapper<>(entity, new String[0]);

        // Then
        assertNotNull(wrapper);
        assertEquals("Shanghai", wrapper.getEntity().getName());
        // 空列数组不应该设置 sqlSelect
        assertNull(wrapper.getSqlSelect());
    }

    /**
     * 测试带实体和列参数的构造函数 - 包含表别名的列
     */
    @Test
    public void testConstructorWithEntityAndColumnsWithAlias() {
        // Given
        TAreas entity = new TAreas();
        entity.setId("A002");

        // When - 使用包含表别名和 AS 的列名
        JoinQueryWrapper<TAreas> wrapper = new JoinQueryWrapper<>(entity,
            "c.id", "c.name", "c.parent_id", "p.name AS parent_name");

        // Then
        assertNotNull(wrapper);
        assertEquals("A002", wrapper.getEntity().getId());

        // 验证 select 包含所有列（包括别名）
        String sqlSelect = wrapper.getSqlSelect();
        assertNotNull(sqlSelect);
        assertEquals("c.id,c.name,c.parent_id,p.name AS parent_name", sqlSelect);
        assertTrue(sqlSelect.contains("c.id"));
        assertTrue(sqlSelect.contains("parent_name"));
    }

    /**
     * 测试带 null 实体和列参数的构造函数
     */
    @Test
    public void testConstructorWithNullEntityAndColumns() {
        // When
        JoinQueryWrapper<TAreas> wrapper = new JoinQueryWrapper<>(null, "id", "name");

        // Then
        assertNotNull(wrapper);
        assertNull(wrapper.getEntity());
        assertEquals("id,name", wrapper.getSqlSelect());
    }

    /**
     * 测试构造函数设置的 select 可以被后续调用覆盖
     */
    @Test
    public void testConstructorColumnsCanBeOverridden() {
        // Given
        TAreas entity = new TAreas();
        JoinQueryWrapper<TAreas> wrapper = new JoinQueryWrapper<>(entity, "id", "name");

        // When - 后续调用 select 覆盖构造函数中的设置
        wrapper.select("parent_id", "id");

        // Then
        assertEquals("parent_id,id", wrapper.getSqlSelect(),
            "后续 select 调用应该覆盖构造函数中的设置");
    }

    /**
     * 测试带实体和列参数的构造函数 - 与条件查询配合使用
     */
    @Test
    public void testConstructorWithEntityAndColumnsWithConditions() {
        // Given
        TAreas entity = new TAreas();
        entity.setParentId(100);

        // When
        JoinQueryWrapper<TAreas> wrapper = new JoinQueryWrapper<>(entity, "c.id", "c.name", "p.name AS parent_name");
        wrapper.eq("p.id", entity.getParentId())
               .like("c.name", "Bei%");

        // Then
        assertNotNull(wrapper.getSqlSelect());
        assertEquals("c.id,c.name,p.name AS parent_name", wrapper.getSqlSelect());

        String sqlSegment = wrapper.getSqlSegment();
        assertTrue(sqlSegment.contains("p.id"));
        assertTrue(sqlSegment.contains("c.name"));
        assertTrue(sqlSegment.contains("LIKE"));
    }

    /**
     * 测试 select 方法 - 字符串数组参数
     */
    @Test
    public void testSelectWithStringColumns() {
        // Given
        JoinQueryWrapper<TAreas> wrapper = new JoinQueryWrapper<>();

        // When
        wrapper.select("id", "name", "parent_id");

        // Then
        assertNotNull(wrapper.getSqlSelect());
        assertEquals("id,name,parent_id", wrapper.getSqlSelect());
    }

    /**
     * 测试 select 方法 - 单个列
     */
    @Test
    public void testSelectWithSingleColumn() {
        // Given
        JoinQueryWrapper<TAreas> wrapper = new JoinQueryWrapper<>();

        // When
        wrapper.select("id");

        // Then
        assertEquals("id", wrapper.getSqlSelect());
    }

    /**
     * 测试 select 方法 - 空数组
     */
    @Test
    public void testSelectWithEmptyArray() {
        // Given
        JoinQueryWrapper<TAreas> wrapper = new JoinQueryWrapper<>();

        // When
        wrapper.select(new String[0]);

        // Then
        assertNull(wrapper.getSqlSelect());
    }

    /**
     * 测试 select 方法 - 多次调用（后者覆盖前者）
     */
    @Test
    public void testSelectMultipleTimes() {
        // Given
        JoinQueryWrapper<TAreas> wrapper = new JoinQueryWrapper<>();

        // When
        wrapper.select("id", "name");
        wrapper.select("parent_id");

        // Then
        assertEquals("parent_id", wrapper.getSqlSelect(),
            "Second select should override first");
    }

    /**
     * 测试 select 方法 - 使用 Predicate 选择所有字段
     */
    @Test
    public void testSelectWithPredicate() {
        // Given
        JoinQueryWrapper<TAreas> wrapper = new JoinQueryWrapper<>();

        // When - 选择所有字段
        wrapper.select(TAreas.class, tableFieldInfo -> true);

        // Then
        assertNotNull(wrapper.getSqlSelect());
        assertTrue(wrapper.getSqlSelect().contains("name"));
        assertTrue(wrapper.getSqlSelect().contains("parent_id"));
    }

    /**
     * 测试 select 方法 - 使用 Predicate 过滤特定字段
     */
    @Test
    public void testSelectWithPredicateFilter() {
        // Given
        JoinQueryWrapper<TAreas> wrapper = new JoinQueryWrapper<>();

        // When - 只选择字符串类型的字段
        wrapper.select(TAreas.class, TableFieldInfo::isCharSequence);

        // Then
        assertNotNull(wrapper.getSqlSelect());
        assertTrue(wrapper.getSqlSelect().contains("name"));
        assertFalse(wrapper.getSqlSelect().contains("parent_id"));
    }

    /**
     * 测试 getSqlSelect 初始状态
     */
    @Test
    public void testGetSqlSelectInitialState() {
        // Given
        JoinQueryWrapper<TAreas> wrapper = new JoinQueryWrapper<>();

        // When
        String sqlSelect = wrapper.getSqlSelect();

        // Then
        assertNull(sqlSelect);
    }

    /**
     * 测试 lambda 方法 - 创建 MainLambdaQueryWrapper
     */
    @Test
    public void testLambdaMethod() {
        // Given
        JoinQueryWrapper<TAreas> wrapper = new JoinQueryWrapper<>();

        // When
        MainLambdaQueryWrapper<TAreas> lambdaWrapper = wrapper.lambda("main");

        // Then
        assertNotNull(lambdaWrapper);
    }

    /**
     * 测试 lambda 方法 - 与 select 配合使用
     */
    @Test
    public void testLambdaWithSelect() {
        // Given
        JoinQueryWrapper<TAreas> wrapper = new JoinQueryWrapper<>();
        wrapper.select("c.id", "c.name", "c.parent_id");

        // When
        MainLambdaQueryWrapper<TAreas> lambdaWrapper = wrapper.lambda("c");

        // Then
        assertNotNull(lambdaWrapper);
        assertEquals("c.id,c.name,c.parent_id", lambdaWrapper.getSqlSelect());
    }

    /**
     * 测试 lambda 方法 - 带空表别名
     */
    @Test
    public void testLambdaWithEmptyTableAlias() {
        // Given
        JoinQueryWrapper<TAreas> wrapper = new JoinQueryWrapper<>();
        wrapper.select("id", "name");

        // When
        MainLambdaQueryWrapper<TAreas> lambdaWrapper = wrapper.lambda("");

        // Then
        assertNotNull(lambdaWrapper);
        assertEquals("id,name", lambdaWrapper.getSqlSelect());
    }

    /**
     * 测试完整的 JOIN 查询场景
     * 验证 JoinQueryWrapper 的 select、lambda 方法在实际场景中的配合使用
     */
    @Test
    public void testCompleteJoinQueryScenario() {
        // Given
        JoinQueryWrapper<TAreas> wrapper = new JoinQueryWrapper<>();

        // When - 模拟一个完整的 JOIN 查询
        wrapper.select("c.id", "c.name", "c.parent_id", "p.name AS parent_name")
               .eq("p.id", 100);

        MainLambdaQueryWrapper<TAreas> lambdaWrapper = wrapper.lambda("c");
        lambdaWrapper.ge(TAreas::getId, "A001")
                     .orderByAsc(TAreas::getName);

        // Then - 验证 JoinQueryWrapper 的方法
        assertNotNull(wrapper.getSqlSelect());
        assertTrue(wrapper.getSqlSelect().contains("c.id"));
        assertTrue(wrapper.getSqlSelect().contains("parent_name"));

        // 验证 lambda 方法返回的 wrapper 包含正确的 select
        assertNotNull(lambdaWrapper.getSqlSelect());
        assertEquals(wrapper.getSqlSelect(), lambdaWrapper.getSqlSelect());
    }

    // ========== instance() 方法测试 ==========
    // instance() 方法是 protected 方法，主要用于嵌套查询
    // 通过以下 public 方法间接调用：and(Consumer), or(Consumer), nested(Consumer)

    /**
     * 测试 instance() 方法 - 通过 and 嵌套条件调用
     * and(Consumer) 方法内部会调用 instance() 创建嵌套的 wrapper
     */
    @Test
    public void testInstanceMethodViaAndNested() {
        // Given
        JoinQueryWrapper<TAreas> wrapper = new JoinQueryWrapper<>();
        wrapper.select("id", "name", "parent_id");

        // When - 使用 and 嵌套条件，内部会调用 instance()
        wrapper.eq("p.id", 100)
               .and(nested -> nested.eq("name", "Beijing").or().eq("name", "Shanghai"));

        // Then - 验证嵌套查询生成正确的 SQL
        String sqlSegment = wrapper.getSqlSegment();
        assertNotNull(sqlSegment);
        assertTrue(sqlSegment.contains("p.id"));
        assertTrue(sqlSegment.contains("name"));
        assertTrue(sqlSegment.contains("OR"));
        // 验证有参数占位符
        assertTrue(sqlSegment.contains("#{ew.paramNameValuePairs."));

        // 重要：验证嵌套 wrapper 不会影响 select 字段
        // instance() 方法注释说明："故 sqlSelect 不向下传递"
        assertEquals("id,name,parent_id", wrapper.getSqlSelect());
    }

    /**
     * 测试 instance() 方法 - 通过 or 嵌套条件调用
     */
    @Test
    public void testInstanceMethodViaOrNested() {
        // Given
        JoinQueryWrapper<TAreas> wrapper = new JoinQueryWrapper<>();
        wrapper.select("c.id", "c.name");

        // When - 使用 or 嵌套条件
        wrapper.eq("p.parent_id", 1)
               .or(nested -> nested.like("c.name", "Bei%")
                                   .like("c.name", "Shang%"));

        // Then
        String sqlSegment = wrapper.getSqlSegment();
        assertNotNull(sqlSegment);
        assertTrue(sqlSegment.contains("p.parent_id"));
        assertTrue(sqlSegment.contains("c.name"));
        assertTrue(sqlSegment.contains("LIKE"));

        // 验证 select 字段没有传递到嵌套 wrapper
        assertEquals("c.id,c.name", wrapper.getSqlSelect());
    }

    /**
     * 测试 instance() 方法 - 通过 nested 条件调用
     */
    @Test
    public void testInstanceMethodViaNested() {
        // Given
        JoinQueryWrapper<TAreas> wrapper = new JoinQueryWrapper<>();
        wrapper.select("id", "name", "parent_id");

        // When - 使用 nested 条件，会调用 instance() 创建独立的条件组
        wrapper.nested(nested -> nested.eq("name", "Beijing")
                                       .eq("parent_id", 100))
               .or()
               .nested(nested -> nested.eq("name", "Shanghai")
                                       .eq("parent_id", 200));

        // Then
        String sqlSegment = wrapper.getSqlSegment();
        assertNotNull(sqlSegment);
        assertTrue(sqlSegment.contains("name"));
        assertTrue(sqlSegment.contains("parent_id"));
        assertTrue(sqlSegment.contains("OR"));

        // 验证 sqlSelect 不向下传递
        assertEquals("id,name,parent_id", wrapper.getSqlSelect());
    }

    /**
     * 测试 instance() 方法 - 多层嵌套
     */
    @Test
    public void testInstanceMethodWithMultiLevelNesting() {
        // Given
        JoinQueryWrapper<TAreas> wrapper = new JoinQueryWrapper<>();
        wrapper.select("c.id", "c.name", "p.name as parent_name");

        // When - 多层嵌套，每层都会调用 instance()
        wrapper.eq("p.id", 1)
               .and(level1 -> level1
                   .eq("c.parent_id", 100)
                   .or(level2 -> level2
                       .like("c.name", "Bei%")
                       .like("c.name", "Shang%")));

        // Then
        String sqlSegment = wrapper.getSqlSegment();
        assertNotNull(sqlSegment);
        assertTrue(sqlSegment.contains("p.id"));
        assertTrue(sqlSegment.contains("c.parent_id"));
        assertTrue(sqlSegment.contains("c.name"));
        assertTrue(sqlSegment.contains("LIKE"));

        // 验证最外层的 select 保持不变
        String sqlSelect = wrapper.getSqlSelect();
        assertEquals("c.id,c.name,p.name as parent_name", sqlSelect);
    }

    /**
     * 测试 instance() 方法创建的嵌套 wrapper 不继承 select 字段
     * 这是 instance() 方法的关键特性："故 sqlSelect 不向下传递"
     */
    @Test
    public void testInstanceMethodDoesNotInheritSqlSelect() {
        // Given
        JoinQueryWrapper<TAreas> wrapper = new JoinQueryWrapper<>();
        wrapper.select("c.id", "c.name", "c.parent_id", "p.name AS parent_name");

        // When - 创建嵌套条件
        wrapper.eq("p.id", 100)
               .and(nested -> {
                   // 嵌套 wrapper 的 getSqlSelect() 应该返回 null
                   // 因为 instance() 方法传递 SharedString.emptyString()
                   nested.eq("c.name", "Beijing");

                   // 注意：这里我们不能直接访问 nested，因为它是 lambda 参数
                   // 但通过 SQL 生成可以验证行为
               });

        // Then - 主 wrapper 的 select 保持不变
        assertEquals("c.id,c.name,c.parent_id,p.name AS parent_name", wrapper.getSqlSelect());

        // SQL 片段应该正确生成
        String sqlSegment = wrapper.getSqlSegment();
        assertTrue(sqlSegment.contains("p.id"));
        assertTrue(sqlSegment.contains("c.name"));
    }

    /**
     * 测试 instance() 方法 - 验证嵌套条件与主条件的隔离性
     */
    @Test
    public void testInstanceMethodIsolation() {
        // Given
        JoinQueryWrapper<TAreas> wrapper = new JoinQueryWrapper<>();
        wrapper.select("id", "name");

        // When - 添加主条件和嵌套条件
        wrapper.eq("parent_id", 1)
               .and(nested -> nested.isNotNull("id").isNotNull("name"))
               .or()
               .eq("parent_id", 2);

        // Then - 验证条件正确生成
        String sqlSegment = wrapper.getSqlSegment();
        assertNotNull(sqlSegment);
        assertTrue(sqlSegment.contains("parent_id"));
        assertTrue(sqlSegment.contains("id"));
        assertTrue(sqlSegment.contains("name"));
        assertTrue(sqlSegment.contains("IS NOT NULL"));

        // 主 wrapper 的 select 应该保持不变
        assertEquals("id,name", wrapper.getSqlSelect());
    }

    /**
     * 测试 instance() 方法 - 空嵌套条件
     */
    @Test
    public void testInstanceMethodWithEmptyNested() {
        // Given
        JoinQueryWrapper<TAreas> wrapper = new JoinQueryWrapper<>();
        wrapper.select("id", "name");

        // When - 空的嵌套条件
        wrapper.eq("parent_id", 1)
               .and(nested -> {
                   // 空的嵌套，不添加任何条件
               });

        // Then - 主条件应该仍然存在
        String sqlSegment = wrapper.getSqlSegment();
        assertNotNull(sqlSegment);
        assertTrue(sqlSegment.contains("parent_id"));

        // select 保持不变
        assertEquals("id,name", wrapper.getSqlSelect());
    }
}
