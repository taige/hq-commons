package io.hqwu.commons.bean.converters;

import io.hqwu.commons.bean.ValueOf;
import io.hqwu.commons.util.StringUtil;

/**
 * Created with IntelliJ IDEA for pp-gopay-fa
 * User: taige
 * Date: 2020/5/7
 * Time: 01:19
 */
public class Boolean2Integer implements ValueOf<Boolean, Integer> {

    /**
     *
     * @param srcValue       original value
     * @param srcBean        original bean object
     * @param srcProperty    original property name
     * @param targetBean     target bean object
     * @param targetProperty target property name
     * @param params         format: ["INT_IF_TRUE", "INT_IF_FALSE"]
     * @return               (INT_IF_TRUE or 1) if srcValue true, else (INT_IF_FALSE or 0)
     */
    @Override
    public Integer valueOf(Boolean srcValue, Object srcBean, String srcProperty, Object targetBean, String targetProperty, String... params) {
        if (srcValue) {
            if (params.length > 0 && StringUtil.isNotBlank(params[0])) {
                try {
                    return Integer.parseInt(params[0]);
                } catch (NumberFormatException e) {
                }
            }
            if (params.length > 1 && StringUtil.isNotBlank(params[1])) {
                // if false -> 1, then true -> 0
                try {
                    if (Integer.parseInt(params[1]) == 1) {
                        return 0;
                    }
                } catch (NumberFormatException e) {
                }
            }
            return 1;
        } else {
            if (params.length > 1 && StringUtil.isNotBlank(params[1])) {
                try {
                    return Integer.parseInt(params[1]);
                } catch (NumberFormatException e) {
                }
            }
            if (params.length > 0 && StringUtil.isNotBlank(params[0])) {
                // if true -> 0, then false -> 1
                try {
                    if (Integer.parseInt(params[0]) == 0) {
                        return 1;
                    }
                } catch (NumberFormatException e) {
                }
            }
            return 0;
        }
    }

}
