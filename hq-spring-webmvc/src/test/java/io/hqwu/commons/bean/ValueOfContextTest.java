package io.hqwu.commons.bean;

import io.hqwu.commons.annotation.SourceProperty;
import lombok.Data;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ValueOfContext 接口功能测试
 *
 * ValueOfContext 是一个特殊的转换器接口：
 * - 它继承自 ValueOf<Object, T>
 * - 提供了一个简化的 valueOf(targetBean, targetProperty, params) 方法
 * - default 实现会忽略源值和源属性，只使用目标对象的上下文
 *
 * 应用场景：
 * - 生成默认值
 * - 基于目标对象状态生成值
 * - 不依赖源属性的值计算
 *
 * @author taige (Wu, Hongqiang)
 * Date: 2026/1/26
 */
class ValueOfContextTest {

    /**
     * 测试 ValueOfContext 接口的 default 方法
     *
     * 验证 ValueOfContext 忽略源值，使用目标对象上下文生成值
     */
    @Test
    void test_default_method_ignores_source_value() {
        ValueOfContextSrc src = new ValueOfContextSrc();
        src.setGeneratedField("will be ignored");  // ValueOfContext 会忽略这个值
        src.setConstantField("will be ignored");   // ValueOfContext 会忽略这个值

        ValueOfContextTarget target = BeanConverter.convert(src, ValueOfContextTarget.class);

        assertNotNull(target);
        // ValueOfContext 忽略源值，返回默认值
        assertEquals("DEFAULT_VALUE", target.getGeneratedField());
        assertEquals("CONSTANT", target.getConstantField());
    }

    @Data
    static class ValueOfContextSrc {
        private String generatedField;  // 字段名匹配
        private String constantField;   // 字段名匹配
    }

    @Data
    static class ValueOfContextTarget {
        @SourceProperty(valueOf = DefaultValueContext.class)
        private String generatedField;

        @SourceProperty(valueOf = ConstantValueContext.class)
        private String constantField;
    }

    static class DefaultValueContext implements ValueOfContext<String> {
        @Override
        public String valueOf(Object targetBean, String targetProperty, String... params) {
            // 只使用目标对象上下文，不依赖源值
            return "DEFAULT_VALUE";
        }
    }

    static class ConstantValueContext implements ValueOfContext<String> {
        @Override
        public String valueOf(Object targetBean, String targetProperty, String... params) {
            return "CONSTANT";
        }
    }

    /**
     * 测试 ValueOfContext 不需要源属性值的特性
     *
     * 当使用 ValueOfContext 时，源值会被忽略
     */
    @Test
    void test_source_value_is_ignored() {
        ValueOfContextNoSrcSrc src = new ValueOfContextNoSrcSrc();
        src.setTargetField("this will be ignored");  // ValueOfContext 会忽略源值

        ValueOfContextNoSrcTarget target = BeanConverter.convert(src, ValueOfContextNoSrcTarget.class);

        assertNotNull(target);
        // ValueOfContext 忽略源值，生成自己的值
        assertEquals("GENERATED", target.getTargetField());
    }

    @Data
    static class ValueOfContextNoSrcSrc {
        private String targetField;  // 字段名匹配
    }

    @Data
    static class ValueOfContextNoSrcTarget {
        // 使用 ValueOfContext，源值会被忽略
        @SourceProperty(valueOf = GeneratedValueContext.class)
        private String targetField;
    }

    static class GeneratedValueContext implements ValueOfContext<String> {
        @Override
        public String valueOf(Object targetBean, String targetProperty, String... params) {
            return "GENERATED";
        }
    }

    /**
     * 测试 ValueOfContext 使用 params 参数
     */
    @Test
    void test_params_are_passed_correctly() {
        ValueOfContextParamsSrc src = new ValueOfContextParamsSrc();
        src.setPrefixedValue("ignored");

        ValueOfContextParamsTarget target = BeanConverter.convert(src, ValueOfContextParamsTarget.class);

        assertNotNull(target);
        // ValueOfContext 可以使用 params 参数
        assertEquals("PREFIX_PARAM1_PARAM2", target.getPrefixedValue());
    }

    @Data
    static class ValueOfContextParamsSrc {
        private String prefixedValue;  // 字段名匹配
    }

    @Data
    static class ValueOfContextParamsTarget {
        @SourceProperty(valueOf = ParamsValueContext.class, params = {"PARAM1", "PARAM2"})
        private String prefixedValue;
    }

    static class ParamsValueContext implements ValueOfContext<String> {
        @Override
        public String valueOf(Object targetBean, String targetProperty, String... params) {
            StringBuilder sb = new StringBuilder("PREFIX");
            for (String param : params) {
                sb.append("_").append(param);
            }
            return sb.toString();
        }
    }

