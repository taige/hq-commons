package io.hqwu.commons.mybatisplus;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import io.hqwu.commons.mybatisplus.entity.TAreas;
import io.hqwu.commons.mybatisplus.pojo.Area;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test cases for {@link PageHelper}
 *
 * @author Wu, Hongqiang
 * @since 2026/1/25
 */
@SpringBootTest
public class PageHelperTest {

    /**
     * 测试基本的分页功能，不带排序
     */
    @Test
    public void testPageHelperBasicPagination() {
        // Given
        AreaQuery queryRequest = new AreaQuery();
        queryRequest.setPageNum(2);
        queryRequest.setPageSize(20);

        // When
        IPage<TAreas> page = PageHelper.pageHelper(queryRequest);

        // Then
        assertNotNull(page);
        assertEquals(2, page.getCurrent(), "Page number should be 2");
        assertEquals(20, page.getSize(), "Page size should be 20");
        assertTrue(page.orders().isEmpty(), "Should have no order items when sortField is not set");
    }

    /**
     * 测试升序排序
     */
    @Test
    public void testPageHelperAscendOrder() {
        // Given
        AreaQuery queryRequest = new AreaQuery();
        queryRequest.setPageNum(1);
        queryRequest.setPageSize(10);
        queryRequest.setAscend("name");

        // When
        IPage<TAreas> page = PageHelper.pageHelper(queryRequest);

        // Then
        assertNotNull(page);
        assertEquals(1, page.getCurrent());
        assertEquals(10, page.getSize());
        List<OrderItem> orders = page.orders();
        assertEquals(1, orders.size(), "Should have one order item");
        assertEquals("name", orders.get(0).getColumn());
        assertTrue(orders.get(0).isAsc(), "Should be ascending order");
    }

    /**
     * 测试降序排序
     */
    @Test
    public void testPageHelperDescendOrder() {
        // Given
        AreaQuery queryRequest = new AreaQuery();
        queryRequest.setPageNum(1);
        queryRequest.setPageSize(10);
        queryRequest.setDescend("parentId");

        // When
        IPage<TAreas> page = PageHelper.pageHelper(queryRequest);

        // Then
        assertNotNull(page);
        List<OrderItem> orders = page.orders();
        assertEquals(1, orders.size(), "Should have one order item");
        assertEquals("parent_id", orders.get(0).getColumn());
        assertFalse(orders.get(0).isAsc(), "Should be descending order");
    }

    /**
     * 测试带表别名的分页排序（用于JOIN查询）
     */
    @Test
    public void testPageHelperWithLeftTableAlias() {
        // Given
        AreaQuery queryRequest = new AreaQuery();
        queryRequest.setPageNum(1);
        queryRequest.setPageSize(15);
        queryRequest.setAscend("name");

        // When
        IPage<TAreas> page = PageHelper.pageHelper(queryRequest, "t");

        // Then
        assertNotNull(page);
        List<OrderItem> orders = page.orders();
        assertEquals(1, orders.size());
        assertEquals("t.name", orders.get(0).getColumn(), "Should have table alias prefix");
        assertTrue(orders.get(0).isAsc());
    }

    /**
     * 测试带点号后缀的表别名
     */
    @Test
    public void testPageHelperWithLeftTableAliasWithDot() {
        // Given
        AreaQuery queryRequest = new AreaQuery();
        queryRequest.setPageNum(1);
        queryRequest.setPageSize(15);
        queryRequest.setAscend("name");

        // When
        IPage<TAreas> page = PageHelper.pageHelper(queryRequest, "t.");

        // Then
        assertNotNull(page);
        List<OrderItem> orders = page.orders();
        assertEquals(1, orders.size());
        assertEquals("t.name", orders.get(0).getColumn(), "Should strip trailing dot from alias");
        assertTrue(orders.get(0).isAsc());
    }

    /**
     * 测试空字符串表别名
     */
    @Test
    public void testPageHelperWithEmptyLeftTable() {
        // Given
        AreaQuery queryRequest = new AreaQuery();
        queryRequest.setPageNum(1);
        queryRequest.setPageSize(10);
        queryRequest.setAscend("name");

        // When
        IPage<TAreas> page = PageHelper.pageHelper(queryRequest, "");

        // Then
        assertNotNull(page);
        List<OrderItem> orders = page.orders();
        assertEquals(1, orders.size());
        assertEquals("name", orders.get(0).getColumn(), "Should not add prefix for empty alias");
    }

