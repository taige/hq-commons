package io.hqwu.commons.mybatisplus.annotation;

import java.lang.annotation.*;

/**
 * Created with IntelliJ IDEA for hq-commons-parent
 *
 * @author taige (Wu, Hongqiang)
 * Date: 2021-04-04
 * Time: 11:23 a.m.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface TableIdPrefix {

    String value();

}
