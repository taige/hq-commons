package io.hqwu.commons.bean;

/**
 * Bean 属性值转换器接口。
 *
 * <p>该接口用于在对象属性拷贝或转换过程中，实现从源类型 {@code F} 到目标类型 {@code T} 的自定义转换逻辑。
 * 相比于简单的类型转换，它提供了丰富的上下文信息，包括源/目标对象实例及属性名称。</p>
 *
 * <p>主要应用场景包括：
 * <ul>
 *     <li>复杂对象的深拷贝或特定字段的格式化处理。</li>
 *     <li>配合自定义注解实现声明式的属性转换。</li>
 *     <li>在不同领域模型（如 DTO 与 Entity）之间进行灵活的数据映射。</li>
 * </ul>
 * </p>
 *
 * @param <F> 源属性类型 (From)
 * @param <T> 目标属性类型 (To)
 * @author taige
 * @since  2020/5/4
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
