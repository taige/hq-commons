package io.hqwu.commons.mybatisplus;

import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;

/**
 * Created with IntelliJ IDEA for hq-commons-parent
 *
 * @author taige (Wu, Hongqiang)
 * Date: 2021-10-12
 * Time: 12:03
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
    protected Object parse(String json) {
        if (json.startsWith("\"") && json.endsWith("\"")) {
            json = json.substring(1, json.length() - 1);
            json = StringEscapeUtils.unescapeJava(json);
        }
        return super.parse(json);
    }
}
