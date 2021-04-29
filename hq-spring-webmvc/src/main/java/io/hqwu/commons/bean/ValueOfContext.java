package io.hqwu.commons.bean;

/**
 * Created with IntelliJ IDEA for pp-gopay-fa
 *
 * @author taige (Wu, Hongqiang)
 * Date: 2020/6/4
 * Time: 18:49
 */
public interface ValueOfContext<T> extends ValueOf<Object, T> {

    T valueOf(Object targetBean, String targetProperty, String... params);

    @Override
    default T valueOf(Object srcValue, Object srcBean, String srcProperty, Object targetBean, String targetProperty, String... params) {
        return valueOf(targetBean, targetProperty, params);
    }

}
