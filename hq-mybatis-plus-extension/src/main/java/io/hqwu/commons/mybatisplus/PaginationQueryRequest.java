package io.hqwu.commons.mybatisplus;

import com.baomidou.mybatisplus.core.metadata.IPage;

import java.io.Serializable;

/**
 * 分页与排序查询请求的通用接口。
 * <p>
 * 该接口定义了前端发起分页查询时所需的基础参数规范，包括页码、每页数据量、排序字段及排序方向。
 * 通过集成此接口，可以配合 {@link PageHelper} 快速构建 MyBatis-Plus 的 {@link IPage} 对象，
 * 从而简化从 Controller 层到 Service 层的数据传递与分页逻辑转换。
 * </p>
 *
 * @param <E> 数据库实体类 (Entity)
 * @param <P> 返回给前端的 POJO/DTO 类
 */
public interface PaginationQueryRequest<E, P> extends Serializable {

    /**
     * default order by ascend
     * @return whether order by ascend
     */
    boolean isAscend();

    /**
     * get page size
     * @return page size
     */
    Integer getPageSize();

    /**
     * get current page number
     * @return current page number
     */
    Integer getPageNum() ;

    /**
     * get field name the result expected sorting by
     * @return field name the result to sort
     */
    String getSortField();

    /**
     * generate IPage object used to query with pagination
     * @return IPage object
     */
    default IPage<E> page() {
        return PageHelper.pageHelper(this);
    }

    /**
     * generate IPage object used to query with pagination
     * @param mainTable the left table alias name in sql statement
     * @return IPage object
     */
    default IPage<E> page(String mainTable) {
        return PageHelper.pageHelper(this, mainTable);
    }

}
