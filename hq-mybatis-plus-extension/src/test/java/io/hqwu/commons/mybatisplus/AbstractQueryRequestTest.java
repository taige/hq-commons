package io.hqwu.commons.mybatisplus;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test cases for {@link AbstractQueryRequest}
 *
 * <p>本测试只关注 AbstractQueryRequest 自己的代码：
 * <ul>
 *   <li>构造函数：初始化默认值</li>
 *   <li>isAscend()：判断排序方向</li>
 *   <li>setAscend/setDescend：便捷设置方法</li>
 *   <li>getter/setter：所有属性的访问</li>
 *   <li>toString()：字符串表示</li>
 * </ul>
 *
 * @author Wu, Hongqiang
 * @since 2026/1/26
 */
public class AbstractQueryRequestTest {

    // ========== 构造函数测试 ==========

    /**
     * 测试构造函数 - 验证默认值
     */
    @Test
    public void testConstructor() {
        // When
        AreaQuery request = new AreaQuery();

        // Then - 验证默认值
        assertEquals(50, request.getPageSize());
        assertEquals(1, request.getPageNum());
        assertNull(request.getSortField());
        assertNull(request.getSortOrder());
    }

    // ========== isAscend() 方法测试 ==========

    /**
     * 测试 isAscend() - sortOrder 为 null 时默认为升序
     */
    @Test
    public void testIsAscendWhenSortOrderIsNull() {
        // Given
        AreaQuery request = new AreaQuery();
        // sortOrder 默认为 null

        // When & Then
        assertTrue(request.isAscend());
    }

    /**
     * 测试 isAscend() - sortOrder 为空字符串时默认为升序
     */
    @Test
    public void testIsAscendWhenSortOrderIsEmpty() {
        // Given
        AreaQuery request = new AreaQuery();
        request.setSortOrder("");

        // When & Then
        assertTrue(request.isAscend());
    }

    /**
     * 测试 isAscend() - sortOrder 为空白字符串时默认为升序
     */
    @Test
    public void testIsAscendWhenSortOrderIsBlank() {
        // Given
        AreaQuery request = new AreaQuery();
        request.setSortOrder("   ");

        // When & Then
        assertTrue(request.isAscend());
    }

    /**
     * 测试 isAscend() - sortOrder 为 "ascend" 时返回 true
     */
    @Test
    public void testIsAscendWhenSortOrderIsAscend() {
        // Given
        AreaQuery request = new AreaQuery();
        request.setSortOrder("ascend");

        // When & Then
        assertTrue(request.isAscend());
    }

    /**
     * 测试 isAscend() - sortOrder 为 "descend" 时返回 false
     */
    @Test
    public void testIsAscendWhenSortOrderIsDescend() {
        // Given
        AreaQuery request = new AreaQuery();
        request.setSortOrder("descend");

        // When & Then
        assertFalse(request.isAscend());
    }

    // ========== setAscend() 方法测试 ==========

    /**
     * 测试 setAscend() - 设置升序排序
     */
    @Test
    public void testSetAscend() {
        // Given
        AreaQuery request = new AreaQuery();

        // When
        AreaQuery result = request.setAscend("createTime");

        // Then
        assertSame(request, result, "应该返回 this 支持链式调用");
        assertEquals("createTime", request.getSortField());
        assertEquals("ascend", request.getSortOrder());
        assertTrue(request.isAscend());
    }

    /**
     * 测试 setAscend() - 覆盖之前的降序设置
     */
    @Test
    public void testSetAscendOverrideDescend() {
        // Given
        AreaQuery request = new AreaQuery();
        request.setDescend("updateTime");

        // When
        request.setAscend("createTime");

        // Then
        assertEquals("createTime", request.getSortField());
        assertEquals("ascend", request.getSortOrder());
        assertTrue(request.isAscend());
    }

    // ========== setDescend() 方法测试 ==========

    /**
     * 测试 setDescend() - 设置降序排序
     */
    @Test
    public void testSetDescend() {
        // Given
        AreaQuery request = new AreaQuery();

        // When
        AreaQuery result = request.setDescend("updateTime");

        // Then
        assertSame(request, result, "应该返回 this 支持链式调用");
        assertEquals("updateTime", request.getSortField());
        assertEquals("descend", request.getSortOrder());
        assertFalse(request.isAscend());
    }

