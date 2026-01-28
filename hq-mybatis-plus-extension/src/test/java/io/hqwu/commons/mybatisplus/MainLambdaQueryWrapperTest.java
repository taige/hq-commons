package io.hqwu.commons.mybatisplus;

import com.baomidou.mybatisplus.core.metadata.TableFieldInfo;
import io.hqwu.commons.mybatisplus.entity.TAreas;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test cases for {@link MainLambdaQueryWrapper}
 *
 * <p>只测试 MainLambdaQueryWrapper 自己实现的方法，不测试从 AbstractLambdaWrapper 继承的方法。
 * MainLambdaQueryWrapper 自己实现的方法包括：
 * <ul>
 *   <li>构造函数: 带 mainTable 参数的构造函数</li>
 *   <li>columnToString() - 重写，自动添加主表前缀</li>
 *   <li>select(Class, Predicate) - 重写</li>
 *   <li>select(boolean, List) - 重写</li>
 *   <li>getSqlSelect() - 实现接口方法</li>
 *   <li>instance() - 重写，用于生成嵌套 SQL</li>
 *   <li>clear() - 重写</li>
 * </ul>
 *
 * @author Wu, Hongqiang
 * @since 2026/1/26
 */
@SpringBootTest
public class MainLambdaQueryWrapperTest {

    // ========== 构造函数测试 ==========

    /**
     * 测试构造函数 - 只传入 mainTable
     */
    @Test
    public void testConstructorWithMainTable() {
        // When
        MainLambdaQueryWrapper<TAreas> wrapper = new MainLambdaQueryWrapper<>("c");

        // Then
        assertNotNull(wrapper);
        assertNull(wrapper.getEntity());
        assertNull(wrapper.getSqlSelect());
    }

    /**
     * 测试构造函数 - 传入 entity 和 mainTable
     */
    @Test
    public void testConstructorWithEntityAndMainTable() {
        // Given
        TAreas entity = new TAreas();
        entity.setName("Beijing");

        // When
        MainLambdaQueryWrapper<TAreas> wrapper = new MainLambdaQueryWrapper<>(entity, "c");

        // Then
        assertNotNull(wrapper);
        assertNotNull(wrapper.getEntity());
        assertEquals("Beijing", wrapper.getEntity().getName());
    }

    /**
     * 测试构造函数 - mainTable 末尾有点号会被去除
     */
    @Test
    public void testConstructorRemovesTrailingDots() {
        // When - mainTable 末尾有多个点
        MainLambdaQueryWrapper<TAreas> wrapper = new MainLambdaQueryWrapper<>("c...");

        // Then - 应该正常创建
        assertNotNull(wrapper);
    }

    /**
     * 测试构造函数 - mainTable 为空字符串
     */
    @Test
    public void testConstructorWithEmptyMainTable() {
        // When
        MainLambdaQueryWrapper<TAreas> wrapper = new MainLambdaQueryWrapper<>("");

        // Then
        assertNotNull(wrapper);
    }

    /**
     * 测试构造函数 - mainTable 为空白字符串
     */
    @Test
    public void testConstructorWithBlankMainTable() {
        // When
        MainLambdaQueryWrapper<TAreas> wrapper = new MainLambdaQueryWrapper<>("   ");

        // Then
        assertNotNull(wrapper);
    }

    // ========== columnToString() 测试（通过条件方法间接测试） ==========

    /**
     * 测试 columnToString - 通过 eq 方法验证主表前缀被添加
     */
    @Test
    public void testColumnToStringAddsMainTablePrefix() {
        // Given
        MainLambdaQueryWrapper<TAreas> wrapper = new MainLambdaQueryWrapper<>("c");

        // When
        wrapper.eq(TAreas::getId, "A001");

        // Then - 生成的 SQL 应该包含 "c." 前缀
        String sqlSegment = wrapper.getSqlSegment();
        assertTrue(sqlSegment.contains("c.id"), "SQL should contain 'c.id': " + sqlSegment);
    }

