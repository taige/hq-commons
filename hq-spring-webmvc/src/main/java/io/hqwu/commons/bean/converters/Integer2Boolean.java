package io.hqwu.commons.bean.converters;

import io.hqwu.commons.bean.ValueOf;
import io.hqwu.commons.util.Logger;
import io.hqwu.commons.util.StringUtil;

import java.util.Arrays;

/**
 * Created with IntelliJ IDEA for pp-gopay-fa
 * User: taige
 * Date: 2020/5/4
 * Time: 22:24
 */
public class Integer2Boolean implements ValueOf<Integer, Boolean> {
    private static final Logger LOGGER = new Logger();

    /**
     *
     * @param srcValue       original value
     * @param srcBean        original bean object
     * @param srcProperty    original property name
     * @param targetBean     target bean object
     * @param targetProperty target property name
     * @param params         format: ["true", "int1_if_true", "int2_if_true" ...]
     *                            or ["false", "int1_if_false", "int2_if_false" ...]
     * @return               depends on params if params available, else true if srcValue == 1 or false
     */
    @Override
    public Boolean valueOf(Integer srcValue, Object srcBean, String srcProperty, Object targetBean, String targetProperty, String... params) {
        if (params.length > 1) {
            if (StringUtil.isNotBlank(params[0])) {
                if (params[0].equalsIgnoreCase("true")) {
                    return anyMatch(Arrays.copyOfRange(params, 1, params.length), srcValue);
                } else if (params[0].equalsIgnoreCase("false")) {
                    return ! anyMatch(Arrays.copyOfRange(params, 1, params.length), srcValue);
                } else {
                    LOGGER.debug("unknown flag of bool: ", params[0]);
                }
            }
        }
        return srcValue == 1;
    }

    private Boolean anyMatch(String[] params, Integer srcValue) {
        return Arrays.stream(params).map(s -> {
            try {
                return Integer.valueOf(s);
            } catch (NumberFormatException e) {
                LOGGER.debug(e.toString());
                return null;
            }
        }).anyMatch(integer -> integer != null && integer.equals(srcValue));
    }
    
}
