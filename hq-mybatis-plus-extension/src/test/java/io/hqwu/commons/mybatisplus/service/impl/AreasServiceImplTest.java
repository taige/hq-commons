package io.hqwu.commons.mybatisplus.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.hqwu.commons.mybatisplus.AreaQuery;
import io.hqwu.commons.mybatisplus.TestApplication;
import io.hqwu.commons.mybatisplus.entity.TAreas;
import io.hqwu.commons.mybatisplus.service.AreasService;
import lombok.CustomLog;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;

import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Created with IntelliJ IDEA for pp-gopay-fa
 *
 * @author taige (Wu, Hongqiang)
 * Date: 2020/5/15
 * Time: 22:13
 */
@CustomLog
@SpringBootTest(classes = TestApplication.class)
public class AreasServiceImplTest {

    @Autowired
    private AreasService areasService;

    @Test
    @Sql(statements = "DELETE FROM `gpf_areas` WHERE id = 3")
    void test_getProvicesTop5() {
        IPage<TAreas> iPage = areasService.getAreasByParentId(null,
                new AreaQuery().setPageSize(5).setAscend("id"));
        assertEquals(5, iPage.getRecords().size());
        assertEquals(32, iPage.getTotal());
        Integer[] ids = iPage.getRecords().stream().map(TAreas::getId)
                .collect(Collectors.toList()).toArray(new Integer[] {});
        String[] names = iPage.getRecords().stream().map(TAreas::getName)
                .collect(Collectors.toList()).toArray(new String[] {});
        assertArrayEquals(new String[] {"北京", "上海", "重庆", "河北", "山西"}, names);
        iPage.getRecords().forEach(a -> {
            assertNull(a.getParentId());
            assertNotEquals(0, a.getChildCount());
        });
    }

    @Test
    void test_getBeijingDistrict() {
        IPage<TAreas> iPage = areasService.getAreasByParentId(1,
                new AreaQuery().setPageSize(5).setAscend("id"));
        assertEquals(5, iPage.getRecords().size());
        assertEquals(18, iPage.getTotal());
        Integer[] ids = iPage.getRecords().stream().map(TAreas::getId)
                .collect(Collectors.toList()).toArray(new Integer[] {});
        assertArrayEquals(new Integer[] {72,2800,2801,2802,2803}, ids);
        String[] names = iPage.getRecords().stream().map(TAreas::getName)
                .collect(Collectors.toList()).toArray(new String[] {});
        assertArrayEquals(new String[] {"朝阳区", "海淀区", "西城区", "东城区", "崇文区"}, names);
        iPage.getRecords().forEach(a -> {
            assertEquals(1, a.getParentId());
            assertNotEquals(0, a.getChildCount());
        });
    }

    @Test
    void test_getBeijingChaoyangStreet() {
        IPage<TAreas> iPage = areasService.getAreasByParentId(72,
                new AreaQuery().setPageSize(5).setAscend("id"));
        assertEquals(5, iPage.getRecords().size());
        assertEquals(7, iPage.getTotal());
        Integer[] ids = iPage.getRecords().stream().map(TAreas::getId)
                .collect(Collectors.toList()).toArray(new Integer[] {});
        assertArrayEquals(new Integer[] {2799,2819,2839,2840,4137}, ids);
        String[] names = iPage.getRecords().stream().map(TAreas::getName)
                .collect(Collectors.toList()).toArray(new String[] {});
        assertArrayEquals(new String[] {"三环以内", "三环到四环之间", "四环到五环之间", "五环到六环之间", "管庄地区"}, names);
        iPage.getRecords().forEach(a -> {
            assertEquals(72, a.getParentId());
            assertEquals(0, a.getChildCount());
        });
    }

    @Test
    void test_getUnknowArea() {
        IPage<TAreas> iPage = areasService.getAreasByParentId(60072,
                new AreaQuery().setPageSize(5).setAscend("id"));
        assertEquals(0, iPage.getRecords().size());
        assertEquals(0, iPage.getTotal());
    }

    @Test
    void test_getChildAreasWithStartId() {
        IPage<TAreas> iPage = areasService.getChildAreasWithStartId(1, 1000,
                new AreaQuery().setPageSize(5).setAscend("id"));
        iPage.getRecords().forEach(a -> {
            assertEquals(1, a.getParentId());
            assertEquals("北京", a.getParentName());
            LOGGER.debug(a);
        });
        assertEquals(5, iPage.getRecords().size());
        assertEquals(17, iPage.getTotal());
        Integer[] ids = iPage.getRecords().stream().map(TAreas::getId)
                .collect(Collectors.toList()).toArray(new Integer[] {});
        assertArrayEquals(new Integer[] {2800,2801,2802,2803,2804}, ids);
        String[] names = iPage.getRecords().stream().map(TAreas::getName)
                .collect(Collectors.toList()).toArray(new String[] {});
        assertArrayEquals(new String[] {"海淀区", "西城区", "东城区", "崇文区", "宣武区"}, names);
    }

    @Test
    void test_getChildAreasByParentId() {
        IPage<TAreas> iPage = areasService.getChildAreasByParentId(1, 1000,
                new AreaQuery().setPageSize(5).setAscend("id"));
        iPage.getRecords().forEach(a -> {
            assertEquals(1, a.getParentId());
            assertEquals("北京", a.getParentName());
            LOGGER.debug(a);
        });
        assertEquals(5, iPage.getRecords().size());
        assertEquals(17, iPage.getTotal());
        Integer[] ids = iPage.getRecords().stream().map(TAreas::getId)
                .collect(Collectors.toList()).toArray(new Integer[] {});
        assertArrayEquals(new Integer[] {2800,2801,2802,2803,2804}, ids);
        String[] names = iPage.getRecords().stream().map(TAreas::getName)
                .collect(Collectors.toList()).toArray(new String[] {});
        assertArrayEquals(new String[] {"海淀区", "西城区", "东城区", "崇文区", "宣武区"}, names);
    }

    @Test
    void test_getChildAreasByParentName() {
        IPage<TAreas> iPage = areasService.getChildAreasByParentName("北京", 1000,
                new AreaQuery().setPageSize(5).setAscend("id"));
        iPage.getRecords().forEach(a -> {
            assertEquals(1, a.getParentId());
            assertEquals("北京", a.getParentName());
            LOGGER.debug(a);
        });
        assertEquals(5, iPage.getRecords().size());
        assertEquals(17, iPage.getTotal());
        Integer[] ids = iPage.getRecords().stream().map(TAreas::getId)
                .collect(Collectors.toList()).toArray(new Integer[] {});
        assertArrayEquals(new Integer[] {2800,2801,2802,2803,2804}, ids);
        String[] names = iPage.getRecords().stream().map(TAreas::getName)
                .collect(Collectors.toList()).toArray(new String[] {});
        assertArrayEquals(new String[] {"海淀区", "西城区", "东城区", "崇文区", "宣武区"}, names);
    }
}