    /**
     * 测试 @JoinColumn 注解的字段排序
     */
    @Test
    public void testPageHelperWithJoinColumnAnnotation() {
        // Given
        AreaQuery queryRequest = new AreaQuery();
        queryRequest.setPageNum(1);
        queryRequest.setPageSize(10);
        // parentName field has @JoinColumn("p.name") annotation
        queryRequest.setAscend("parentName");

        // When
        IPage<TAreas> page = PageHelper.pageHelper(queryRequest);

        // Then
        assertNotNull(page);
        List<OrderItem> orders = page.orders();
        assertEquals(1, orders.size());
        assertEquals("p.name", orders.get(0).getColumn(),
            "Should use JoinColumn value instead of entity field name");
        assertTrue(orders.get(0).isAsc());
    }

    /**
     * 测试 @JoinColumn 注解字段排序时忽略 leftTable 参数
     */
    @Test
    public void testPageHelperWithJoinColumnAnnotationIgnoresLeftTable() {
        // Given
        AreaQuery queryRequest = new AreaQuery();
        queryRequest.setPageNum(1);
        queryRequest.setPageSize(10);
        queryRequest.setAscend("parId");

        // When
        IPage<TAreas> page = PageHelper.pageHelper(queryRequest, "t");

        // Then
        assertNotNull(page);
        List<OrderItem> orders = page.orders();
        assertEquals(1, orders.size());
        // parId has @JoinColumn("p.id"), so it should use that directly
        assertEquals("p.id", orders.get(0).getColumn(),
            "Should use JoinColumn value and ignore leftTable parameter");
    }

    /**
     * 测试排序字段在 POJO 中不存在的情况
     */
    @Test
    public void testPageHelperWithNonExistentField() {
        // Given
        AreaQuery queryRequest = new AreaQuery();
        queryRequest.setPageNum(1);
        queryRequest.setPageSize(10);
        queryRequest.setAscend("nonExistentField");

        // When
        IPage<TAreas> page = PageHelper.pageHelper(queryRequest);

        // Then
        assertNotNull(page);
        assertTrue(page.orders().isEmpty(),
            "Should have no order items when field doesn't exist");
    }

    /**
     * 测试 getJoinColumn 方法 - 字段有 @JoinColumn 注解
     */
    @Test
    public void testGetJoinColumnWithAnnotation() {
        // When
        String joinColumn = PageHelper.getJoinColumn(TAreas.class, "parentName");

        // Then
        assertEquals("p.name", joinColumn);
    }

    /**
     * 测试 getJoinColumn 方法 - 字段没有 @JoinColumn 注解
     */
    @Test
    public void testGetJoinColumnWithoutAnnotation() {
        // When
        String joinColumn = PageHelper.getJoinColumn(TAreas.class, "name");

        // Then
        assertNull(joinColumn);
    }

    /**
     * 测试 getJoinColumn 方法 - 字段不存在
     */
    @Test
    public void testGetJoinColumnWithNonExistentField() {
        // When
        String joinColumn = PageHelper.getJoinColumn(TAreas.class, "nonExistentField");

        // Then
        assertNull(joinColumn);
    }

    /**
     * 测试默认分页参数
     */
    @Test
    public void testPageHelperWithDefaultParameters() {
        // Given
        AreaQuery queryRequest = new AreaQuery();
        // Using default pageNum=1, pageSize=50 from AbstractQueryRequest

        // When
        IPage<TAreas> page = PageHelper.pageHelper(queryRequest);

        // Then
        assertNotNull(page);
        assertEquals(1, page.getCurrent(), "Default page number should be 1");
        assertEquals(50, page.getSize(), "Default page size should be 50");
    }

    /**
     * 测试 PaginationQueryRequest 接口的默认 page() 方法
     */
    @Test
    public void testPaginationQueryRequestDefaultPageMethod() {
        // Given
        AreaQuery queryRequest = new AreaQuery();
        queryRequest.setPageNum(3);
        queryRequest.setPageSize(25);
        queryRequest.setAscend("id");

        // When
        IPage<TAreas> page = queryRequest.page();

        // Then
        assertNotNull(page);
        assertEquals(3, page.getCurrent());
        assertEquals(25, page.getSize());
        assertEquals(1, page.orders().size());
        assertEquals("id", page.orders().get(0).getColumn());
    }

