package io.hqwu.commons.boot4compat.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@TableName("t_area")
public class Area {

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    private String name;

    private Integer parentId;
}
