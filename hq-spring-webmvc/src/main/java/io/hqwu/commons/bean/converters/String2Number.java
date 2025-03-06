package io.hqwu.commons.bean.converters;

import io.hqwu.commons.bean.ValueOf;
import io.hqwu.commons.util.Logger;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA for pp-gopay-fa
 * User: taige
 * Date: 2020/5/5
 * Time: 11:11
 */
public class String2Number implements ValueOf<String, Number> {
    private static final Logger LOGGER = new Logger();

    private ThreadLocal<Map<String, DecimalFormat>> decimalFormatThreadLocal =
            ThreadLocal.withInitial(HashMap::new);

    @Override
    public Number valueOf(String srcValue, Object srcBean, String srcProperty, Object targetBean, String targetProperty, String... params) {
        String fmt = params.length > 0 ? params[0] : "#";
        try {
            return decimalFormatThreadLocal.get().computeIfAbsent(fmt, DecimalFormat::new).parse(srcValue);
        } catch (ParseException e) {
            LOGGER.info(e.toString());
            return null;
        }
    }
    
}
