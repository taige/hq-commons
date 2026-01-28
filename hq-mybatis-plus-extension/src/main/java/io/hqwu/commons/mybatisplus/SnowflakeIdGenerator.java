package io.hqwu.commons.mybatisplus;

import com.baomidou.mybatisplus.core.incrementer.DefaultIdentifierGenerator;
import com.baomidou.mybatisplus.core.toolkit.Sequence;
import io.hqwu.commons.mybatisplus.annotation.TableIdPrefix;
import io.hqwu.commons.util.StringUtil;

/**
 * 基于雪花算法（Snowflake）的字符串类型 ID 生成器。
 *
 * <p>该类继承自 {@link DefaultIdentifierGenerator}，旨在为 MyBatis Plus 提供支持自定义前缀的字符串 ID 生成能力。</p>
 *
 * <p>主要功能特性：</p>
 * <ul>
 *   <li>支持通过 {@link TableIdPrefix} 注解为不同实体配置个性化 ID 前缀。</li>
 *   <li>默认规则：若无注解，自动截取实体类名的前两个字符作为前缀。</li>
 *   <li>兼容性：支持直接传入字符串前缀或处理无实体场景。</li>
 * </ul>
 *
 * @author taige (Wu, Hongqiang)
 * @since 2021-04-04
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
