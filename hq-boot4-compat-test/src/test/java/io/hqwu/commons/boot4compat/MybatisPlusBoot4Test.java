package io.hqwu.commons.boot4compat;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import io.hqwu.commons.boot4compat.entity.Area;
import io.hqwu.commons.boot4compat.mapper.AreaMapper;
import io.hqwu.commons.cp.HqcpDataSourceBoot;
import io.hqwu.commons.mybatisplus.CommonFieldsFiller;
import io.hqwu.commons.mybatisplus.MainLambdaQueryWrapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.sql.DataSource;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SB4 + mybatis-plus-spring-boot4-starter 3.5.17 + hq-cp-boot4-starter 下，
 * hq-mybatis-plus-extension（编译于 mybatis-plus 3.5.11）能否正常工作。
 */
@SpringBootTest
class MybatisPlusBoot4Test {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private AreaMapper areaMapper;

    @Autowired
    private MetaObjectHandler metaObjectHandler;

    @Test
    void dataSourceIsHqcpFromBoot4Starter() {
        assertThat(dataSource).isInstanceOf(HqcpDataSourceBoot.class);
    }

    @Test
    void commonFieldsFillerIsPickedUpAsMetaObjectHandler() {
        assertThat(metaObjectHandler).isInstanceOf(CommonFieldsFiller.class);
    }

    @Test
    void baseMapperWorksThroughHqcp() {
        List<Area> all = areaMapper.selectList(null);
        assertThat(all).hasSize(3);
    }

    @Test
    void mainLambdaQueryWrapperPrefixesMainTableAlias() {
        MainLambdaQueryWrapper<Area> wrapper = new MainLambdaQueryWrapper<>("a");
        wrapper.eq(Area::getParentId, 1);

        List<Area> children = areaMapper.selectWithAlias(wrapper);

        assertThat(children).extracting(Area::getName).containsExactlyInAnyOrder("Haidian", "Chaoyang");
    }
}
