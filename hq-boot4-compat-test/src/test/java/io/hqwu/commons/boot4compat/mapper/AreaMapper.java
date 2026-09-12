package io.hqwu.commons.boot4compat.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import io.hqwu.commons.boot4compat.entity.Area;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface AreaMapper extends BaseMapper<Area> {

    /** 带表别名的查询，配合 MainLambdaQueryWrapper 自动加的 "a." 前缀 */
    @Select("SELECT a.id, a.name, a.parent_id FROM t_area a ${ew.customSqlSegment}")
    List<Area> selectWithAlias(@Param(Constants.WRAPPER) Wrapper<Area> wrapper);
}
