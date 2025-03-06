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
 * Created with IntelliJ IDEA for qrcode-api-server
 *
 * @author taige (Wu, Hongqiang)
 * Date: 2021-10-19
 * Time: 20:43
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
