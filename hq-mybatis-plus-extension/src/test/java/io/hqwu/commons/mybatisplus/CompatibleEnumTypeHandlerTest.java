package io.hqwu.commons.mybatisplus;

import com.baomidou.mybatisplus.annotation.IEnum;
import org.apache.ibatis.type.JdbcType;
import org.junit.jupiter.api.Test;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

/**
 * Test cases for {@link CompatibleEnumTypeHandler}
 *
 * <p>本测试只关注 CompatibleEnumTypeHandler 自己的代码：
 * <ul>
 *   <li>构造函数：尝试创建 MybatisEnumTypeHandler，失败则创建 EnumTypeHandler</li>
 *   <li>委托方法：setNonNullParameter 和 getNullableResult(3个重载) 都直接委托</li>
 *   <li>异常处理：SQLException 直接向上传播，不捕获</li>
 * </ul>
 *
 * @author Wu, Hongqiang
 * @since 2026/1/25
 */
public class CompatibleEnumTypeHandlerTest {

    /**
     * 测试用枚举 - 实现了 IEnum 接口，应使用 MybatisEnumTypeHandler
     */
    enum StatusEnum implements IEnum<Integer> {
        ACTIVE(1),
        INACTIVE(0),
        DELETED(-1);

        private final Integer value;

        StatusEnum(Integer value) {
            this.value = value;
        }

        @Override
        public Integer getValue() {
            return value;
        }
    }

    /**
     * 测试用枚举 - 普通枚举，应使用 EnumTypeHandler
     */
    enum SimpleEnum {
        OPTION_A,
        OPTION_B,
        OPTION_C
    }

    // ========== 构造函数测试 ==========

    /**
     * 测试构造函数 - IEnum 枚举应成功创建（内部使用 MybatisEnumTypeHandler）
     */
    @Test
    public void testConstructorWithIEnumType() {
        // When - 构造函数应该成功创建 MybatisEnumTypeHandler
        CompatibleEnumTypeHandler<StatusEnum> handler = new CompatibleEnumTypeHandler<>(StatusEnum.class);

        // Then - 构造函数不抛出异常
        assertNotNull(handler);
    }

    /**
     * 测试构造函数 - 普通枚举应成功创建（内部捕获异常并创建 EnumTypeHandler）
     */
    @Test
    public void testConstructorWithSimpleEnumType() {
        // When - 构造函数应该捕获 IllegalArgumentException 并创建 EnumTypeHandler
        CompatibleEnumTypeHandler<SimpleEnum> handler = new CompatibleEnumTypeHandler<>(SimpleEnum.class);

        // Then - 构造函数不抛出异常
        assertNotNull(handler);
    }

    // ========== setNonNullParameter 委托测试 ==========

    /**
     * 测试 setNonNullParameter - 方法正确委托给底层 handler
     */
    @Test
    public void testSetNonNullParameterDelegation() throws SQLException {
        // Given
        CompatibleEnumTypeHandler<StatusEnum> handler = new CompatibleEnumTypeHandler<>(StatusEnum.class);
        PreparedStatement ps = mock(PreparedStatement.class);

        // When - 调用委托方法
        handler.setNonNullParameter(ps, 1, StatusEnum.ACTIVE, JdbcType.INTEGER);

        // Then - 验证底层 handler 调用了 PreparedStatement 的 setObject 方法（委托成功）
        // 使用 times(1) 验证调用了一次，但不限定参数数量
        verify(ps, times(1)).setObject(anyInt(), any(), anyInt());
    }

    // ========== getNullableResult 委托测试 ==========

    /**
     * 测试 getNullableResult(ResultSet, String) - 方法正确委托
     */
    @Test
    public void testGetNullableResultByColumnNameDelegation() throws SQLException {
        // Given
        CompatibleEnumTypeHandler<StatusEnum> handler = new CompatibleEnumTypeHandler<>(StatusEnum.class);
        ResultSet rs = mock(ResultSet.class);

        // When - 调用委托方法
        handler.getNullableResult(rs, "status");

        // Then - 验证底层 handler 访问了 ResultSet 的 getObject 方法（委托成功）
        // MybatisEnumTypeHandler 会调用 getObject(String, Class)
        verify(rs, times(1)).getObject(eq("status"), any(Class.class));
    }

    /**
     * 测试 getNullableResult(ResultSet, int) - 方法正确委托
     */
    @Test
    public void testGetNullableResultByColumnIndexDelegation() throws SQLException {
        // Given
        CompatibleEnumTypeHandler<StatusEnum> handler = new CompatibleEnumTypeHandler<>(StatusEnum.class);
        ResultSet rs = mock(ResultSet.class);

        // When - 调用委托方法
        handler.getNullableResult(rs, 1);

        // Then - 验证底层 handler 访问了 ResultSet 的 getObject 方法（委托成功）
        // MybatisEnumTypeHandler 会调用 getObject(int, Class)
        verify(rs, times(1)).getObject(eq(1), any(Class.class));
    }

    /**
     * 测试 getNullableResult(CallableStatement, int) - 方法正确委托
     */
    @Test
    public void testGetNullableResultFromCallableStatementDelegation() throws SQLException {
        // Given
        CompatibleEnumTypeHandler<StatusEnum> handler = new CompatibleEnumTypeHandler<>(StatusEnum.class);
        CallableStatement cs = mock(CallableStatement.class);

        // When - 调用委托方法
        handler.getNullableResult(cs, 1);

        // Then - 验证底层 handler 访问了 CallableStatement 的 getObject 方法（委托成功）
        // MybatisEnumTypeHandler 会调用 getObject(int, Class)
        verify(cs, times(1)).getObject(eq(1), any(Class.class));
    }

    // ========== SQLException 传播测试 ==========

    /**
     * 测试 setNonNullParameter - SQLException 正确传播
     * CompatibleEnumTypeHandler 代码中没有 try-catch，异常应该直接向上传播
     */
    @Test
    public void testSetNonNullParameterPropagatesSQLException() throws SQLException {
        // Given
        CompatibleEnumTypeHandler<StatusEnum> handler = new CompatibleEnumTypeHandler<>(StatusEnum.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        // 模拟两种可能的 setObject 调用都抛出异常
        doThrow(new SQLException("Database error")).when(ps).setObject(anyInt(), any(), anyInt());
        doThrow(new SQLException("Database error")).when(ps).setObject(anyInt(), any());

        // When & Then - SQLException 应该被传播（代码中没有 try-catch）
        assertThrows(SQLException.class, () ->
            handler.setNonNullParameter(ps, 1, StatusEnum.ACTIVE, JdbcType.INTEGER)
        );
    }
}
