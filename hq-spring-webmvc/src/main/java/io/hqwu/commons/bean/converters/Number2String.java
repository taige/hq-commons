package io.hqwu.commons.bean.converters;

import io.hqwu.commons.bean.ValueOf;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA for pp-gopay-fa
 * User: taige
 * Date: 2020/5/5
 * Time: 11:11
 */
public class Number2String implements ValueOf<Number, String> {

    private ThreadLocal<Map<String, DecimalFormat>> decimalFormatThreadLocal =
            ThreadLocal.withInitial(HashMap::new);

    @Override
    public String valueOf(Number srcValue, Object srcBean, String srcProperty, Object targetBean, String targetProperty, String... params) {
        String fmt = params.length > 0 ? params[0] : "#.#";
        return decimalFormatThreadLocal.get().computeIfAbsent(fmt, DecimalFormat::new).format(srcValue);
    }
    
}
