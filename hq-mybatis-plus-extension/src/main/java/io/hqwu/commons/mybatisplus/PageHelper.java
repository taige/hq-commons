package io.hqwu.commons.mybatisplus;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.core.toolkit.LambdaUtils;
import com.baomidou.mybatisplus.core.toolkit.support.ColumnCache;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.umpay.commons.util.Logger;
import com.umpay.commons.util.StringUtil;
import io.hqwu.commons.bean.BeanConverter;
import io.hqwu.commons.mybatisplus.annotation.JoinColumn;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Map;

/**
 * Created with IntelliJ IDEA for pp-gopay-fa
 * User: taige
 * Date: 2020/5/8
 * Time: 22:44
 */
public class PageHelper {
    private static final Logger LOGGER = new Logger();

    /**
     * help to instantiate {@link IPage} and set sorting fields and order by asc/des
     * @param queryRequest Query Request pojo (contain pageNum/pageSize/sortingField/sortingOrder) from front-end
     * @param <T>          Table entity class
     * @return             {@link IPage} instance
     */
    public static <T> IPage<T> pageHelper(PaginationQueryRequest<T, ?> queryRequest) {
        return pageHelper(queryRequest, null);
    }

    public static <T> IPage<T> pageHelper(PaginationQueryRequest<T, ?> queryRequest, String leftTable) {
        Page<T> page = new Page<>(queryRequest.getPageNum(), queryRequest.getPageSize());
        if (StringUtil.isNotBlank(queryRequest.getSortField())) {
            String orderColumn = getOrderColumn(queryRequest, leftTable);
            if (orderColumn != null) {
                page.setOrders(queryRequest.isAscend() ? OrderItem.ascs(orderColumn) : OrderItem.descs(orderColumn));
            }
        }
        return page;
    }


    public static String getJoinColumn(Class<?> entityClass, String fieldName) {
        try {
            Field field = entityClass.getDeclaredField(fieldName);
            if (field.isAnnotationPresent(JoinColumn.class)) {
                JoinColumn annJoinColumn = field.getAnnotation(JoinColumn.class);
                String joinColumn = annJoinColumn.value();
                if (StringUtil.isNotBlank(joinColumn)) {
                    return joinColumn;
                }
            }
        } catch (NoSuchFieldException ignored) {
        }
        return null;
    }

    protected static String getOrderColumn(PaginationQueryRequest<?, ?> queryRequest, String leftTable) {
        Type[] types = ((ParameterizedType) queryRequest.getClass().getGenericSuperclass()).getActualTypeArguments();
        if (types[0] instanceof Class && types[1] instanceof Class) {
            Class<?> entityClass = (Class<?>) types[0];
            Class<?> pojoClass = (Class<?>) types[1];
            String entityProperty = BeanConverter.getSourcePropertyName(entityClass, pojoClass, queryRequest.getSortField());
            if (entityProperty == null) {
                LOGGER.debug("can not find property `%s` for this entity [%s]",
                        queryRequest.getSortField(), entityClass.getName());
                return null;
            }
            String joinColumn = getJoinColumn(entityClass, entityProperty);
            if (joinColumn != null) {
                return joinColumn;
            }
            Map<String, ColumnCache> columnMap = LambdaUtils.getColumnMap(entityClass);
            if (columnMap == null) {
                LOGGER.warn("can not find column cache for this entity [%s]", entityClass.getName());
                return null;
            }
            ColumnCache columnCache = columnMap.get(LambdaUtils.formatKey(entityProperty));
            if (columnCache == null) {
                LOGGER.warn("can not find column `%s`of entity [%s]", entityProperty, entityClass.getName());
                return null;
            }
            String orderColumn = columnCache.getColumn();
            if (null != leftTable) {
                leftTable = leftTable.replaceAll("\\.*$", "");
                if (StringUtil.isNotBlank(leftTable)) {
                    orderColumn = leftTable.replaceAll("\\.*$", "") + "." + orderColumn;
                }
            }
            return orderColumn;
        }
        LOGGER.warn("ActualType of [%s] are not Class: %s, %s", queryRequest.getClass().getName(),
                types[0].getTypeName(), types[1].getTypeName());
        return null;
    }

}