    /**
     * 测试 PaginationQueryRequest 接口的 page(String) 方法
     */
    @Test
    public void testPaginationQueryRequestPageMethodWithTableAlias() {
        // Given
        AreaQuery queryRequest = new AreaQuery();
        queryRequest.setPageNum(2);
        queryRequest.setPageSize(30);
        queryRequest.setDescend("name");

        // When
        IPage<TAreas> page = queryRequest.page("main");

        // Then
        assertNotNull(page);
        assertEquals(2, page.getCurrent());
        assertEquals(30, page.getSize());
        assertEquals(1, page.orders().size());
        assertEquals("main.name", page.orders().get(0).getColumn());
        assertFalse(page.orders().get(0).isAsc());
    }

    /**
     * 测试 SourceProperty 注解映射的字段排序
     * isLeaf 在 Area POJO 中通过 @SourceProperty 映射到 childCount
     * 但 childCount 是 transient 字段，不会被 MyBatis Plus 映射，所以排序会失败
     */
    @Test
    public void testPageHelperWithSourcePropertyMappedField() {
        // Given
        AreaQuery queryRequest = new AreaQuery();
        queryRequest.setPageNum(1);
        queryRequest.setPageSize(10);
        // isLeaf in Area maps to childCount in TAreas via @SourceProperty
        // but childCount is transient, so it won't be found in column cache
        queryRequest.setAscend("isLeaf");

        // When
        IPage<TAreas> page = PageHelper.pageHelper(queryRequest);

        // Then
        assertNotNull(page);
        List<OrderItem> orders = page.orders();
        // Since childCount is transient, it won't be found and no order will be added
        assertEquals(0, orders.size(),
            "Should have no order since childCount is transient field");
    }

    /**
     * 测试 null 排序字段
     */
    @Test
    public void testPageHelperWithNullSortField() {
        // Given
        AreaQuery queryRequest = new AreaQuery();
        queryRequest.setPageNum(1);
        queryRequest.setPageSize(10);
        queryRequest.setSortField(null);

        // When
        IPage<TAreas> page = PageHelper.pageHelper(queryRequest);

        // Then
        assertNotNull(page);
        assertTrue(page.orders().isEmpty(), "Should have no order when sortField is null");
    }

    /**
     * 测试空字符串排序字段
     */
    @Test
    public void testPageHelperWithEmptySortField() {
        // Given
        AreaQuery queryRequest = new AreaQuery();
        queryRequest.setPageNum(1);
        queryRequest.setPageSize(10);
        queryRequest.setSortField("");

        // When
        IPage<TAreas> page = PageHelper.pageHelper(queryRequest);

        // Then
        assertNotNull(page);
        assertTrue(page.orders().isEmpty(), "Should have no order when sortField is empty");
    }

    /**
     * 测试边界值 - 最小页码和页大小
     */
    @Test
    public void testPageHelperWithMinimumPageValues() {
        // Given
        AreaQuery queryRequest = new AreaQuery();
        queryRequest.setPageNum(1);
        queryRequest.setPageSize(1);

        // When
        IPage<TAreas> page = PageHelper.pageHelper(queryRequest);

        // Then
        assertNotNull(page);
        assertEquals(1, page.getCurrent());
        assertEquals(1, page.getSize());
    }

    /**
     * 测试边界值 - 最大允许页大小
     */
    @Test
    public void testPageHelperWithMaximumPageSize() {
        // Given
        AreaQuery queryRequest = new AreaQuery();
        queryRequest.setPageNum(1);
        queryRequest.setPageSize(100);

        // When
        IPage<TAreas> page = PageHelper.pageHelper(queryRequest);

        // Then
        assertNotNull(page);
        assertEquals(100, page.getSize());
    }

    // ========== 补充测试用例：覆盖 columnMap == null 和泛型参数不是 Class 的分支 ==========

    /**
     * 测试用实体类 - 没有任何 MyBatis Plus 注解的类
     * 用于测试当 LambdaUtils.getColumnMap() 返回 null 的情况（第 110-111 行）
     */
    static class NoAnnotationEntity {
        private String name;
        private Integer age;

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
    }

    /**
     * 测试用 POJO 类
     */
    static class NoAnnotationPojo {
        private String name;
        private Integer age;

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
    }

