package io.hqwu.commons.bean.converters;

import io.hqwu.commons.bean.ValueOf;
import org.apache.commons.lang3.StringUtils;

public class String2MaskString implements ValueOf<String, String> {

    @Override
    public String valueOf(String srcValue, Object srcBean, String srcProperty, Object targetBean, String targetProperty, String... params) {
        if (srcValue==null) return null;
        if (srcValue.length() < 5) return srcValue;
        return StringUtils.repeat("*", srcValue.length() - 4) + StringUtils.right(srcValue, 4);
    }

}