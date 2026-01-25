package io.hqwu.commons.utils;

import jakarta.validation.Valid;

import java.util.*;
import java.util.function.UnaryOperator;

/**
 * 支持Jakarta Validation的List包装类。
 * <p>
 * 该类实现了{@link List}接口,并在内部持有一个{@link ArrayList}实例。
 * 通过在内部list字段上添加{@link jakarta.validation.Valid}注解,
 * 使得当此类作为请求参数或Bean的属性时,能够触发对List中每个元素的级联校验。
 * </p>
 *
 * <p>
 * 主要用于Spring MVC等框架中,当需要对List类型的请求参数进行JSR-380/Jakarta Validation校验时使用。
 * 所有的List操作都委托给内部的list字段实现。
 * </p>
 *
 * @param <E> 列表中元素的类型
 * @author taige (Wu, Hongqiang)
 * @see List
 * @see jakarta.validation.Valid
 * @see ArrayList
 * @since 2020/6/16
 */
public class ValidList<E> implements List<E> {

    @Valid
    private List<E> list = new ArrayList<>();

    public List<E> getList() {
        return list;
    }

    public void setList(List<E> list) {
        this.list = list;
    }

    @Override
    public int size() {
        return list.size();
    }

    @Override
    public boolean isEmpty() {
        return list.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return list.contains(o);
    }

    @Override
    public Iterator<E> iterator() {
        return list.iterator();
    }

    @Override
    public Object[] toArray() {
        return list.toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return list.toArray(a);
    }

    @Override
    public boolean add(E e) {
        return list.add(e);
    }

    @Override
    public boolean remove(Object o) {
        return list.remove(o);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return list.containsAll(c);
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        return list.addAll(c);
    }

    @Override
    public boolean addAll(int index, Collection<? extends E> c) {
        return list.addAll(index, c);
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return list.removeAll(c);
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return list.retainAll(c);
    }

    @Override
    public void replaceAll(UnaryOperator<E> operator) {
        list.replaceAll(operator);
    }

    @Override
    public void sort(Comparator<? super E> c) {
        list.sort(c);
    }

    @Override
    public void clear() {
        list.clear();
    }

    @Override
    public boolean equals(Object o) {
        return list.equals(o);
    }

    @Override
    public int hashCode() {
        return list.hashCode();
    }

    @Override
    public E get(int index) {
        return list.get(index);
    }

    @Override
    public E set(int index, E element) {
        return list.set(index, element);
    }

    @Override
    public void add(int index, E element) {
        list.add(index, element);
    }

    @Override
    public E remove(int index) {
        return list.remove(index);
    }

    @Override
    public int indexOf(Object o) {
        return list.indexOf(o);
    }

    @Override
    public int lastIndexOf(Object o) {
        return list.lastIndexOf(o);
    }

    @Override
    public ListIterator<E> listIterator() {
        return list.listIterator();
    }

    @Override
    public ListIterator<E> listIterator(int index) {
        return list.listIterator(index);
    }

    @Override
    public List<E> subList(int fromIndex, int toIndex) {
        return list.subList(fromIndex, toIndex);
    }

    @Override
    public Spliterator<E> spliterator() {
        return list.spliterator();
    }

}