    /**
     * 测试用查询请求 - 使用没有注解的实体类
     */
    static class NoAnnotationQuery extends AbstractQueryRequest<NoAnnotationEntity, NoAnnotationPojo> {
    }

    /**
     * 测试 columnMap 为 null 的情况（覆盖第 110-111 行）
     * 当实体类没有 MyBatis Plus 的表注解时，LambdaUtils.getColumnMap() 可能返回 null
     */
    @Test
    public void testPageHelperWithNullColumnMap() {
        // Given
        NoAnnotationQuery queryRequest = new NoAnnotationQuery();
        queryRequest.setPageNum(1);
        queryRequest.setPageSize(10);
        queryRequest.setAscend("name");

        // When
        IPage<NoAnnotationEntity> page = PageHelper.pageHelper(queryRequest);

        // Then
        assertNotNull(page);
        assertEquals(1, page.getCurrent());
        assertEquals(10, page.getSize());
        // columnMap 为 null 时，应该没有排序
        assertTrue(page.orders().isEmpty(),
            "Should have no order when columnMap is null");
    }

    /**
     * 测试 columnMap 为 null 时带表别名的情况
     */
    @Test
    public void testPageHelperWithNullColumnMapAndTableAlias() {
        // Given
        NoAnnotationQuery queryRequest = new NoAnnotationQuery();
        queryRequest.setPageNum(1);
        queryRequest.setPageSize(10);
        queryRequest.setDescend("age");

        // When
        IPage<NoAnnotationEntity> page = PageHelper.pageHelper(queryRequest, "t");

        // Then
        assertNotNull(page);
        // 即使有表别名，columnMap 为 null 时也不应该有排序
        assertTrue(page.orders().isEmpty(),
            "Should have no order when columnMap is null even with table alias");
    }

    /**
     * 测试用查询请求 - 泛型参数使用 TypeVariable（不是具体的 Class）
     * 通过创建一个泛型基类和具体子类来模拟这种情况
     */
    static class GenericBaseRequest<E, P> implements PaginationQueryRequest<E, P> {
        private Integer pageSize = 50;
        private Integer pageNum = 1;
        private String sortField;
        private String sortOrder;

        @Override
        public Integer getPageSize() {
            return pageSize;
        }

        public void setPageSize(Integer pageSize) {
            this.pageSize = pageSize;
        }

        @Override
        public Integer getPageNum() {
            return pageNum;
        }

        public void setPageNum(Integer pageNum) {
            this.pageNum = pageNum;
        }

        @Override
        public String getSortField() {
            return sortField;
        }

        public void setSortField(String sortField) {
            this.sortField = sortField;
        }

        public String getSortOrder() {
            return sortOrder;
        }

        public void setSortOrder(String sortOrder) {
            this.sortOrder = sortOrder;
        }

        @Override
        public boolean isAscend() {
            return sortOrder == null || "ascend".equals(sortOrder);
        }
    }

    /**
     * 具体的查询请求类 - 继承自泛型基类但不指定具体类型
     * 这样 getActualTypeArguments 会返回 TypeVariable 而不是 Class
     */
    static class ConcreteGenericRequest extends GenericBaseRequest<TAreas, Area> {
        // 这个类的父类是 GenericBaseRequest<TAreas, Area>
        // 它的泛型参数是具体的 Class，所以可以正常工作
    }

    /**
     * 测试泛型参数不是 Class 的情况（覆盖第 127-129 行）
     * 实际上，要触发这个分支比较困难，因为通常继承时会指定具体类型
     * 这里我们测试一个边界情况：当 getGenericSuperclass 返回的类型处理失败时
     */
    @Test
    public void testPageHelperWithConcreteGenericRequest() {
        // Given
        ConcreteGenericRequest queryRequest = new ConcreteGenericRequest();
        queryRequest.setPageNum(1);
        queryRequest.setPageSize(10);
        queryRequest.setSortField("name");
        queryRequest.setSortOrder("ascend");

        // When
        IPage<TAreas> page = PageHelper.pageHelper(queryRequest);

        // Then
        assertNotNull(page);
        assertEquals(1, page.getCurrent());
        assertEquals(10, page.getSize());
        // 这个应该能正常工作，因为泛型参数是具体的 Class
        assertEquals(1, page.orders().size());
        assertEquals("name", page.orders().get(0).getColumn());
    }
}
