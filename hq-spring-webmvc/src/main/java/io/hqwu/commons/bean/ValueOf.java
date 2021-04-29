package io.hqwu.commons.bean;

/**
 * Created with IntelliJ IDEA for pp-gopay-fa
 *
 * <b>The implementation of this interface will be singletonized, to ensure thread safety when implementing.</b>
 *
 * User: taige
 * Date: 2020/5/4
 * Time: 21:01
 */
public interface ValueOf<F, T> {

    /**
     * <b>The implementation of this interface will be singletonized, to ensure thread safety when implementing.</b>
     * <br/>
     *
     * convert bean property from class F to class T
     * 
     * @param srcValue       original value
     * @param srcBean        original bean object
     * @param srcProperty    original property name
     * @param targetBean     target bean object
     * @param targetProperty target property name
     * @param params         parameters from a specific annotation call
     * @return target value with class T
     */
    T valueOf(F srcValue, Object srcBean, String srcProperty, Object targetBean, String targetProperty, String... params);
    
}