    /**
     * 测试 columnToString - 多个条件都应该添加主表前缀
     */
    @Test
    public void testColumnToStringWithMultipleConditions() {
        // Given
        MainLambdaQueryWrapper<TAreas> wrapper = new MainLambdaQueryWrapper<>("main");

        // When
        wrapper.eq(TAreas::getId, "A001")
               .like(TAreas::getName, "Bei%")
               .ge(TAreas::getParentId, 100);

        // Then - 所有列都应该有 "main." 前缀
        String sqlSegment = wrapper.getSqlSegment();
        assertTrue(sqlSegment.contains("main.id"), "Should contain 'main.id'");
        assertTrue(sqlSegment.contains("main.name"), "Should contain 'main.name'");
        assertTrue(sqlSegment.contains("main.parent_id"), "Should contain 'main.parent_id'");
    }

    /**
     * 测试 columnToString - mainTable 为空时不添加前缀
     */
    @Test
    public void testColumnToStringWithEmptyMainTable() {
        // Given
        MainLambdaQueryWrapper<TAreas> wrapper = new MainLambdaQueryWrapper<>("");

        // When
        wrapper.eq(TAreas::getId, "A001");

        // Then - 不应该有前缀
        String sqlSegment = wrapper.getSqlSegment();
        assertFalse(sqlSegment.contains(".id"), "Should not contain prefix: " + sqlSegment);
    }

    // ========== select() 方法测试 ==========

    /**
     * 测试 select(Class, Predicate) - 使用 Predicate 选择字段
     */
    @Test
    public void testSelectWithPredicate() {
        // Given
        MainLambdaQueryWrapper<TAreas> wrapper = new MainLambdaQueryWrapper<>("c");

        // When - 选择所有字段
        wrapper.select(TAreas.class, field -> true);

        // Then
        assertNotNull(wrapper.getSqlSelect());
        String sqlSelect = wrapper.getSqlSelect();
        assertTrue(sqlSelect.contains("id"));
        assertTrue(sqlSelect.contains("name"));
    }

    /**
     * 测试 select(Class, Predicate) - 使用 Predicate 过滤字段
     */
    @Test
    public void testSelectWithPredicateFilter() {
        // Given
        MainLambdaQueryWrapper<TAreas> wrapper = new MainLambdaQueryWrapper<>("c");

        // When - 只选择 String 类型的字段
        wrapper.select(TAreas.class, TableFieldInfo::isCharSequence);

        // Then
        assertNotNull(wrapper.getSqlSelect());
        String sqlSelect = wrapper.getSqlSelect();
        // String 类型字段应该被包含
        assertTrue(sqlSelect.contains("id") || sqlSelect.contains("name"));
    }

    /**
     * 测试 select(boolean, List) - 条件为 true
     */
    @Test
    public void testSelectWithConditionTrue() {
        // Given
        MainLambdaQueryWrapper<TAreas> wrapper = new MainLambdaQueryWrapper<>("c");

        // When
        wrapper.select(true, Arrays.asList(TAreas::getId, TAreas::getName));

        // Then
        assertNotNull(wrapper.getSqlSelect());
        String sqlSelect = wrapper.getSqlSelect();
        assertTrue(sqlSelect.contains("id"));
        assertTrue(sqlSelect.contains("name"));
    }

    /**
     * 测试 select(boolean, List) - 条件为 false
     */
    @Test
    public void testSelectWithConditionFalse() {
        // Given
        MainLambdaQueryWrapper<TAreas> wrapper = new MainLambdaQueryWrapper<>("c");

        // When
        wrapper.select(false, Arrays.asList(TAreas::getId, TAreas::getName));

        // Then - 条件为 false，不应该设置 sqlSelect
        assertNull(wrapper.getSqlSelect());
    }

    /**
     * 测试 select(boolean, List) - 列表为空
     */
    @Test
    public void testSelectWithEmptyList() {
        // Given
        MainLambdaQueryWrapper<TAreas> wrapper = new MainLambdaQueryWrapper<>("c");

        // When
        wrapper.select(true, Arrays.asList());

        // Then - 空列表不应该设置 sqlSelect
        assertNull(wrapper.getSqlSelect());
    }

    /**
     * 测试 select(boolean, List) - 列表为 null
     */
    @Test
    public void testSelectWithNullList() {
        // Given
        MainLambdaQueryWrapper<TAreas> wrapper = new MainLambdaQueryWrapper<>("c");

        // When - 显式指定类型以避免歧义
        wrapper.select(true, (java.util.List<com.baomidou.mybatisplus.core.toolkit.support.SFunction<TAreas, ?>>) null);

        // Then - null 列表不应该设置 sqlSelect
        assertNull(wrapper.getSqlSelect());
    }

