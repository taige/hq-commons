package io.hqwu.commons.mybatisplus.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.hqwu.commons.mybatisplus.AreaQuery;
import io.hqwu.commons.mybatisplus.JoinQueryWrapper;
import io.hqwu.commons.mybatisplus.MainLambdaQueryWrapper;
import io.hqwu.commons.mybatisplus.PageHelper;
import io.hqwu.commons.mybatisplus.entity.TAreas;
import io.hqwu.commons.mybatisplus.mapper.AreasMapper;
import io.hqwu.commons.mybatisplus.service.AreasService;
import org.springframework.stereotype.Service;

/**
 * <p>
 *  Service Implement
 * </p>
 *
 * @author Wu, Hongqiang
 * @since 2020-05-15
 */
@Service
public class AreasServiceImpl extends ServiceImpl<AreasMapper, TAreas> implements AreasService {

    @Override
    public IPage<TAreas> getAreasByParentId(Integer parentId, AreaQuery queryRequest) {
        MainLambdaQueryWrapper<TAreas> queryWrapper = new MainLambdaQueryWrapper<>("a");
        queryWrapper.eq(parentId != null, TAreas::getParentId, parentId);
        queryWrapper.isNull(parentId == null, TAreas::getParentId);
        queryWrapper.groupBy(TAreas::getId, TAreas::getName, TAreas::getParentId);
        return this.getBaseMapper().selectPageWithChildCount(queryRequest.page("a"), queryWrapper);
    }

    @Override
    public IPage<TAreas> getChildAreasWithStartId(Integer parentId, Integer startId, AreaQuery queryRequest) {
        JoinQueryWrapper<TAreas> queryWrapper = new JoinQueryWrapper<>();
        queryWrapper.eq("p.id", parentId);
        MainLambdaQueryWrapper<TAreas> lambdaQueryWrapper = queryWrapper.lambda("c");
        lambdaQueryWrapper.ge(TAreas::getId, startId);
        return this.getBaseMapper().selectPageWithParentName(PageHelper.pageHelper(queryRequest, "c"), queryWrapper);
    }

    @Override
    public IPage<TAreas> getChildAreasByParentId(Integer parentId, Integer startId, AreaQuery queryRequest) {
        MainLambdaQueryWrapper<TAreas> lambdaQueryWrapper = new MainLambdaQueryWrapper<>("c");
        lambdaQueryWrapper.eq(TAreas::getParentId, parentId);
        lambdaQueryWrapper.ge(TAreas::getId, startId);
        return this.getBaseMapper().selectPageWithParentName(queryRequest.page("c"), lambdaQueryWrapper);
    }

    @Override
    public IPage<TAreas> getChildAreasByParentName(String parentName, Integer startId, AreaQuery queryRequest) {
        MainLambdaQueryWrapper<TAreas> lambdaQueryWrapper = new MainLambdaQueryWrapper<>("c");
        lambdaQueryWrapper.eq(TAreas::getParentName, parentName);
        lambdaQueryWrapper.ge(TAreas::getId, startId);
        return this.getBaseMapper().selectPageWithParentName(queryRequest.page("c"), lambdaQueryWrapper);
    }

}
