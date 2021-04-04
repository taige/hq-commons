package io.hqwu.commons.mybatisplus.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.hqwu.commons.mybatisplus.annotation.JoinColumn;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * <p>
 * 
 * </p>
 *
 * @author Wu, Hongqiang
 * @since 2020-05-15
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("t_areas")
public class TAreas implements Serializable {

    @TableId
    private Integer id;

    private String name;

    private Integer parentId;

    private transient Integer childCount;

    @JoinColumn("p.name")
    private transient String parentName;

    @JoinColumn("p.id")
    private transient Integer parId;
}
