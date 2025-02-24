package io.hqwu.commons.mybatisplus;

import com.baomidou.mybatisplus.core.conditions.AbstractLambdaWrapper;
import com.baomidou.mybatisplus.core.conditions.SharedString;
import com.baomidou.mybatisplus.core.conditions.query.Query;
import com.baomidou.mybatisplus.core.conditions.segments.MergeSegments;
import com.baomidou.mybatisplus.core.metadata.TableFieldInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.toolkit.ArrayUtils;
import com.baomidou.mybatisplus.core.toolkit.LambdaUtils;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.core.toolkit.support.SerializedLambda;
import io.hqwu.commons.util.StringUtil;
import org.apache.ibatis.reflection.property.PropertyNamer;
import org.springframework.lang.NonNull;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

/**
 * Created with IntelliJ IDEA for pp-gopay-fa
 *
 * 关联查询时代表 `主表` 的查询条件封装。
 *   主要为了实现：在where条件从句上自动添加主表前缀
 *
 * 大部分代码从 {@link com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper} 拷贝而来
 *
 * User: taige
 * Date: 2020/5/9
 * Time: 11:13
 */
public class MainLambdaQueryWrapper<T> extends AbstractLambdaWrapper<T, MainLambdaQueryWrapper<T>>
        implements Query<MainLambdaQueryWrapper<T>, T, SFunction<T, ?>> {

    private final String mainTable;

    /**
     * 查询字段
     */
    private SharedString sqlSelect = new SharedString();

    @Override
    protected String columnToString(SFunction<T, ?> column, boolean onlyColumn) {
        SerializedLambda lambda = LambdaUtils.resolve(column);
        String fieldName = PropertyNamer.methodToProperty(lambda.getImplMethodName());
        Class<?> aClass = lambda.getInstantiatedType();
        String joinColumn = PageHelper.getJoinColumn(aClass, fieldName);
        if (joinColumn != null) {
            return joinColumn;
        }
        return mainTable + super.columnToString(column, onlyColumn);
    }

    public MainLambdaQueryWrapper(@NonNull String mainTable) {
        this(null, mainTable);
    }

    public MainLambdaQueryWrapper(T entity, @NonNull String mainTable) {
        super.setEntity(entity);
        super.initNeed();
        mainTable = mainTable.replaceAll("\\.*$", "");
        this.mainTable = StringUtil.isNotBlank(mainTable) ? (mainTable + ".") : "";
    }

    MainLambdaQueryWrapper(T entity, Class<T> entityClass, SharedString sqlSelect, AtomicInteger paramNameSeq,
                           Map<String, Object> paramNameValuePairs, MergeSegments mergeSegments,
                           SharedString lastSql, SharedString sqlComment, SharedString sqlFirst,
                           @NonNull String mainTable) {
        super.setEntity(entity);
        super.setEntityClass(entityClass);
        this.paramNameSeq = paramNameSeq;
        this.paramNameValuePairs = paramNameValuePairs;
        this.expression = mergeSegments;
        this.sqlSelect = sqlSelect;
        this.lastSql = lastSql;
        this.sqlComment = sqlComment;
        this.sqlFirst = sqlFirst;
        mainTable = mainTable.replaceAll("\\.*$", "");
        this.mainTable = StringUtil.isNotBlank(mainTable) ? (mainTable + ".") : "";
    }

    /**
     * SELECT 部分 SQL 设置
     *
     * @param columns 查询字段
     */
    @SafeVarargs
    @Override
    public final MainLambdaQueryWrapper<T> select(SFunction<T, ?>... columns) {
        if (ArrayUtils.isNotEmpty(columns)) {
            this.sqlSelect.setStringValue(columnsToString(false, columns));
        }
        return typedThis;
    }

    @Override
    public MainLambdaQueryWrapper<T> select(Predicate<TableFieldInfo> predicate) {
        return select(getEntityClass(), predicate);
    }

    /**
     * 过滤查询的字段信息(主键除外!)
     * <p>例1: 只要 java 字段名以 "test" 开头的             -> select(i -&gt; i.getProperty().startsWith("test"))</p>
     * <p>例2: 只要 java 字段属性是 CharSequence 类型的     -> select(TableFieldInfo::isCharSequence)</p>
     * <p>例3: 只要 java 字段没有填充策略的                 -> select(i -&gt; i.getFieldFill() == FieldFill.DEFAULT)</p>
     * <p>例4: 要全部字段                                   -> select(i -&gt; true)</p>
     * <p>例5: 只要主键字段                                 -> select(i -&gt; false)</p>
     *
     * @param predicate 过滤方式
     * @return this
     */
    @Override
    public MainLambdaQueryWrapper<T> select(Class<T> entityClass, Predicate<TableFieldInfo> predicate) {
        if (entityClass == null) {
            entityClass = this.getEntityClass();
        } else {
            this.setEntityClass(entityClass);
        }
        this.sqlSelect.setStringValue(TableInfoHelper.getTableInfo(entityClass).chooseSelect(predicate));
        return typedThis;
    }

    @Override
    public String getSqlSelect() {
        return sqlSelect.getStringValue();
    }

    /**
     * 用于生成嵌套 sql
     * <p>故 sqlSelect 不向下传递</p>
     */
    @Override
    protected MainLambdaQueryWrapper<T> instance() {
        return new MainLambdaQueryWrapper<>(getEntity(), getEntityClass(), null, paramNameSeq, paramNameValuePairs,
                new MergeSegments(), SharedString.emptyString(), SharedString.emptyString(), SharedString.emptyString(), mainTable);
    }
}
