package io.hqwu.commons.annotation;

import io.hqwu.commons.bean.ValueOf;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.*;

/**
 * Created with IntelliJ IDEA for pp-gopay-fa
 * User: taige
 * Date: 2020/5/4
 * Time: 20:59
 */
@Target({ElementType.FIELD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface SourceProperty {

    /**
     * 来源类的属性名(缺省使用同名属性)
     * @return 拷贝/转换来源类的属性名
     */
    @AliasFor("name")
    String value() default "";

    /**
     * 来源类的属性名(缺省使用同名属性)
     * @return 拷贝/转换来源类的属性名
     */
    @AliasFor("value")
    String name() default "";

    /**
     * 转换器，缺省从ApplicationContext中查找该类的Bean
     * @return 转换器
     */
    Class<? extends ValueOf>[] valueOf() default {};

    /**
     * 限定来源属性的所属类(适用于拷贝源来自多个类的情况，在class上使用表示属性的缺省来源类)
     * @return
     */
    Class<?>[] sourceClasses() default {};

    /**
     *  a qualifier for candidate bean for {@link #valueOf}
     * @return bean name
     */
    String qualifier() default "";

    /**
     * parameters when invoke valueOf for customize converter
     * @return parameters
     */
    String[] params() default {};

    /**
     * don't do copy if true
     * @return whether do copy
     */
    boolean ignore() default false;

    /**
     * String 类型长度限制，默认不限制；
     * 最小值：4，小于4时不缩略
     * 超过长度时，调用 {@link org.apache.commons.lang3.StringUtils#abbreviate(String, int)}
     * @return
     */
    int abbreviate() default 0;

}
