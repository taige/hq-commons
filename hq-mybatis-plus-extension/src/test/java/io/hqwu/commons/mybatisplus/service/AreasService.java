package io.hqwu.commons.mybatisplus.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import io.hqwu.commons.mybatisplus.AreaQuery;
import io.hqwu.commons.mybatisplus.entity.TAreas;

/**
 * <p>
 *  Service Interface
 * </p>
 *
 * @author Wu, Hongqiang
 * @since 2020-05-15
 */
public interface AreasService extends IService<TAreas> {

    IPage<TAreas> getAreasByParentId(Integer parentId, AreaQuery queryRequest);

    IPage<TAreas> getChildAreasWithStartId(Integer parentId, Integer startId, AreaQuery queryRequest);

    IPage<TAreas> getChildAreasByParentId(Integer parentId, Integer startId, AreaQuery queryRequest);

    IPage<TAreas> getChildAreasByParentName(String parentName, Integer startId, AreaQuery queryRequest);
}
