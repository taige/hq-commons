package io.hqwu.commons.mybatisplus;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.umpay.commons.util.StringUtil;
import org.hibernate.validator.constraints.Range;

import javax.validation.constraints.Min;
import javax.validation.constraints.Pattern;
import java.io.Serializable;
import java.util.StringJoiner;

/**
 * 支持分页和排序的前端查询请求抽象类
 * @param <E> 数据库实体类
 * @param <P> 返回给前端的pojo类
 */
public abstract class AbstractQueryRequest<E, P> implements Serializable {

    @Range(min = 1, max = 100)
    private Integer pageSize = 50;

    @Min(1)
    private Integer pageNum = 1;

    private String sortField;

    @Pattern(regexp = "ascend|descend")
    private String sortOrder;

    public AbstractQueryRequest() {
    }

    /**
     * default order by ascend
     * @return whether order by ascend
     */
    public boolean isAscend() {
        return StringUtil.isBlank(this.sortOrder) || this.sortOrder.equals("ascend");
    }

    /**
     * set request order by {@param sortField} ascend.
     *  mainly used in test code
     * @param sortField order field
     * @return          self
     */
    public <C extends AbstractQueryRequest<E, P>> C setAscend(String sortField) {
        return this.setSortOrder("ascend").setSortField(sortField);
    }

    /**
     * set request order by {@param sortField} descend.
     *  mainly used in test code
     * @param sortField order field
     * @return          self
     */
    public <C extends AbstractQueryRequest<E, P>> C setDescend(String sortField) {
        return this.setSortOrder("descend").setSortField(sortField);
    }

    public IPage<E> page() {
        return PageHelper.pageHelper(this);
    }

    public IPage<E> page(String mainTable) {
        return PageHelper.pageHelper(this, mainTable);
    }

    public <C extends AbstractQueryRequest<E, P>> C setPageSize(@Range(min = 1, max = 100) Integer pageSize) {
        this.pageSize = pageSize;
        return (C) this;
    }

    public <C extends AbstractQueryRequest<E, P>> C setPageNum(@Min(1) Integer pageNum) {
        this.pageNum = pageNum;
        return (C) this;
    }

    public <C extends AbstractQueryRequest<E, P>> C setSortField(String sortField) {
        this.sortField = sortField;
        return (C) this;
    }

    public <C extends AbstractQueryRequest<E, P>> C setSortOrder(@Pattern(regexp = "ascend|descend") String sortOrder) {
        this.sortOrder = sortOrder;
        return (C) this;
    }

    public @Range(min = 1, max = 100) Integer getPageSize() {
        return this.pageSize;
    }

    public @Min(1) Integer getPageNum() {
        return this.pageNum;
    }

    public String getSortField() {
        return this.sortField;
    }

    public @Pattern(regexp = "ascend|descend") String getSortOrder() {
        return this.sortOrder;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", AbstractQueryRequest.class.getSimpleName() + "[", "]")
                .add("pageSize=" + pageSize)
                .add("pageNum=" + pageNum)
                .add("sortField='" + sortField + "'")
                .add("sortOrder='" + sortOrder + "'")
                .toString();
    }
}
