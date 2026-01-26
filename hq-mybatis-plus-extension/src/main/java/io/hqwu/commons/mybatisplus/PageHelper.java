package io.hqwu.commons.mybatisplus;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.core.toolkit.LambdaUtils;
import com.baomidou.mybatisplus.core.toolkit.support.ColumnCache;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.hqwu.commons.bean.BeanConverter;
import io.hqwu.commons.mybatisplus.annotation.JoinColumn;
import io.hqwu.commons.util.Logger;
import io.hqwu.commons.util.StringUtil;
import lombok.experimental.UtilityClass;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Map;

/**
 * 分页辅助工具类。
 *
 * <p>该工具类主要用于将前端的分页查询请求 {@link PaginationQueryRequest} 转换为 MyBatis Plus 的 {@link IPage} 对象。
 * 它能够自动处理排序逻辑，包括从 DTO/VO 属性名到实体类字段名、再到数据库列名的映射转换。
 *
 * <p>主要功能包括：
 * <ul>
 *   <li>实例化 {@link Page} 对象并设置分页参数（页码、每页大小）。</li>
 *   <li>解析排序字段，支持通过 {@link JoinColumn} 注解自定义排序列。</li>
 *   <li>支持在多表关联查询时为排序字段添加表别名前缀。</li>
 * </ul>
 *
 * <p>使用示例：
 * <pre>{@code
 * // 1. 定义请求对象
 * public class UserQueryRequest extends PaginationQueryRequest<UserEntity, UserVO> {}
 *
 * // 2. 在 Controller 或 Service 中使用
 * var request = new UserQueryRequest();
 * request.setPageNum(1);
 * request.setPageSize(10);
 * request.setSortField("userName"); // 对应 UserVO 的属性名
 *
 * IPage<UserEntity> page = PageHelper.pageHelper(request, "u");
 * }</pre>
 *
 * @author Wu, Hongqiang
 * @since 2020/5/8
 * @see PaginationQueryRequest
 * @see IPage
 * @see JoinColumn
 */
@UtilityClass
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
