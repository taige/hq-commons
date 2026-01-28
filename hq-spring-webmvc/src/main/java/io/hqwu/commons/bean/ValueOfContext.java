package io.hqwu.commons.bean;

/**
 * 基于上下文环境获取属性值的转换接口。
 *
 * <p>该接口扩展了 {@link ValueOf}，主要用于在 Bean 映射或属性填充过程中，
 * 根据目标对象（Target Bean）及其属性名称（Target Property）动态计算或提取值。
 *
 * <p>适用于转换逻辑依赖于目标对象状态或需要额外参数的业务场景。
 *
 * @param <T> 目标属性值的类型
 * @see ValueOf
 * @author taige (Wu, Hongqiang)
 * @since  2020/6/4
 */
public interface ValueOfContext<T> extends ValueOf<Object, T> {

    T valueOf(Object targetBean, String targetProperty, String... params);

    @Override
    default T valueOf(Object srcValue, Object srcBean, String srcProperty, Object targetBean, String targetProperty, String... params) {
        return valueOf(targetBean, targetProperty, params);
    }

}
