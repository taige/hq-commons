package io.hqwu.commons.mybatisplus;

import com.baomidou.mybatisplus.core.incrementer.DefaultIdentifierGenerator;
import com.baomidou.mybatisplus.core.toolkit.Sequence;
import io.hqwu.commons.mybatisplus.annotation.TableIdPrefix;
import io.hqwu.commons.util.StringUtil;

/**
 * Created with IntelliJ IDEA for hq-commons-parent
 *
 *  String型雪花算法(Snowflake)ID生成器
 *      默认：Entity类名(simpleName) + snowflake_id
 *      Entity 如果有{@link TableIdPrefix}，就用其指定的value做为id前缀
 *
 * @author taige (Wu, Hongqiang)
 * Date: 2021-04-04
 * Time: 10:55 a.m.
 */
public class SnowflakeIdGenerator extends DefaultIdentifierGenerator {

    public SnowflakeIdGenerator() {
        super(new Sequence(null));
    }

    public SnowflakeIdGenerator(Sequence sequence) {
        super(sequence);
    }

    @Override
    public String nextUUID(Object entity) {
        Long id = super.nextId(entity);
        if (entity == null) {
            return String.valueOf(id);
        }
        if (entity instanceof String) {
            return ((String) entity) + id;
        }
        if (entity.getClass().isAnnotationPresent(TableIdPrefix.class)) {
            TableIdPrefix annIdPrefix = entity.getClass().getAnnotation(TableIdPrefix.class);
            String idPrefix = annIdPrefix.value();
            if (StringUtil.isNotBlank(idPrefix)) {
                return idPrefix + id;
            }
        }
        return entity.getClass().getSimpleName().substring(0, 2) + id;
    }

}
