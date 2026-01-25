package io.hqwu.commons.utils;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link ValidList} 的单元测试类。
 * <p>
 * 测试覆盖：
 * <ul>
 *   <li>基本List操作：add, remove, get, set等基本操作</li>
 *   <li>集合操作：addAll, removeAll, retainAll等批量操作</li>
 *   <li>迭代器和流操作：iterator, listIterator, spliterator等</li>
 *   <li>Jakarta Validation校验：验证@Valid注解对List元素的级联校验</li>
 *   <li>边界条件：空列表、单元素列表等边界场景</li>
 *   <li>equals和hashCode：验证相等性和哈希码</li>
 * </ul>
 * </p>
 *
 * @author taige (Wu, Hongqiang)
 */
@DisplayName("ValidList 测试")
class ValidListTest {

    private ValidList<String> validList;
    private Validator validator;
    private ValidatorFactory validatorFactory;

    @BeforeEach
    void setUp() {
        validList = new ValidList<>();
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    // ==================== 基本操作测试 ====================

    @Test
    @DisplayName("测试添加元素")
    void testAdd() {
        assertTrue(validList.add("element1"));
        assertEquals(1, validList.size());
        assertEquals("element1", validList.get(0));

        validList.add(0, "element0");
        assertEquals(2, validList.size());
        assertEquals("element0", validList.get(0));
        assertEquals("element1", validList.get(1));
    }

    @Test
    @DisplayName("测试移除元素")
    void testRemove() {
        validList.add("element1");
        validList.add("element2");
        validList.add("element3");

        // 按对象移除
        assertTrue(validList.remove("element2"));
        assertEquals(2, validList.size());
        assertFalse(validList.contains("element2"));

        // 按索引移除
        String removed = validList.remove(0);
        assertEquals("element1", removed);
        assertEquals(1, validList.size());
    }

    @Test
    @DisplayName("测试获取和设置元素")
    void testGetAndSet() {
        validList.add("element1");
        validList.add("element2");

        assertEquals("element1", validList.get(0));
        assertEquals("element2", validList.get(1));

        String oldValue = validList.set(0, "newElement");
        assertEquals("element1", oldValue);
        assertEquals("newElement", validList.get(0));
    }

    @Test
    @DisplayName("测试列表为空判断")
    void testIsEmpty() {
        assertTrue(validList.isEmpty());
        validList.add("element");
        assertFalse(validList.isEmpty());
    }

    @Test
    @DisplayName("测试列表大小")
    void testSize() {
        assertEquals(0, validList.size());
        validList.add("element1");
        assertEquals(1, validList.size());
        validList.add("element2");
        assertEquals(2, validList.size());
        validList.remove(0);
        assertEquals(1, validList.size());
    }

    @Test
    @DisplayName("测试包含元素")
    void testContains() {
        validList.add("element1");
        validList.add("element2");

        assertTrue(validList.contains("element1"));
        assertTrue(validList.contains("element2"));
        assertFalse(validList.contains("element3"));
    }

    @Test
    @DisplayName("测试清空列表")
    void testClear() {
        validList.add("element1");
        validList.add("element2");
        assertEquals(2, validList.size());

        validList.clear();
        assertEquals(0, validList.size());
        assertTrue(validList.isEmpty());
    }

    // ==================== 批量操作测试 ====================

    @Test
    @DisplayName("测试批量添加")
    void testAddAll() {
        List<String> elements = Arrays.asList("a", "b", "c");
        assertTrue(validList.addAll(elements));
        assertEquals(3, validList.size());
        assertTrue(validList.containsAll(elements));

        // 在指定位置批量添加
        List<String> moreElements = Arrays.asList("x", "y");
        assertTrue(validList.addAll(1, moreElements));
        assertEquals(5, validList.size());
        assertEquals("a", validList.get(0));
        assertEquals("x", validList.get(1));
        assertEquals("y", validList.get(2));
        assertEquals("b", validList.get(3));
    }

    @Test
    @DisplayName("测试批量移除")
    void testRemoveAll() {
        validList.addAll(Arrays.asList("a", "b", "c", "d"));
        List<String> toRemove = Arrays.asList("b", "d");

        assertTrue(validList.removeAll(toRemove));
        assertEquals(2, validList.size());
        assertTrue(validList.contains("a"));
        assertTrue(validList.contains("c"));
        assertFalse(validList.contains("b"));
        assertFalse(validList.contains("d"));
    }

    @Test
    @DisplayName("测试保留指定元素")
    void testRetainAll() {
        validList.addAll(Arrays.asList("a", "b", "c", "d"));
        List<String> toRetain = Arrays.asList("a", "c");

        assertTrue(validList.retainAll(toRetain));
        assertEquals(2, validList.size());
        assertTrue(validList.contains("a"));
        assertTrue(validList.contains("c"));
        assertFalse(validList.contains("b"));
        assertFalse(validList.contains("d"));
    }

    @Test
    @DisplayName("测试包含所有元素")
    void testContainsAll() {
        validList.addAll(Arrays.asList("a", "b", "c"));

        assertTrue(validList.containsAll(Arrays.asList("a", "b")));
        assertTrue(validList.containsAll(Arrays.asList("a", "c")));
        assertFalse(validList.containsAll(Arrays.asList("a", "d")));
    }

    // ==================== 查找和索引测试 ====================

    @Test
    @DisplayName("测试查找元素索引")
    void testIndexOf() {
        validList.addAll(Arrays.asList("a", "b", "c", "b"));

        assertEquals(0, validList.indexOf("a"));
        assertEquals(1, validList.indexOf("b"));
        assertEquals(-1, validList.indexOf("d"));
    }

    @Test
    @DisplayName("测试查找元素最后索引")
    void testLastIndexOf() {
        validList.addAll(Arrays.asList("a", "b", "c", "b"));

        assertEquals(0, validList.lastIndexOf("a"));
        assertEquals(3, validList.lastIndexOf("b"));
        assertEquals(-1, validList.lastIndexOf("d"));
    }

    // ==================== 迭代器测试 ====================

    @Test
    @DisplayName("测试迭代器")
    void testIterator() {
        validList.addAll(Arrays.asList("a", "b", "c"));

        Iterator<String> iterator = validList.iterator();
        assertTrue(iterator.hasNext());
        assertEquals("a", iterator.next());
        assertEquals("b", iterator.next());
        assertEquals("c", iterator.next());
        assertFalse(iterator.hasNext());
    }

    @Test
    @DisplayName("测试列表迭代器")
    void testListIterator() {
        validList.addAll(Arrays.asList("a", "b", "c"));

        ListIterator<String> iterator = validList.listIterator();
        assertTrue(iterator.hasNext());
        assertEquals("a", iterator.next());

        assertTrue(iterator.hasPrevious());
        assertEquals("a", iterator.previous());

        // 从指定位置开始迭代
        ListIterator<String> iterator2 = validList.listIterator(1);
        assertEquals("b", iterator2.next());
    }

    @Test
    @DisplayName("测试子列表")
    void testSubList() {
        validList.addAll(Arrays.asList("a", "b", "c", "d", "e"));

        List<String> subList = validList.subList(1, 4);
        assertEquals(3, subList.size());
        assertEquals("b", subList.get(0));
        assertEquals("c", subList.get(1));
        assertEquals("d", subList.get(2));
    }

    @Test
    @DisplayName("测试替换所有元素")
    void testReplaceAll() {
        validList.addAll(Arrays.asList("a", "b", "c"));

        validList.replaceAll(String::toUpperCase);

        assertEquals("A", validList.get(0));
        assertEquals("B", validList.get(1));
        assertEquals("C", validList.get(2));
    }

    @Test
    @DisplayName("测试排序")
    void testSort() {
        validList.addAll(Arrays.asList("c", "a", "b"));

        validList.sort(Comparator.naturalOrder());

        assertEquals("a", validList.get(0));
        assertEquals("b", validList.get(1));
        assertEquals("c", validList.get(2));
    }

    @Test
    @DisplayName("测试Spliterator")
    void testSpliterator() {
        validList.addAll(Arrays.asList("a", "b", "c"));

        Spliterator<String> spliterator = validList.spliterator();
        assertNotNull(spliterator);
        assertEquals(3, spliterator.estimateSize());
    }

    // ==================== 数组转换测试 ====================

    @Test
    @DisplayName("测试转换为数组")
    void testToArray() {
        validList.addAll(Arrays.asList("a", "b", "c"));

        Object[] array = validList.toArray();
        assertEquals(3, array.length);
        assertEquals("a", array[0]);
        assertEquals("b", array[1]);
        assertEquals("c", array[2]);
    }

    @Test
    @DisplayName("测试转换为指定类型数组")
    void testToArrayWithType() {
        validList.addAll(Arrays.asList("a", "b", "c"));

        String[] array = validList.toArray(new String[0]);
        assertEquals(3, array.length);
        assertEquals("a", array[0]);
        assertEquals("b", array[1]);
        assertEquals("c", array[2]);
    }

    // ==================== equals 和 hashCode 测试 ====================

    @Test
    @DisplayName("测试equals方法")
    void testEquals() {
        validList.addAll(Arrays.asList("a", "b", "c"));

        List<String> otherList = new ArrayList<>(Arrays.asList("a", "b", "c"));
        assertEquals(validList, otherList);

        ValidList<String> anotherValidList = new ValidList<>();
        anotherValidList.addAll(Arrays.asList("a", "b", "c"));
        assertEquals(validList, anotherValidList);

        otherList.add("d");
        assertNotEquals(validList, otherList);
    }

    @Test
    @DisplayName("测试hashCode方法")
    void testHashCode() {
        validList.addAll(Arrays.asList("a", "b", "c"));

        List<String> otherList = new ArrayList<>(Arrays.asList("a", "b", "c"));
        assertEquals(validList.hashCode(), otherList.hashCode());
    }

    // ==================== Getter/Setter 测试 ====================

    @Test
    @DisplayName("测试getList和setList方法")
    void testGetAndSetList() {
        List<String> newList = new ArrayList<>(Arrays.asList("x", "y", "z"));
        validList.setList(newList);

        assertEquals(3, validList.size());
        assertEquals(newList, validList.getList());
        assertEquals("x", validList.get(0));
    }

    // ==================== Jakarta Validation 测试 ====================

    /**
     * 测试用的包装类，用于验证ValidList的级联校验功能
     */
    static class TestContainer {
        @jakarta.validation.Valid
        private ValidList<TestItem> items;

        public ValidList<TestItem> getItems() {
            return items;
        }

        public void setItems(ValidList<TestItem> items) {
            this.items = items;
        }
    }

    /**
     * 测试用的元素类，包含验证约束
     */
    static class TestItem {
        @NotBlank(message = "名称不能为空")
        @Size(min = 2, max = 10, message = "名称长度必须在2-10之间")
        private String name;

        @NotNull(message = "值不能为null")
        private Integer value;

        public TestItem() {
        }

        public TestItem(String name, Integer value) {
            this.name = name;
            this.value = value;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Integer getValue() {
            return value;
        }

        public void setValue(Integer value) {
            this.value = value;
        }
    }

    @Test
    @DisplayName("测试ValidList的级联校验 - 校验通过")
    void testValidationSuccess() {
        TestContainer container = new TestContainer();
        ValidList<TestItem> items = new ValidList<>();
        items.add(new TestItem("Item1", 100));
        items.add(new TestItem("Item2", 200));
        container.setItems(items);

        Set<ConstraintViolation<TestContainer>> violations = validator.validate(container);
        assertTrue(violations.isEmpty(), "校验应该通过");
    }

    @Test
    @DisplayName("测试ValidList的级联校验 - 校验失败（名称为空）")
    void testValidationFailureBlankName() {
        TestContainer container = new TestContainer();
        ValidList<TestItem> items = new ValidList<>();
        items.add(new TestItem("", 100)); // 名称为空，违反@NotBlank约束
        items.add(new TestItem("Item2", 200));
        container.setItems(items);

        Set<ConstraintViolation<TestContainer>> violations = validator.validate(container);
        assertFalse(violations.isEmpty(), "校验应该失败");
        assertTrue(violations.stream()
                .anyMatch(v -> v.getMessage().contains("名称不能为空")));
    }

    @Test
    @DisplayName("测试ValidList的级联校验 - 校验失败（值为null）")
    void testValidationFailureNullValue() {
        TestContainer container = new TestContainer();
        ValidList<TestItem> items = new ValidList<>();
        items.add(new TestItem("Item1", null)); // 值为null，违反@NotNull约束
        container.setItems(items);

        Set<ConstraintViolation<TestContainer>> violations = validator.validate(container);
        assertFalse(violations.isEmpty(), "校验应该失败");
        assertTrue(violations.stream()
                .anyMatch(v -> v.getMessage().contains("值不能为null")));
    }

    @Test
    @DisplayName("测试ValidList的级联校验 - 校验失败（名称长度不符）")
    void testValidationFailureNameSize() {
        TestContainer container = new TestContainer();
        ValidList<TestItem> items = new ValidList<>();
        items.add(new TestItem("A", 100)); // 名称长度小于2
        items.add(new TestItem("VeryLongName", 200)); // 名称长度大于10
        container.setItems(items);

        Set<ConstraintViolation<TestContainer>> violations = validator.validate(container);
        assertFalse(violations.isEmpty(), "校验应该失败");
        // 每个item都有一个@Size违规，所以至少有2个错误
        assertTrue(violations.size() >= 2, "至少应该有2个校验错误");
        assertTrue(violations.stream()
                .anyMatch(v -> v.getMessage().contains("名称长度必须在2-10之间")));
    }

    @Test
    @DisplayName("测试ValidList的级联校验 - 多个元素多个错误")
    void testValidationMultipleItemsWithErrors() {
        TestContainer container = new TestContainer();
        ValidList<TestItem> items = new ValidList<>();
        items.add(new TestItem("", null)); // 两个错误
        items.add(new TestItem("Ok", 100)); // 正确
        items.add(new TestItem("X", null)); // 两个错误
        container.setItems(items);

        Set<ConstraintViolation<TestContainer>> violations = validator.validate(container);
        assertFalse(violations.isEmpty(), "校验应该失败");
        assertTrue(violations.size() >= 4, "至少应该有4个校验错误");
    }

    @Test
    @DisplayName("测试ValidList的级联校验 - 空列表")
    void testValidationEmptyList() {
        TestContainer container = new TestContainer();
        ValidList<TestItem> items = new ValidList<>();
        container.setItems(items);

        Set<ConstraintViolation<TestContainer>> violations = validator.validate(container);
        assertTrue(violations.isEmpty(), "空列表校验应该通过");
    }

    // ==================== 边界条件测试 ====================

    @Test
    @DisplayName("测试空列表操作")
    void testEmptyListOperations() {
        assertTrue(validList.isEmpty());
        assertEquals(0, validList.size());
        assertFalse(validList.contains("anything"));
        assertFalse(validList.iterator().hasNext());
        assertEquals(0, validList.toArray().length);
    }

    @Test
    @DisplayName("测试单元素列表操作")
    void testSingleElementList() {
        validList.add("single");

        assertEquals(1, validList.size());
        assertTrue(validList.contains("single"));
        assertEquals("single", validList.get(0));
        assertEquals(0, validList.indexOf("single"));
        assertEquals(0, validList.lastIndexOf("single"));
    }

    @Test
    @DisplayName("测试null元素处理")
    void testNullElements() {
        validList.add(null);
        validList.add("notNull");
        validList.add(null);

        assertEquals(3, validList.size());
        assertTrue(validList.contains(null));
        assertNull(validList.get(0));
        assertEquals("notNull", validList.get(1));
        assertNull(validList.get(2));
    }
}