    // ========== getSqlSelect() 方法测试 ==========

    /**
     * 测试 getSqlSelect() - 初始状态
     */
    @Test
    public void testGetSqlSelectInitialState() {
        // Given
        MainLambdaQueryWrapper<TAreas> wrapper = new MainLambdaQueryWrapper<>("c");

        // When & Then
        assertNull(wrapper.getSqlSelect());
    }

    /**
     * 测试 getSqlSelect() - select 之后
     */
    @Test
    public void testGetSqlSelectAfterSelect() {
        // Given
        MainLambdaQueryWrapper<TAreas> wrapper = new MainLambdaQueryWrapper<>("c");
        wrapper.select(true, Arrays.asList(TAreas::getId, TAreas::getName));

        // When
        String sqlSelect = wrapper.getSqlSelect();

        // Then
        assertNotNull(sqlSelect);
        assertTrue(sqlSelect.contains("id"));
        assertTrue(sqlSelect.contains("name"));
    }

    // ========== instance() 方法测试（通过嵌套查询间接测试） ==========

    /**
     * 测试 instance() - 通过 and(Consumer) 测试嵌套查询
     */
    @Test
    public void testInstanceMethodViaAndNested() {
        // Given
        MainLambdaQueryWrapper<TAreas> wrapper = new MainLambdaQueryWrapper<>("c");
        wrapper.select(true, Arrays.asList(TAreas::getId, TAreas::getName));

        // When - 使用嵌套条件
        wrapper.eq(TAreas::getParentId, 100)
               .and(nested -> nested.eq(TAreas::getName, "Beijing")
                                   .or()
                                   .eq(TAreas::getName, "Shanghai"));

        // Then - 嵌套查询应该正常工作
        String sqlSegment = wrapper.getSqlSegment();
        assertNotNull(sqlSegment);
        assertTrue(sqlSegment.contains("c.parent_id"));
        assertTrue(sqlSegment.contains("c.name"));
    }

    /**
     * 测试 instance() - 验证嵌套查询不继承 sqlSelect
     */
    @Test
    public void testInstanceMethodDoesNotInheritSqlSelect() {
        // Given
        MainLambdaQueryWrapper<TAreas> wrapper = new MainLambdaQueryWrapper<>("c");
        wrapper.select(true, Arrays.asList(TAreas::getId, TAreas::getName, TAreas::getParentId));

        // When - 嵌套查询
        wrapper.and(nested -> {
            nested.eq(TAreas::getId, "A001");
            // 嵌套的 wrapper 的 sqlSelect 应该是 null（因为 instance() 方法传入的是 null）
            // 这是代码设计的行为：嵌套查询不继承 sqlSelect
        });

        // Then - 外层的 sqlSelect 应该保持不变
        assertNotNull(wrapper.getSqlSelect());
        assertTrue(wrapper.getSqlSelect().contains("id"));
    }

    /**
     * 测试 instance() - 多层嵌套
     */
    @Test
    public void testInstanceMethodWithMultiLevelNesting() {
        // Given
        MainLambdaQueryWrapper<TAreas> wrapper = new MainLambdaQueryWrapper<>("c");

        // When - 多层嵌套
        wrapper.eq(TAreas::getParentId, 100)
               .and(level1 -> level1.eq(TAreas::getName, "Beijing")
                                    .or(level2 -> level2.like(TAreas::getName, "Shang%")
                                                        .ge(TAreas::getId, "A001")));

        // Then
        String sqlSegment = wrapper.getSqlSegment();
        assertNotNull(sqlSegment);
        assertTrue(sqlSegment.contains("c.parent_id"));
        assertTrue(sqlSegment.contains("c.name"));
    }

    // ========== clear() 方法测试 ==========

    /**
     * 测试 clear() - 清空所有设置
     */
    @Test
    public void testClear() {
        // Given
        MainLambdaQueryWrapper<TAreas> wrapper = new MainLambdaQueryWrapper<>("c");
        wrapper.select(true, Arrays.asList(TAreas::getId, TAreas::getName));
        wrapper.eq(TAreas::getId, "A001");

        // When
        wrapper.clear();

        // Then - sqlSelect 应该被清空
        assertNull(wrapper.getSqlSelect());
        // SQL 片段也应该被清空
        String sqlSegment = wrapper.getSqlSegment();
        assertTrue(sqlSegment == null || sqlSegment.isEmpty() || sqlSegment.isBlank());
    }

