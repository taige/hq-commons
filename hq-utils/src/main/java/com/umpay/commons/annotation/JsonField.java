package com.umpay.commons.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Controller中方法把json根据field拆分成多个对象做为参数
 *
 * Use the fields in the json object as separated parameters of the controller method,
 *  instead of using the json object as ONE parameter, as in the @RequestBody annotation.
 *
 * @author Wangyang Liu
 * Date: 2018/08/27
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface JsonField {
    /**
     * 解析时用到的JSON的key
     * field in json used as a parameter, use parameter name if not specified
     */
    String value() default "";

    /**
     * 是否必须出现的参数
     * indicate whether it is a required parameter
     */
    boolean required() default true;

    /**
     * 当value的值或者参数名不匹配时，是否允许解析最外层属性到该对象
     *
     * indicate whether parse the whole json object as parameter if the specific field not found
     *  (if it's a require parameter)
     */
    boolean parseAllFields() default true;
}