    /**
     * 测试 setDescend() - 覆盖之前的升序设置
     */
    @Test
    public void testSetDescendOverrideAscend() {
        // Given
        AreaQuery request = new AreaQuery();
        request.setAscend("createTime");

        // When
        request.setDescend("updateTime");

        // Then
        assertEquals("updateTime", request.getSortField());
        assertEquals("descend", request.getSortOrder());
        assertFalse(request.isAscend());
    }

    // ========== Setter 方法测试 ==========

    /**
     * 测试 setPageSize() - 正常值
     */
    @Test
    public void testSetPageSize() {
        // Given
        AreaQuery request = new AreaQuery();

        // When
        AreaQuery result = request.setPageSize(20);

        // Then
        assertSame(request, result, "应该返回 this 支持链式调用");
        assertEquals(20, request.getPageSize());
    }

    /**
     * 测试 setPageSize() - 边界值 1
     */
    @Test
    public void testSetPageSizeMinValue() {
        // Given
        AreaQuery request = new AreaQuery();

        // When
        request.setPageSize(1);

        // Then
        assertEquals(1, request.getPageSize());
    }

    /**
     * 测试 setPageSize() - 边界值 100
     */
    @Test
    public void testSetPageSizeMaxValue() {
        // Given
        AreaQuery request = new AreaQuery();

        // When
        request.setPageSize(100);

        // Then
        assertEquals(100, request.getPageSize());
    }

    /**
     * 测试 setPageNum() - 正常值
     */
    @Test
    public void testSetPageNum() {
        // Given
        AreaQuery request = new AreaQuery();

        // When
        AreaQuery result = request.setPageNum(5);

        // Then
        assertSame(request, result, "应该返回 this 支持链式调用");
        assertEquals(5, request.getPageNum());
    }

    /**
     * 测试 setPageNum() - 边界值 1
     */
    @Test
    public void testSetPageNumMinValue() {
        // Given
        AreaQuery request = new AreaQuery();

        // When
        request.setPageNum(1);

        // Then
        assertEquals(1, request.getPageNum());
    }

    /**
     * 测试 setSortField() - 正常值
     */
    @Test
    public void testSetSortField() {
        // Given
        AreaQuery request = new AreaQuery();

        // When
        AreaQuery result = request.setSortField("name");

        // Then
        assertSame(request, result, "应该返回 this 支持链式调用");
        assertEquals("name", request.getSortField());
    }

    /**
     * 测试 setSortField() - null 值
     */
    @Test
    public void testSetSortFieldNull() {
        // Given
        AreaQuery request = new AreaQuery();
        request.setSortField("name");

        // When
        request.setSortField(null);

        // Then
        assertNull(request.getSortField());
    }

    /**
     * 测试 setSortOrder() - ascend
     */
    @Test
    public void testSetSortOrderAscend() {
        // Given
        AreaQuery request = new AreaQuery();

        // When
        AreaQuery result = request.setSortOrder("ascend");

        // Then
        assertSame(request, result, "应该返回 this 支持链式调用");
        assertEquals("ascend", request.getSortOrder());
    }

    /**
     * 测试 setSortOrder() - descend
     */
    @Test
    public void testSetSortOrderDescend() {
        // Given
        AreaQuery request = new AreaQuery();

        // When
        request.setSortOrder("descend");

        // Then
        assertEquals("descend", request.getSortOrder());
    }

    /**
     * 测试 setSortOrder() - null 值
     */
    @Test
    public void testSetSortOrderNull() {
        // Given
        AreaQuery request = new AreaQuery();
        request.setSortOrder("ascend");

        // When
        request.setSortOrder(null);

        // Then
        assertNull(request.getSortOrder());
    }

    // ========== Getter 方法测试 ==========

    /**
     * 测试 getPageSize() - 默认值
     */
    @Test
    public void testGetPageSizeDefault() {
        // Given
        AreaQuery request = new AreaQuery();

        // When & Then
        assertEquals(50, request.getPageSize());
    }

    /**
     * 测试 getPageNum() - 默认值
     */
    @Test
    public void testGetPageNumDefault() {
        // Given
        AreaQuery request = new AreaQuery();

        // When & Then
        assertEquals(1, request.getPageNum());
    }

