package com.umpay.commons.util.converters;

import com.umpay.commons.util.ValueOf;

import java.math.BigDecimal;

/**
 * Created with IntelliJ IDEA for pp-gopay-fa
 * User: taige
 * Date: 2020/5/9
 * Time: 23:53
 */
public class Double2BigDecimal implements ValueOf<Double, BigDecimal> {

    @Override
    public BigDecimal valueOf(Double srcValue, Object srcBean, String srcProperty, Object targetBean, String targetProperty, String... params) {
        return BigDecimal.valueOf(srcValue);
    }

}
