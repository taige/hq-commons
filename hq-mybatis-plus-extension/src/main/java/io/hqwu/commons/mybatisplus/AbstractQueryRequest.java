package io.hqwu.commons.mybatisplus;

import io.hqwu.commons.util.StringUtil;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import org.hibernate.validator.constraints.Range;

import java.util.StringJoiner;

/**
 * Created with IntelliJ IDEA for hq-mybatis-plus-extension
 *
 * @author taige (Wu, Hongqiang)
 * Date: 2021-04-04
 * Time: 9:45 a.m.
 */
public abstract class AbstractQueryRequest<E, P> implements PaginationQueryRequest<E, P> {

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
    @Override
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

    @Override
    public @Range(min = 1, max = 100) Integer getPageSize() {
        return this.pageSize;
    }

    @Override
    public @Min(1) Integer getPageNum() {
        return this.pageNum;
    }

    @Override
    public String getSortField() {
        return this.sortField;
    }

    public @Pattern(regexp = "ascend|descend") String getSortOrder() {
        return this.sortOrder;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", this.getClass().getSimpleName() + "[", "]")
                .add("pageSize=" + pageSize)
                .add("pageNum=" + pageNum)
                .add("sortField='" + sortField + "'")
                .add("sortOrder='" + sortOrder + "'")
                .toString();
    }
}
