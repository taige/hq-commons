package io.hqwu.commons.mybatisplus;

import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;

/**
 * 基于 Jackson 的 MyBatis-Plus 类型处理器，旨在增强对 H2 数据库的兼容性。
 * <p>
 * 该类扩展自 {@link JacksonTypeHandler}，用于在 MyBatis 映射中实现 Java 对象与数据库 JSON 字段的自动转换。
 * 特别解决了 H2 数据库在处理 JSON 类型时可能出现的带引号或转义字符的字符串解析问题，确保在单元测试或 H2 环境下 JSON 数据能被正确还原。
 * </p>
 *
 * @author taige (Wu, Hongqiang)
 * @since 2021-10-12
 */
@MappedTypes({Object.class})
@MappedJdbcTypes(JdbcType.VARCHAR)
public class H2CompatibleJacksonTypeHandler extends JacksonTypeHandler {

    private final Class<?> type;

    public H2CompatibleJacksonTypeHandler(Class<?> type) {
        super(type);
        this.type = type;
    }

    @Override
    public Object parse(String json) {
        if (json.startsWith("\"") && json.endsWith("\"")) {
            json = json.substring(1, json.length() - 1);
            json = StringEscapeUtils.unescapeJava(json);
        }
        return super.parse(json);
    }
}
