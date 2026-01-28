package io.hqwu.commons.mybatisplus;

import com.baomidou.mybatisplus.core.handlers.MybatisEnumTypeHandler;
import io.hqwu.commons.util.Logger;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.EnumTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 兼容性枚举类型处理器。
 * <p>
 * 该类用于协调 MyBatis-Plus 与原生 MyBatis 的枚举处理逻辑。
 * 它会优先尝试使用 {@link MybatisEnumTypeHandler} 处理符合 MyBatis-Plus 规范的枚举（如使用 {@code @EnumValue} 或实现 {@code IEnum} 接口）；
 * 若不符合规范，则自动回退至 MyBatis 原生的 {@link EnumTypeHandler} 处理。
 * </p>
 *
 * @param <E> 枚举类型
 * @author taige (Wu, Hongqiang)
 * @since 2021-10-19
 * @see MybatisEnumTypeHandler
 * @see EnumTypeHandler
 * @see BaseTypeHandler
 */
public class CompatibleEnumTypeHandler<E extends Enum<E>> extends BaseTypeHandler<E> {
    private static final Logger LOGGER = new Logger();

    private BaseTypeHandler<E> enumTypeHandler;

    public CompatibleEnumTypeHandler(Class<E> type) {
        try {
            enumTypeHandler = new MybatisEnumTypeHandler<>(type);
        } catch (IllegalArgumentException e) {
            LOGGER.debug("%s, use %s to handle %s", e.getMessage(), EnumTypeHandler.class.getName(), type.getName());
            enumTypeHandler = new EnumTypeHandler<>(type);
        }
    }

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, E parameter, JdbcType jdbcType) throws SQLException {
        enumTypeHandler.setNonNullParameter(ps, i, parameter, jdbcType);
    }

    @Override
    public E getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return enumTypeHandler.getNullableResult(rs, columnName);
    }

    @Override
    public E getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return enumTypeHandler.getNullableResult(rs, columnIndex);
    }

    @Override
    public E getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return enumTypeHandler.getNullableResult(cs, columnIndex);
    }
}