    /**
     * 测试 ValueOfContext 基于目标对象状态生成值
     */
    @Test
    void test_can_access_target_bean_state() {
        ValueOfContextStateSrc src = new ValueOfContextStateSrc();
        src.setId(123);
        src.setComputedField("ignored");  // 会被 ValueOfContext 忽略

        ValueOfContextStateTarget target = BeanConverter.convert(src, ValueOfContextStateTarget.class);

        assertNotNull(target);
        assertEquals(123, target.getId());
        // computedField 基于目标对象的 id 字段计算
        assertEquals("ID_123", target.getComputedField());
    }

    @Data
    static class ValueOfContextStateSrc {
        private Integer id;
        private String computedField;  // 字段名匹配
    }

    @Data
    static class ValueOfContextStateTarget {
        private Integer id;  // 先被设置

        @SourceProperty(valueOf = StateBasedValueContext.class)
        private String computedField;  // 然后基于 id 生成值
    }

    static class StateBasedValueContext implements ValueOfContext<String> {
        @Override
        public String valueOf(Object targetBean, String targetProperty, String... params) {
            if (targetBean instanceof ValueOfContextStateTarget) {
                ValueOfContextStateTarget target = (ValueOfContextStateTarget) targetBean;
                Integer id = target.getId();
                return id != null ? "ID_" + id : "NO_ID";
            }
            return "UNKNOWN";
        }
    }

    /**
     * 测试 ValueOfContext 的 default 方法实现
     * 验证它正确地委托到简化的 valueOf 方法
     */
    @Test
    void test_default_method_delegates_correctly() {
        // 直接测试 ValueOfContext 的 default 方法
        TestValueOfContext context = new TestValueOfContext();

        // 调用 default 方法（6个参数的版本）
        String result = context.valueOf(
            "ignored_src_value",      // 应该被忽略
            new Object(),              // srcBean - 应该被忽略
            "ignored_src_property",   // 应该被忽略
            new Object(),              // targetBean
            "targetProperty",
            "param1", "param2"
        );

        // 验证 default 方法正确委托到简化版本
        assertEquals("targetProperty:param1,param2", result);
    }

    static class TestValueOfContext implements ValueOfContext<String> {
        @Override
        public String valueOf(Object targetBean, String targetProperty, String... params) {
            // 验证只接收到 targetBean, targetProperty, params
            // srcValue, srcBean, srcProperty 应该被 default 方法忽略
            StringBuilder sb = new StringBuilder(targetProperty);
            if (params.length > 0) {
                sb.append(":");
                sb.append(String.join(",", params));
            }
            return sb.toString();
        }
    }

    /**
     * 测试 ValueOfContext 返回 null 的情况
     */
    @Test
    void test_can_return_null() {
        ValueOfContextNullSrc src = new ValueOfContextNullSrc();
        src.setNullableField("will be ignored");

        ValueOfContextNullTarget target = BeanConverter.convert(src, ValueOfContextNullTarget.class);

        assertNotNull(target);
        assertNull(target.getNullableField());
    }

    @Data
    static class ValueOfContextNullSrc {
        private String nullableField;  // 字段名匹配
    }

    @Data
    static class ValueOfContextNullTarget {
        @SourceProperty(valueOf = NullValueContext.class)
        private String nullableField;
    }

    static class NullValueContext implements ValueOfContext<String> {
        @Override
        public String valueOf(Object targetBean, String targetProperty, String... params) {
            return null;  // 返回 null
        }
    }

    /**
     * 测试 ValueOfContext 返回复杂对象
     */
    @Test
    void test_can_return_complex_objects() {
        ValueOfContextComplexSrc src = new ValueOfContextComplexSrc();
        src.setMetadata(new HashMap<>());  // 会被忽略

        ValueOfContextComplexTarget target = BeanConverter.convert(src, ValueOfContextComplexTarget.class);

        assertNotNull(target);
        assertNotNull(target.getMetadata());
        assertTrue(target.getMetadata().containsKey("generated"));
        assertEquals("true", target.getMetadata().get("generated"));
        assertTrue(target.getMetadata().containsKey("timestamp"));
        assertTrue(target.getMetadata().containsKey("property"));
        assertEquals("metadata", target.getMetadata().get("property"));
    }

    @Data
    static class ValueOfContextComplexSrc {
        private Map<String, String> metadata;  // 字段名匹配
    }

    @Data
    static class ValueOfContextComplexTarget {
        @SourceProperty(valueOf = MetadataValueContext.class)
        private Map<String, String> metadata;
    }

    static class MetadataValueContext implements ValueOfContext<Map<String, String>> {
        @Override
        public Map<String, String> valueOf(Object targetBean, String targetProperty, String... params) {
            Map<String, String> metadata = new HashMap<>();
            metadata.put("generated", "true");
            metadata.put("timestamp", String.valueOf(System.currentTimeMillis()));
            metadata.put("property", targetProperty);
            return metadata;
        }
    }

}
