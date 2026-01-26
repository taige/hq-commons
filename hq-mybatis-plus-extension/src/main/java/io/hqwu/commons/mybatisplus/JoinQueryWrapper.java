package io.hqwu.commons.mybatisplus;

import com.baomidou.mybatisplus.core.conditions.SharedString;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.segments.MergeSegments;
import com.baomidou.mybatisplus.core.metadata.TableFieldInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.toolkit.ArrayUtils;
import com.baomidou.mybatisplus.core.toolkit.StringPool;
import org.springframework.lang.NonNull;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

/**
 * 关联查询条件封装类。
 *
 * <p>主要用于在执行多表关联查询时，封装“从表（Secondary Table）”的查询条件及字段选择逻辑。</p>
 * <p>该类大部分代码从 {@link QueryWrapper} 拷贝而来，并扩展了对多表场景下 SQL 片段的处理。通过 {@link #lambda(String)}
 * 方法可以转换为 {@link MainLambdaQueryWrapper}，从而支持强类型的 Lambda 表达式关联查询。</p>
 *
 * @param <T> 实体类泛型
 * @author taige
 * @since 2020/5/9
 */
public class JoinQueryWrapper<T> extends QueryWrapper<T> {

    /**
     * 查询字段
     */
    private SharedString sqlSelect;

    public JoinQueryWrapper() {
        super();
        sqlSelect = new SharedString();
    }

    public JoinQueryWrapper(T entity) {
        super(entity);
        sqlSelect = new SharedString();
    }

    public JoinQueryWrapper(T entity, String... columns) {
        this.sqlSelect = new SharedString();
        super.setEntity(entity);
        super.initNeed();
        if (ArrayUtils.isNotEmpty(columns)) {
            select(columns);
        }
    }

    /**
     * 非对外公开的构造方法,只用于生成嵌套 sql
     */
    private JoinQueryWrapper(T entity, Class<T> entityClass, AtomicInteger paramNameSeq,
                             Map<String, Object> paramNameValuePairs, MergeSegments mergeSegments, SharedString paramAlias,
                             SharedString lastSql, SharedString sqlComment, SharedString sqlFirst) {
        super.setEntity(entity);
        super.setEntityClass(entityClass);
        this.paramNameSeq = paramNameSeq;
        this.paramNameValuePairs = paramNameValuePairs;
        this.expression = mergeSegments;
        this.paramAlias = paramAlias;
        this.lastSql = lastSql;
        this.sqlComment = sqlComment;
        this.sqlFirst = sqlFirst;
    }

    @Override
    public QueryWrapper<T> select(String... columns) {
        if (ArrayUtils.isNotEmpty(columns)) {
            this.sqlSelect.setStringValue(String.join(StringPool.COMMA, columns));
        }
        return typedThis;
    }

    @Override
    public QueryWrapper<T> select(Class<T> entityClass, Predicate<TableFieldInfo> predicate) {
        this.setEntityClass(entityClass);
        this.sqlSelect.setStringValue(TableInfoHelper.getTableInfo(getEntityClass()).chooseSelect(predicate));
        return typedThis;
    }

    @Override
    public String getSqlSelect() {
        return sqlSelect.getStringValue();
    }

    /**
     * 用于生成嵌套 sql
     * <p>
     * 故 sqlSelect 不向下传递
     * </p>
     */
    @Override
    protected JoinQueryWrapper<T> instance() {
        return new JoinQueryWrapper<>(getEntity(), getEntityClass(), paramNameSeq, paramNameValuePairs, new MergeSegments(),
                paramAlias, SharedString.emptyString(), SharedString.emptyString(), SharedString.emptyString());
    }

    /**
     * 返回一个支持 lambda 函数写法的 wrapper
     */
    public MainLambdaQueryWrapper<T> lambda(@NonNull String mainTable) {
        return new MainLambdaQueryWrapper<>(getEntity(), getEntityClass(), sqlSelect, paramNameSeq, paramNameValuePairs,
        expression, paramAlias, lastSql, sqlComment, sqlFirst, mainTable);
    }

}
