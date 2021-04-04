package io.hqwu.commons.mybatisplus;

import com.baomidou.mybatisplus.core.metadata.IPage;

import java.io.Serializable;

/**
 * 支持分页和排序的前端查询请求接口
 * @param <E> 数据库实体类
 * @param <P> 返回给前端的pojo类
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
