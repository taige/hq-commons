package io.hqwu.commons.mybatisplus.annotation;

import java.lang.annotation.*;

/**
 * Created with IntelliJ IDEA for pp-gopay-fa
 *
 * @author taige (Wu, Hongqiang)
 * Date: 2020/5/29
 * Time: 11:23
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface JoinColumn {

    /**
     * the column name of right table when join query
     * @return column name
     */
    String value();
    
}