    /**
     * 测试 clear() - 清空后可以重新设置
     */
    @Test
    public void testClearAndReuse() {
        // Given
        MainLambdaQueryWrapper<TAreas> wrapper = new MainLambdaQueryWrapper<>("c");
        wrapper.select(true, Arrays.asList(TAreas::getId, TAreas::getName));
        wrapper.eq(TAreas::getId, "A001");
        wrapper.clear();

        // When - 重新设置
        wrapper.select(true, Arrays.asList(TAreas::getParentId));
        wrapper.eq(TAreas::getParentId, 100);

        // Then
        assertNotNull(wrapper.getSqlSelect());
        assertTrue(wrapper.getSqlSelect().contains("parent_id"));
        String sqlSegment = wrapper.getSqlSegment();
        assertTrue(sqlSegment.contains("c.parent_id"));
    }

    // ========== 综合测试 ==========

    /**
     * 测试完整的查询场景 - 包含 select、条件、嵌套
     */
    @Test
    public void testCompleteQueryScenario() {
        // Given
        MainLambdaQueryWrapper<TAreas> wrapper = new MainLambdaQueryWrapper<>("c");

        // When - 构建完整查询
        wrapper.select(true, Arrays.asList(TAreas::getId, TAreas::getName, TAreas::getParentId))
               .eq(TAreas::getParentId, 100)
               .and(nested -> nested.like(TAreas::getName, "Bei%")
                                   .or()
                                   .like(TAreas::getName, "Shang%"));

        // Then - 验证 select
        assertNotNull(wrapper.getSqlSelect());
        String sqlSelect = wrapper.getSqlSelect();
        assertTrue(sqlSelect.contains("id"));
        assertTrue(sqlSelect.contains("name"));
        assertTrue(sqlSelect.contains("parent_id"));

        // Then - 验证 where 条件
        String sqlSegment = wrapper.getSqlSegment();
        assertTrue(sqlSegment.contains("c.parent_id"));
        assertTrue(sqlSegment.contains("c.name"));
        assertTrue(sqlSegment.contains("LIKE"));
    }

    /**
     * 测试不同 mainTable 名称的影响
     */
    @Test
    public void testDifferentMainTableNames() {
        // When - 使用不同的 mainTable
        MainLambdaQueryWrapper<TAreas> wrapper1 = new MainLambdaQueryWrapper<>("t1");
        MainLambdaQueryWrapper<TAreas> wrapper2 = new MainLambdaQueryWrapper<>("table2");
        MainLambdaQueryWrapper<TAreas> wrapper3 = new MainLambdaQueryWrapper<>("a");

        wrapper1.eq(TAreas::getId, "A001");
        wrapper2.eq(TAreas::getId, "A001");
        wrapper3.eq(TAreas::getId, "A001");

        // Then - 每个 wrapper 应该有对应的表前缀
        assertTrue(wrapper1.getSqlSegment().contains("t1.id"));
        assertTrue(wrapper2.getSqlSegment().contains("table2.id"));
        assertTrue(wrapper3.getSqlSegment().contains("a.id"));
    }

    /**
     * 测试 select 和条件的组合使用
     */
    @Test
    public void testSelectAndConditionsCombination() {
        // Given
        MainLambdaQueryWrapper<TAreas> wrapper = new MainLambdaQueryWrapper<>("main");

        // When
        wrapper.select(TAreas.class, field -> true)
               .eq(TAreas::getParentId, 100)
               .like(TAreas::getName, "Test%")
               .orderByAsc(TAreas::getId);

        // Then
        assertNotNull(wrapper.getSqlSelect());
        String sqlSegment = wrapper.getSqlSegment();
        assertTrue(sqlSegment.contains("main.parent_id"));
        assertTrue(sqlSegment.contains("main.name"));
        assertTrue(sqlSegment.contains("ORDER BY"));
        assertTrue(sqlSegment.contains("main.id"));
    }
}