    /**
     * 测试 getSortField() - 默认值
     */
    @Test
    public void testGetSortFieldDefault() {
        // Given
        AreaQuery request = new AreaQuery();

        // When & Then
        assertNull(request.getSortField());
    }

    /**
     * 测试 getSortOrder() - 默认值
     */
    @Test
    public void testGetSortOrderDefault() {
        // Given
        AreaQuery request = new AreaQuery();

        // When & Then
        assertNull(request.getSortOrder());
    }

    // ========== 链式调用测试 ==========

    /**
     * 测试链式调用 - 完整的构建器模式
     */
    @Test
    public void testMethodChaining() {
        // When
        AreaQuery request = new AreaQuery()
                .setPageSize(20)
                .setPageNum(3)
                .setSortField("createTime")
                .setSortOrder("descend");

        // Then
        assertEquals(20, request.getPageSize());
        assertEquals(3, request.getPageNum());
        assertEquals("createTime", request.getSortField());
        assertEquals("descend", request.getSortOrder());
        assertFalse(request.isAscend());
    }

    /**
     * 测试链式调用 - 使用 setAscend
     */
    @Test
    public void testMethodChainingWithSetAscend() {
        // When
        AreaQuery request = new AreaQuery()
                .setPageSize(30)
                .setPageNum(2)
                .setAscend("updateTime");

        // Then
        assertEquals(30, request.getPageSize());
        assertEquals(2, request.getPageNum());
        assertEquals("updateTime", request.getSortField());
        assertEquals("ascend", request.getSortOrder());
        assertTrue(request.isAscend());
    }

    /**
     * 测试链式调用 - 使用 setDescend
     */
    @Test
    public void testMethodChainingWithSetDescend() {
        // When
        AreaQuery request = new AreaQuery()
                .setPageSize(10)
                .setDescend("name");

        // Then
        assertEquals(10, request.getPageSize());
        assertEquals("name", request.getSortField());
        assertEquals("descend", request.getSortOrder());
        assertFalse(request.isAscend());
    }

    // ========== toString() 方法测试 ==========

    /**
     * 测试 toString() - 默认值
     */
    @Test
    public void testToStringDefault() {
        // Given
        AreaQuery request = new AreaQuery();

        // When
        String result = request.toString();

        // Then
        assertNotNull(result);
        assertTrue(result.contains("AreaQuery"));
        assertTrue(result.contains("pageSize=50"));
        assertTrue(result.contains("pageNum=1"));
        assertTrue(result.contains("sortField='null'"));
        assertTrue(result.contains("sortOrder='null'"));
    }

    /**
     * 测试 toString() - 所有字段都有值
     */
    @Test
    public void testToStringWithAllFields() {
        // Given
        AreaQuery request = new AreaQuery()
                .setPageSize(20)
                .setPageNum(5)
                .setSortField("createTime")
                .setSortOrder("ascend");

        // When
        String result = request.toString();

        // Then
        assertNotNull(result);
        assertTrue(result.contains("pageSize=20"));
        assertTrue(result.contains("pageNum=5"));
        assertTrue(result.contains("sortField='createTime'"));
        assertTrue(result.contains("sortOrder='ascend'"));
    }

    /**
     * 测试 toString() - 使用 setAscend
     */
    @Test
    public void testToStringWithSetAscend() {
        // Given
        AreaQuery request = new AreaQuery()
                .setAscend("updateTime");

        // When
        String result = request.toString();

        // Then
        assertTrue(result.contains("sortField='updateTime'"));
        assertTrue(result.contains("sortOrder='ascend'"));
    }

    /**
     * 测试 toString() - 使用 setDescend
     */
    @Test
    public void testToStringWithSetDescend() {
        // Given
        AreaQuery request = new AreaQuery()
                .setDescend("name");

        // When
        String result = request.toString();

        // Then
        assertTrue(result.contains("sortField='name'"));
        assertTrue(result.contains("sortOrder='descend'"));
    }

    /**
     * 测试 toString() - 格式验证
     */
    @Test
    public void testToStringFormat() {
        // Given
        AreaQuery request = new AreaQuery();

        // When
        String result = request.toString();

        // Then
        assertTrue(result.startsWith("AreaQuery["));
        assertTrue(result.endsWith("]"));
        assertTrue(result.contains(", "), "字段之间应该用逗号分隔");
    }
}
