package com.umpay.commons.util;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.junit.jupiter.api.Test;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Description: DateTimeUtil单元测试类
 * 
 * @author: shenjianlin <a href="mailto:ustbsjl@gmail.com">ustbsjl@gmail.com</a> <br>
 *          QQ: 79043549
 * @version: 1.0 2013-8-12
 * @history:
 */

public class TimeUtilTest {
    private static final Logger LOGGER = new Logger();

    @Test
    public void testGetCurrentDate8() {
        assertNotNull(TimeUtil.getCurrentDate8());
        assertEquals(date8(), TimeUtil.getCurrentDate8());
    }

    @Test
    public void testGetCurrentDate6() {
        assertNotNull(TimeUtil.getCurrentDate6());
        assertEquals(date8().substring(2), TimeUtil.getCurrentDate6());
    }

    @Test
    public void testGetCurrentDateTime14() {
        assertNotNull(TimeUtil.getCurrentDateTime14());
        assertEquals(time14(), TimeUtil.getCurrentDateTime14());
    }

    @Test
    public void testGetCurrentTime6() {
        assertNotNull(TimeUtil.getCurrentTime6());
        assertEquals(time6(), TimeUtil.getCurrentTime6());
    }

    @Test
    public void testGetCurrentTimeString() {
        assertNotNull(TimeUtil.getCurrentTimeString(null));
        assertEquals(time14(), TimeUtil.getCurrentTimeString(null));
        assertEquals(date8(),
                TimeUtil.getCurrentTimeString(TimeUtil.PATTERN_YYYYMMDD));
        assertEquals(time6(),
                TimeUtil.getCurrentTimeString(TimeUtil.PATTERN_HHMMSS));
    }

    @Test
    public void testDaysOffset() {
        assertNull(TimeUtil.daysOffset(null, null, 0));
        assertEquals("", TimeUtil.daysOffset("", null, 0));
        assertEquals("20130813", TimeUtil.daysOffset("20130812", null, 1));// 后移
        assertEquals("20130811", TimeUtil.daysOffset("20130812", null, -1));// 前移
        assertEquals("20130301", TimeUtil.daysOffset("20130228", null, 1));// 2月
        assertEquals("20130101", TimeUtil.daysOffset("20121229", null, 3));// 跨年
        assertEquals("130101", TimeUtil.daysOffset("121229", "yyMMdd", 3));// 跨年,指定格式
    }

    @Test
    public void testSleepSec() {
        long start = System.currentTimeMillis();
        long sec = 1;
        TimeUtil.sleepSec(sec, "xxx");
        long end = System.currentTimeMillis();
        assertTrue(end - start >= sec * 1000, "休眠时间计算错误");
    }

    @Test
    public void testSleepMilliSec() {
        long start = System.nanoTime();
        long milliSec = 123;
        TimeUtil.sleepMilliSec(milliSec, "xxx");
        long end = System.nanoTime();
        assertTrue(end - start >= milliSec * 1000, "休眠时间计算错误");
    }

    @Test
    public void testSleepNanoSec() {
        long nanoSec = 1230000100L;
        long start = System.nanoTime();
        TimeUtil.sleepNanoSec(nanoSec, "xxx");
        long end = System.nanoTime();
        assertTrue((end - start) >= (nanoSec / 1.5), "休眠时间计算错误");// 纳秒的休眠不是非常精确
    }
    String time6(){
        return new SimpleDateFormat("HHmmss").format(new Date());
    }
    String time14(){
        return new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
    }
    String date8(){
        return new SimpleDateFormat("yyyyMMdd").format(new Date());
    }

    @Test
    public void testFormat(){
       assertEquals(date8(), TimeUtil.format(new Date(), "yyyyMMdd"));
       assertEquals(date8(), TimeUtil.formatDate8(new Date()));
       assertEquals(time6(), TimeUtil.formatTime6(new Date()));
       assertEquals(time14(), TimeUtil.formatDateTime14(new Date()));

       assertEquals(date8(), TimeUtil.format(Calendar.getInstance(), "yyyyMMdd"));
       assertEquals(date8(), TimeUtil.formatDate8(Calendar.getInstance()));
       assertEquals(time6(), TimeUtil.formatTime6(Calendar.getInstance()));
       assertEquals(time14(), TimeUtil.formatDateTime14(Calendar.getInstance()));

       assertEquals(date8(), TimeUtil.format(System.currentTimeMillis(), "yyyyMMdd"));
       assertEquals(date8(), TimeUtil.formatDate8(System.currentTimeMillis()));
       assertEquals(time6(), TimeUtil.formatTime6(System.currentTimeMillis()));
       assertEquals(time14(), TimeUtil.formatDateTime14(System.currentTimeMillis()));
    }

    private static final String hourOffset(String srcDatetime, String pattern, int offset) {
        if (StringUtils.isBlank(pattern)) {// 默认格式
            pattern = TimeUtil.PATTERN_YYYYMMDDHHMMSS;
        }
        try {
            Date before = TimeUtil.parseDateStrictly(srcDatetime, pattern);// 解析成日期类型
            Date after = TimeUtil.addHours(before, offset);// 偏移计算
            String afterFormat = DateFormatUtils.format(after, pattern);
            return afterFormat;
        } catch (ParseException e) {
            throw new IllegalArgumentException("解析日期异常", e);
        }
    }

    @Test
    public void test_TimeZone() {
        long ts = System.currentTimeMillis();
        String pattern = "yyyy/MM/dd.HH:mm:ss";
        String gmtString = DateFormatUtils.format(ts, pattern, TimeZone.getTimeZone("GMT"));
        for (int i = -14; i <= 12; i++) {
            String tzId = (i <= 0 ? "GMT" : "GMT+") + i;
            assertTrue(TimeUtil.isSupportTimeZone(tzId));
            String timeString = TimeUtil.getTimeString(ts, pattern, tzId);
            assertEquals(hourOffset(gmtString, pattern, i), timeString);
            System.out.println("timezone [" + tzId + "] test ok: " + timeString);
        }
    }

    @Test
    public void testTimeZone() {
        Map<String, String> map = TimeUtil.SUPPORT_TIMEZONE;
        for (Map.Entry<String, String> entry : map.entrySet()) {
            String zondId = entry.getKey();

            LOGGER.info(zondId);
            LOGGER.info(TimeUtil.getCurrentDate14(zondId), " ", TimeUtil.getCurrentDate8(zondId), " ", TimeUtil.getCurrentTime6(zondId));

        }
    }

    @Test
    public void testTime() {
        long l = System.currentTimeMillis();
        Map<String, String> map = TimeUtil.SUPPORT_TIMEZONE;
        for (Map.Entry<String, String> entry : map.entrySet()) {
            String zondId = entry.getKey();

            LOGGER.info("l:", l, " zondId:", zondId);
            LOGGER.info(TimeUtil.getDate14(l, zondId), " ", TimeUtil.getDate8(l, zondId), " ", TimeUtil.getTime6(l, zondId));

        }
    }
}
