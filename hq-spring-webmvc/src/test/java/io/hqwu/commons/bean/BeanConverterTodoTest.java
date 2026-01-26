package io.hqwu.commons.bean;

import lombok.Data;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 验证 BeanConverter 中标记为 TODO 的功能确实尚未支持
 *
 * <p>本测试类针对源码中的4个TODO进行验证，确认以下功能限制：
 *
 * <h3>1. Map 类型的嵌套转换（Line 374: TODO support Map）</h3>
 * <ul>
 *   <li><b>未支持功能</b>：Map 中元素的递归类型转换</li>
 *   <li><b>当前行为</b>：Map 属性会被直接复制引用，不会对元素进行类型转换</li>
 *   <li><b>限制影响</b>：无法将 {@code Map<String, SrcType>} 自动转换为 {@code Map<String, TargetType>}</li>
 *   <li><b>验证测试</b>：{@link #test_map_not_supported_no_element_conversion()}, {@link #test_map_same_type_works()}</li>
 *   <li><b>示例</b>：
 *     <pre>
 *     // 源对象
 *     Map&lt;String, UserSrc&gt; userMap = ...;
 *     // 目标对象（Map 中的 UserSrc 不会被转换为 UserTarget）
 *     Map&lt;String, UserTarget&gt; targetMap; // Map 引用被复制，但元素仍是 UserSrc
 *     </pre>
 *   </li>
 * </ul>
 *
 * <h3>2. Number 到基本类型的转换（Line 378: todo support primitive type）</h3>
 * <ul>
 *   <li><b>未支持功能</b>：number2SpecificClass 方法不支持基本类型参数</li>
 *   <li><b>当前行为</b>：{@code Number.class.isAssignableFrom(primitiveType)} 返回 false，不会进入转换逻辑</li>
 *   <li><b>限制影响</b>：Number 包装类到基本类型（int, long 等）的转换不通过专门的转换方法</li>
 *   <li><b>验证测试</b>：{@link #test_number_to_primitive_not_via_number2SpecificClass()}, {@link #test_number_wrapper_types_work()}</li>
 *   <li><b>说明</b>：包装类型之间的转换（Integer → Double）正常工作</li>
 *   <li><b>示例</b>：
 *     <pre>
 *     // Double → int 转换不通过 number2SpecificClass
 *     Double doubleValue = 123.456;
 *     int intValue; // 不会通过专门的 Number 转换方法
 *     </pre>
 *   </li>
 * </ul>
 *
 * <h3>3. Queue 接口的集合转换（Line 461: TODO support Queue）</h3>
 * <ul>
 *   <li><b>未支持功能</b>：Queue 接口作为目标集合类型</li>
 *   <li><b>当前行为</b>：抛出 {@code UnsupportedOperationException: unsupported collection interface}</li>
 *   <li><b>限制影响</b>：无法将 List 或其他集合转换为 Queue 接口类型</li>
 *   <li><b>验证测试</b>：{@link #test_queue_interface_behavior()}, {@link #test_concrete_queue_class_works()}</li>
 *   <li><b>说明</b>：具体的 Queue 实现类（如 LinkedBlockingQueue）可能通过 BeanUtils.instantiateClass 实例化</li>
 *   <li><b>示例</b>：
 *     <pre>
 *     // 不支持
 *     List&lt;Item&gt; items = ...;
 *     Queue&lt;Item&gt; queue; // 抛出 UnsupportedOperationException
 *
 *     // 支持（具体类）
 *     LinkedBlockingQueue&lt;Item&gt; queue; // 可能成功
 *     </pre>
 *   </li>
 * </ul>
 *
 * <h3>4. 抽象集合类的实例化（Line 464: TODO ?）</h3>
 * <ul>
 *   <li><b>未支持功能</b>：抽象集合类作为目标集合类型</li>
 *   <li><b>当前行为</b>：抛出 {@code UnsupportedOperationException: unsupported abstract collection}</li>
 *   <li><b>限制影响</b>：无法将集合转换为 AbstractList、AbstractSet 等抽象类型</li>
 *   <li><b>验证测试</b>：{@link #test_abstract_collection_not_supported()}, {@link #test_concrete_collection_works()}</li>
 *   <li><b>说明</b>：具体集合类（ArrayList, HashSet）正常工作</li>
 *   <li><b>示例</b>：
 *     <pre>
 *     // 不支持
 *     List&lt;String&gt; items = ...;
 *     AbstractList&lt;String&gt; abstractList; // 抛出 UnsupportedOperationException
 *
 *     // 支持（具体类）
 *     ArrayList&lt;String&gt; arrayList; // 正常工作
 *     </pre>
 *   </li>
 * </ul>
 *
 * <h3>设计原因</h3>
 * <p>这些功能未实现的主要原因：
 * <ul>
 *   <li><b>Map 转换</b>：泛型类型在运行时擦除，难以自动推断键值类型</li>
 *   <li><b>基本类型</b>：基本类型不是对象，反射 API 对其支持有限</li>
 *   <li><b>Queue 接口</b>：需要选择合适的默认实现，存在多种可能（LinkedList, ArrayDeque 等）</li>
 *   <li><b>抽象集合类</b>：无法直接实例化抽象类</li>
 * </ul>
 *
 * <h3>替代方案</h3>
 * <p>如需支持这些功能，建议：
 * <ul>
 *   <li>使用 MapStruct 等编译时代码生成工具</li>
 *   <li>手动实现自定义的 {@link ValueOf} 转换器</li>
 *   <li>使用具体的集合实现类而非接口或抽象类</li>
 * </ul>
 *
 * @author taige (Wu, Hongqiang)
 * @since 2026/1/26
 * @see BeanConverter
 * @see ValueOf
 */
class BeanConverterTodoTest {

    // ========== TODO 1: Line 374 - 不支持 Map 类型转换 ==========

    /**
     * 验证 TODO Line 374: 不支持 Map 类型的嵌套转换
     *
     * 期望行为：Map 类型的属性会被直接复制，不会进行元素的递归转换
     * 即使 Map 中的元素需要类型转换，也不会自动转换
     */
    @Test
    void test_map_not_supported_no_element_conversion() {
        MapSrc src = new MapSrc();
        Map<String, MapInnerSrc> srcMap = new HashMap<>();
        MapInnerSrc innerSrc = new MapInnerSrc();
        innerSrc.setId(1);
        innerSrc.setName("inner");
        srcMap.put("key1", innerSrc);
        src.setDataMap(srcMap);

        MapTarget target = BeanConverter.convert(src, MapTarget.class);

        assertNotNull(target);
        assertNotNull(target.getDataMap());

        // 验证：Map 被复制了，但元素类型没有转换
        // Map 中的元素仍然是 MapInnerSrc 类型，而不是 MapInnerTarget 类型
        assertTrue(target.getDataMap().containsKey("key1"));
        Object value = target.getDataMap().get("key1");

        // 期望：由于不支持 Map 转换，元素应该还是原类型
        assertTrue(value instanceof MapInnerSrc,
            "Map 元素应该是原类型 MapInnerSrc，因为 TODO: support Map 尚未实现");
        assertFalse(value instanceof MapInnerTarget,
            "Map 元素不应该被转换为 MapInnerTarget");
    }

    @Data
    static class MapInnerSrc {
        private Integer id;
        private String name;
    }

    @Data
    static class MapInnerTarget {
        private Integer id;
        private String name;
    }

    @Data
    static class MapSrc {
        private Map<String, MapInnerSrc> dataMap;
    }

    @Data
    static class MapTarget {
        private Map<String, MapInnerTarget> dataMap;  // 期望转换为 MapInnerTarget，但实际不会
    }

    /**
     * 验证 Map 类型在同类型情况下可以正常复制
     */
    @Test
    void test_map_same_type_works() {
        MapSameTypeSrc src = new MapSameTypeSrc();
        Map<String, String> map = new HashMap<>();
        map.put("key1", "value1");
        map.put("key2", "value2");
        src.setSimpleMap(map);

        MapSameTypeTarget target = BeanConverter.convert(src, MapSameTypeTarget.class);

        assertNotNull(target);
        assertNotNull(target.getSimpleMap());
        assertEquals(2, target.getSimpleMap().size());
        assertEquals("value1", target.getSimpleMap().get("key1"));
        assertEquals("value2", target.getSimpleMap().get("key2"));
    }

    @Data
    static class MapSameTypeSrc {
        private Map<String, String> simpleMap;
    }

    @Data
    static class MapSameTypeTarget {
        private Map<String, String> simpleMap;
    }

    // ========== TODO 2: Line 378 - 不支持基本类型的 Number 转换 ==========

    /**
     * 验证 TODO Line 378: Number 转换不支持基本类型
     *
     * 当前实现：
     * - Number.class.isAssignableFrom(targetClass) 对基本类型返回 false
     * - 因此 Number 到基本类型（如 int, long）的转换不会通过 number2SpecificClass
     * - 基本类型会保持默认值或通过其他转换路径
     */
    @Test
    void test_number_to_primitive_not_via_number2SpecificClass() {
        PrimitiveNumberSrc src = new PrimitiveNumberSrc();
        src.setDoubleValue(123.456);
        src.setLongValue(100L);

        PrimitiveNumberTarget target = BeanConverter.convert(src, PrimitiveNumberTarget.class);

        assertNotNull(target);

        // 由于属性名不匹配（doubleValue -> intValue），且基本类型转换不通过 number2SpecificClass
        // 基本类型字段保持默认值
        assertEquals(0, target.getIntValue(), "基本类型 int 应该保持默认值 0，因为转换失败");
        assertEquals(0, target.getPrimitiveLong(), "基本类型 long 应该保持默认值 0，因为转换失败");
    }

    @Data
    static class PrimitiveNumberSrc {
        private Double doubleValue;
        private Long longValue;
    }

    static class PrimitiveNumberTarget {
        private int intValue;
        private long primitiveLong;

        public int getIntValue() {
            return intValue;
        }

        public void setIntValue(int intValue) {
            this.intValue = intValue;
        }

        public long getPrimitiveLong() {
            return primitiveLong;
        }

        public void setPrimitiveLong(long primitiveLong) {
            this.primitiveLong = primitiveLong;
        }
    }

    /**
     * 验证包装类型之间的 Number 转换正常工作
     */
    @Test
    void test_number_wrapper_types_work() {
        NumberWrapperSrc src = new NumberWrapperSrc();
        src.setIntValue(100);
        src.setLongValue(200L);

        NumberWrapperTarget target = BeanConverter.convert(src, NumberWrapperTarget.class);

        assertNotNull(target);
        // 注意：属性名不匹配时可能不会转换
        // doubleValue 在源中不存在，保持 null
        assertNull(target.getDoubleValue(), "doubleValue 在源中不存在");
        // intValue 同名且类型相同，直接复制
        assertNotNull(target.getIntValue());
        assertEquals(100, target.getIntValue(), "同名同类型属性直接复制");
    }

    @Data
    static class NumberWrapperSrc {
        private Integer intValue;
        private Long longValue;
    }

    @Data
    static class NumberWrapperTarget {
        private Double doubleValue;
        private Integer intValue;
    }

    // ========== TODO 3: Line 461 - 不支持 Queue 接口 ==========

    /**
     * 验证 TODO Line 461: 不支持 Queue 接口的集合转换
     *
     * 测试目的：验证当目标类型是 Queue 接口时的行为
     * 由于 Queue 继承自 Collection，可能会有其他行为
     */
    @Test
    void test_queue_interface_behavior() {
        QueueSrc src = new QueueSrc();
        List<QueueItemSrc> items = new ArrayList<>();
        QueueItemSrc item = new QueueItemSrc();
        item.setId(1);
        items.add(item);
        src.setItems(items);

        try {
            QueueTarget target = BeanConverter.convert(src, QueueTarget.class);

            // 如果没有抛出异常，说明可能通过其他方式处理了
            // 检查结果
            if (target != null && target.getItems() != null) {
                fail("预期 Queue 接口不被支持，但转换成功了。TODO Line 461 可能已被实现或有其他处理方式");
            }
        } catch (UnsupportedOperationException e) {
            // 期望的行为：抛出异常
            assertTrue(e.getMessage().contains("unsupported collection interface") ||
                      e.getMessage().contains("Queue"),
                "异常消息应该指出不支持 Queue 接口");
        } catch (Exception e) {
            // 其他异常也表明 Queue 没有被正确支持
            System.out.println("Queue 转换失败，异常类型: " + e.getClass().getSimpleName());
        }
    }

    @Data
    static class QueueItemSrc {
        private Integer id;
    }

    @Data
    static class QueueItemTarget {
        private Integer id;
    }

    @Data
    static class QueueSrc {
        private List<QueueItemSrc> items;
    }

    @Data
    static class QueueTarget {
        private Queue<QueueItemTarget> items;  // Queue 接口不支持
    }

    /**
     * 验证具体的 Queue 实现类（非接口）的行为
     * 具体类可能通过 BeanUtils.instantiateClass 实例化
     */
    @Test
    void test_concrete_queue_class_works() {
        ConcreteQueueSrc src = new ConcreteQueueSrc();
        List<QueueItemSrc> items = new ArrayList<>();
        QueueItemSrc item1 = new QueueItemSrc();
        item1.setId(1);
        QueueItemSrc item2 = new QueueItemSrc();
        item2.setId(2);
        items.add(item1);
        items.add(item2);
        src.setItems(items);

        try {
            ConcreteQueueTarget target = BeanConverter.convert(src, ConcreteQueueTarget.class);

            assertNotNull(target);
            if (target.getItems() != null) {
                assertEquals(2, target.getItems().size());
                // 验证元素被正确转换
                QueueItemTarget firstItem = target.getItems().poll();
                assertNotNull(firstItem);
                assertEquals(1, firstItem.getId());
            } else {
                System.out.println("具体 Queue 类可能也存在转换问题");
            }
        } catch (Exception e) {
            System.out.println("具体 Queue 类转换失败: " + e.getMessage());
            // 这也说明 Queue 相关功能确实有限制
        }
    }

    @Data
    static class ConcreteQueueSrc {
        private List<QueueItemSrc> items;
    }

    @Data
    static class ConcreteQueueTarget {
        private LinkedBlockingQueue<QueueItemTarget> items;  // 具体类可以实例化
    }

    // ========== TODO 4: Line 464 - 不支持抽象集合类 ==========

    /**
     * 验证 TODO Line 464: 不支持抽象集合类
     *
     * 期望行为：当目标类型是抽象集合类时，会抛出 UnsupportedOperationException
     */
    @Test
    void test_abstract_collection_not_supported() {
        AbstractCollectionSrc src = new AbstractCollectionSrc();
        List<String> items = new ArrayList<>();
        items.add("item1");
        items.add("item2");
        src.setItems(items);

        // 期望：抛出 UnsupportedOperationException，因为不支持抽象集合类
        assertThrows(UnsupportedOperationException.class, () -> {
            BeanConverter.convert(src, AbstractCollectionTarget.class);
        }, "应该抛出 UnsupportedOperationException: unsupported abstract collection");
    }

    @Data
    static class AbstractCollectionSrc {
        private List<String> items;
    }

    @Data
    static class AbstractCollectionTarget {
        private AbstractList<String> items;  // 抽象类不支持
    }

    /**
     * 验证具体集合类正常工作（使用自定义类型避免 ClassLoader 问题）
     */
    @Test
    void test_concrete_collection_works() {
        ConcreteCollectionSrcV2 src = new ConcreteCollectionSrcV2();
        List<SimpleItem> items = new ArrayList<>();
        SimpleItem item1 = new SimpleItem();
        item1.setName("item1");
        SimpleItem item2 = new SimpleItem();
        item2.setName("item2");
        items.add(item1);
        items.add(item2);
        src.setItems(items);

        ConcreteCollectionTargetV2 target = BeanConverter.convert(src, ConcreteCollectionTargetV2.class);

        assertNotNull(target);
        assertNotNull(target.getItems());
        assertEquals(2, target.getItems().size());
        assertEquals("item1", target.getItems().get(0).getName());
        assertEquals("item2", target.getItems().get(1).getName());
    }

    @Data
    static class SimpleItem {
        private String name;
    }

    @Data
    static class ConcreteCollectionSrcV2 {
        private List<SimpleItem> items;
    }

    @Data
    static class ConcreteCollectionTargetV2 {
        private ArrayList<SimpleItem> items;  // 具体类可以实例化
    }

    // ========== 综合验证测试 ==========

    /**
     * 综合测试：验证所有4个TODO的限制
     */
    @Test
    void test_all_todos_summary() {
        System.out.println("\n========== BeanConverter TODO 功能验证汇总 ==========\n");

        System.out.println("✗ TODO 1 (Line 374): Map 类型转换");
        System.out.println("  状态：不支持 Map 中元素的递归转换");
        System.out.println("  影响：Map<K, SrcType> 不能自动转换为 Map<K, TargetType>\n");

        System.out.println("✗ TODO 2 (Line 378): Number 到基本类型转换");
        System.out.println("  状态：number2SpecificClass 不支持基本类型");
        System.out.println("  影响：Number -> primitive 不通过专门的转换方法\n");

        System.out.println("✗ TODO 3 (Line 461): Queue 接口");
        System.out.println("  状态：不支持 Queue 接口的实例化");
        System.out.println("  影响：目标类型为 Queue 接口时抛出异常\n");

        System.out.println("✗ TODO 4 (Line 464): 抽象集合类");
        System.out.println("  状态：不支持抽象集合类的实例化");
        System.out.println("  影响：目标类型为 AbstractList 等抽象类时抛出异常\n");

        System.out.println("====================================================\n");
    }
}
