package io.hqwu.commons.mybatisplus;

import com.baomidou.mybatisplus.core.incrementer.DefaultIdentifierGenerator;
import com.umpay.commons.util.StringUtil;
import io.hqwu.commons.mybatisplus.annotation.TableIdPrefix;

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

    @Override
    public String nextUUID(Object entity) {
        Long id = super.nextId(entity);
        if (entity.getClass().isAnnotationPresent(TableIdPrefix.class)) {
            TableIdPrefix annIdPrefix = entity.getClass().getAnnotation(TableIdPrefix.class);
            String idPrefix = annIdPrefix.value();
            if (StringUtil.isNotBlank(idPrefix)) {
                return idPrefix + id;
            }
        }
        return entity.getClass().getSimpleName() + id;
    }

}
