package io.hqwu.commons.mybatisplus.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import io.hqwu.commons.mybatisplus.entity.TAreas;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * <p>
 *  Mapper Interface
 * </p>
 *
 * @author Wu, Hongqiang
 * @since 2020-05-15
 */
public interface AreasMapper extends BaseMapper<TAreas> {

    @Select("SELECT a.id, a.name, a.parent_id, count(b.id) AS child_count FROM `gpf_areas` a " +
            "LEFT JOIN `gpf_areas` b ON b.parent_id = a.id ${ew.customSqlSegment}")
    IPage<TAreas> selectPageWithChildCount(IPage<TAreas> page, @Param(Constants.WRAPPER) Wrapper<TAreas> queryWrapper);

    @Select("SELECT c.id, c.name, c.parent_id, p.name AS parent_name FROM `gpf_areas` c " +
            "LEFT JOIN `gpf_areas` p ON c.parent_id = p.id ${ew.customSqlSegment}")
    IPage<TAreas> selectPageWithParentName(IPage<TAreas> page, @Param(Constants.WRAPPER) Wrapper<TAreas> queryWrapper);

}
