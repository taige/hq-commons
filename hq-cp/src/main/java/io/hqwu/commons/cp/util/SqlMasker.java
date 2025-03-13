package io.hqwu.commons.cp.util;

import io.hqwu.commons.util.Logger;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.*;
import net.sf.jsqlparser.expression.operators.relational.*;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.StatementVisitorAdapter;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.select.*;
import net.sf.jsqlparser.statement.update.Update;
import net.sf.jsqlparser.statement.update.UpdateSet;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created with IntelliJ IDEA for hq-commons-parent
 * <br/>
 * 对SQL中的敏感字段进行mask <br/>
 * 支持以下SQL的mask： <br/>
 *     - INSERT语句:  <br/>
 *        1. 单行INSERT语句，ex.
 *                INSERT INTO users (id, name, password, email) VALUES
 *                                  (1, 'John', 'secret123', 'john@example.com') <br/>
 *        2. 多行INSERT语句，ex.
 *                INSERT INTO users (id, name, password, email) VALUES
 *                                  (1, 'John', 'secret123', 'john@example.com'),
 *                                  (2, 'Mary', 'secret456', 'mary@example.com') <br/>
 *        3. INSERT ... SELECT 语句，ex.
 *                INSERT INTO backup_users (id, name, password, email)
 *                                   SELECT id, name, password, email FROM users WHERE password = '' OR password='simple_password' <br/>
 *        4. INSERT ... ON DUPLICATE KEY UPDATE（MySQL 特有），ex.
 *                INSERT INTO table_name (id, password) VALUES (123, 'secret') ON DUPLICATE KEY UPDATE password = 'new_secret'
 *      - UPDATE语句:  <br/>
 *         1. 基础UPDATE语句，ex.
 *                UPDATE users SET enabled = true, password = 'new_password' WHERE id = 1 <br/>
 *                UPDATE users SET enabled = false WHERE password = '' OR password='simple_password' <br/>
 *         2. 带JOIN的UPDATE, ex.
 *                UPDATE users u JOIN temp_users t ON u.id = t.id SET u.password = t.password, t.sync = true WHERE u.password = 'simple_password' <br/>
 *         3. UPDATE ... CASE（条件更新），ex.
 *                UPDATE table_name
 *                  SET column1 = CASE
 *                      WHEN condition1 THEN 'secret1'
 *                      WHEN condition2 THEN 'secret2'
 *                      ELSE column1
 *                  END
 *                  WHERE condition;
 *       - SELECT语句:  <br/>
 *          1. 基础SELECT语句，ex.
 *                SELECT column1, column2 FROM table_name WHERE password = '' OR password='simple_password' <br/>
 *          2. 带JOIN的SELECT, ex.
 *                SELECT t1.column1, t2.column2 FROM table1 t1 JOIN table2 t2 ON t1.id = t2.id WHERE password = '' OR password='simple_password' <br/>
 *          3. 支持UNION, UNION ALL <br/>
 *       - DELETE语句:  <br/>
 *          1. 基础DELETE语句，ex.
 *                DELETE FROM users WHERE password = '' OR password='simple_password' <br/>
 *          2. DELETE ... FROM ... JOIN（部分数据库支持，如 MySQL）
 *                DELETE t1, t2 FROM table1 t1 JOIN table2 t2 ON t1.id = t2.id WHERE password = '' OR password='simple_password' <br/>
 * 对于需要脱敏字段的判断，需要支持以下几种情况： <br/>
 *      1. 普通字段名（password）<br/>
 *      2. 带表别名（u.password）<br/>
 *      3. 带数据库前缀（db1.users.password）<br/>
 * 对于上述语句 WHERE condition 子句的mask，支持以下几种情况： <br/>
 *      1. 比较操作符：=, <, <=, >, >=, <>, != (column可以在操作符的左右任意一边)<br/>
 *      2. 逻辑操作符: AND, OR, NOT <br/>
 *      3. 空值判断：IS NULL, IS NOT NULL <br/>
 *      4. LIKE操作符: 首尾的 % _ 不被mask，其余部分mask，如 '%secret%' mask结果是 '%s****t%' (maskPattern="****") <br/>
 *      5. 范围操作符: BETWEEN, IN<br/>
 *      6. 子查询: (SELECT ... FROM ... WHERE ...) <br/>
 *              WHERE salary > (SELECT AVG(salary) FROM employees WHERE password = 'simple_password')
 *      7. EXISTS操作符: EXISTS (SELECT ... FROM ... WHERE ...) <br/>
 *              WHERE EXISTS (SELECT 1 FROM users WHERE users.customer_id = customers.id and users.password = 'simple_password')
 * 脱敏后的sql语句，除了脱敏字段的值，其他内容跟输入的sql保持一致<br/>
 *
 * @author taige (Wu, Hongqiang)
 * Date: 2025-03-11
 * Time: 08:48
 */
public class SqlMasker {
    private static final Logger LOGGER = new Logger();

    /**
     * 脱敏模式，如"***", "####", "????", "*#?●○"
     */
    protected final String maskPattern;

    /**
     * 敏感字段集合
     */
    private final Set<String> sensitiveFields;

    private final MaskStatementVisitor maskStatementVisitor;

    private final MaskSelectVisitor maskSelectVisitor;

    private final MaskExpressionVisitor maskExpressionVisitor;

    /**
     * 构造函数
     * @param maskPattern 脱敏模式，如"***", "####", "????", "*#?●○"
     * @param sensitiveFields 敏感字段集合
     */
    public SqlMasker(String maskPattern, Set<String> sensitiveFields) {
        this.maskPattern = maskPattern;
        // 将敏感字段转换为小写以确保大小写不敏感
        this.sensitiveFields = sensitiveFields.stream()
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
        this.maskStatementVisitor = new MaskStatementVisitor();
        this.maskSelectVisitor = new MaskSelectVisitor();
        this.maskExpressionVisitor = new MaskExpressionVisitor();
        this.maskExpressionVisitor.setSelectVisitor(this.maskSelectVisitor);
    }

    /**
     * 对SQL中的敏感字段进行mask
     * @param sql 输入的 SQL 语句
     * @param sensitiveFields 敏感字段集合
     * @param maskPattern 脱敏模式（例如 "****"）
     * @return 脱敏后的 SQL 语句
     */
    static String maskSensitiveFields(String sql, Set<String> sensitiveFields, String maskPattern) {

        return new SqlMasker(maskPattern, sensitiveFields).maskSensitiveFields(sql);
    }

    /**
     * 对SQL中的敏感字段进行mask
     * @param sql 输入的 SQL 语句
     * @return 脱敏后的 SQL 语句    
     */
    public String maskSensitiveFields(String sql) {
        try {
            // Parse SQL into a Statement object
            Statement statement = CCJSqlParserUtil.parse(sql);
            // Use a custom visitor to traverse and modify the SQL AST
            statement.accept(this.maskStatementVisitor);
            // Return the modified SQL statement
            return statement.toString();
        } catch (JSQLParserException e) {
            LOGGER.warn("Failed to mask sensitive fields in SQL: {}", sql, e);
            return sql;
        }
    }

    private class MaskStatementVisitor extends StatementVisitorAdapter {

        /**
         * 处理 UPDATE 语句
         */
        @Override
        public void visit(Update update) {
            // 1. 基础UPDATE语句， UPDATE ... SET ...
            maskUpdateSets(update.getUpdateSets());

            // 2. 带JOIN的UPDATE，UPDATE ... JOIN ... ON ...
            // 处理 WHERE 条件中的敏感字段
            maskJoinsAndWhere(update::getJoins, update::getWhere);
        }

        /**
         * 处理 DELETE 语句
         */
        @Override
        public void visit(Delete delete) {
            // 1. 基础DELETE语句， DELETE ... FROM ... WHERE ...
            // 2. DELETE ... FROM ... JOIN（部分数据库支持，如 MySQL）
            // ex. DELETE t1 FROM table1 t1 JOIN table2 t2 ON t1.id = t2.id WHERE t2.status = 'inactive';
            maskJoinsAndWhere(delete::getJoins, delete::getWhere);
        }

        /**
         * 处理 INSERT 语句
         */
        @Override
        public void visit(Insert insert) {
            Select select = insert.getSelect();
            if (select instanceof Values) {
                List<Column> columns = insert.getColumns();
                Values values = (Values) select;
                ExpressionList<? extends Expression> expressions = values.getExpressions();
                for (int i = 0; i < expressions.size(); i++) {
                    Expression expression = expressions.get(i);
                    if (expression instanceof ParenthesedExpressionList) {
                        // 2. 多行INSERT语句，INSERT ... VALUES (1, '2', '3'), (4, '5', '6')
                        ParenthesedExpressionList<? extends Expression> valueList = (ParenthesedExpressionList<? extends Expression>) expression;
                        for (int j = 0; j < valueList.size(); j++) {
                            Expression expr = valueList.get(j);
                            maskFieldValueIfSensitive(columns.get(j), expr);
                        }
                    } else {
                        // 1. 单行INSERT语句，INSERT ... VALUES (1, '2', '3')
                        maskFieldValueIfSensitive(columns.get(i), expression);
                    }
                }
            } else if (select != null) {
                //3. INSERT ... SELECT 语句
                select.accept(SqlMasker.this.maskSelectVisitor);
            }
            // 4. INSERT ... ON DUPLICATE KEY UPDATE（MySQL 特有）
            // ex. INSERT INTO table_name (id, password) VALUES (123, 'secret') ON DUPLICATE KEY UPDATE password = 'new_secret'
            maskUpdateSets(insert.getDuplicateUpdateSets());
        }

        /**
         * 处理 SELECT 语句
         */
        @Override
        public void visit(Select select) {
            select.accept(SqlMasker.this.maskSelectVisitor);
        }

        /**
         * 处理 UPDATE SET 语句
         * @param updateSets
         */
        private void maskUpdateSets(List<UpdateSet> updateSets) {
            if (updateSets == null || updateSets.isEmpty()) {
                return;
            }
            updateSets.forEach(updateSet -> {
                ExpressionList<Column> columns = updateSet.getColumns();
                ExpressionList<? extends Expression> expressions = updateSet.getValues();
                if (expressions.get(0) instanceof ParenthesedSelect) {
                    // UPDATE ... SET (a, b, c) = (VALUES '1', '2', '3')
                    Values values = (Values) ((ParenthesedSelect) expressions.get(0)).getSelect();
                    expressions = values.getExpressions();
                }
                for (int i = 0; i < columns.size(); i++) {
                    Column column = columns.get(i);
                    Expression expression = expressions.get(i);
                    if (expression instanceof CaseExpression) {
                        // 3. UPDATE ... SET column1 = CASE ... END
                        maskCaseExpression(column, (CaseExpression) expression);
                    } else {
                        // UPDATE ... SET column2 = (SELECT ... )
                        // UPDATE ... SET column1 = value1 ...
                        maskExpression(column, expression);
                    }
                }
            });
        }

        /**
         * 处理 CASE 表达式
         * @param column
         * @param caseExpr
         */
        private void maskCaseExpression(Column column, CaseExpression caseExpr) {
            Column switchColumn = null;
            if (caseExpr.getSwitchExpression() instanceof Column) {
                // 3.1 UPDATE ... SET column1 = CASE switchColumn WHEN ... THEN ... ELSE ...END
                switchColumn = (Column) caseExpr.getSwitchExpression();
            }
            for (WhenClause when : caseExpr.getWhenClauses()) {
                Expression whenExpr = when.getWhenExpression();
                if (switchColumn != null) {
                    // 3.1.1 UPDATE ... SET column1 = CASE switchColumn WHEN (SELECT ... ) THEN ... ELSE ...END
                    // 3.1.2 UPDATE ... SET column1 = CASE switchColumn WHEN 'secret' THEN ... ELSE ...END
                    maskExpression(switchColumn, whenExpr);
                } else {
                    // 3.2 UPDATE ... SET column1 = CASE WHEN password='secret' THEN ... ELSE ...END
                    whenExpr.accept(SqlMasker.this.maskExpressionVisitor);
                }
                
                // 3.3 UPDATE ... SET column1 = CASE ... THEN (SELECT ... ) ELSE ...END    
                // 3.4 UPDATE ... SET column1 = CASE ... THEN 'secret' ELSE ...END
                maskExpression(column, when.getThenExpression());
            }
            // 3.5 UPDATE ... SET column1 = CASE ... ELSE (SELECT ... ) END
            // 3.6 UPDATE ... SET column1 = CASE ... ELSE 'secret' END
            maskExpression(column, caseExpr.getElseExpression());
        }

        /**
         * mask expression if it is a sensitive field
         * @param column
         * @param expression
         */
        private void maskExpression(Column column, Expression expression) {
            if (expression instanceof Select) {
                ((Select) expression).accept(SqlMasker.this.maskSelectVisitor);
            } else if (expression != null) {
                maskFieldValueIfSensitive(column, expression);
            }
        }

        /**
         * 处理 JOIN 和 WHERE 条件中的敏感字段
         * @param joinsProvider
         * @param whereProvider
         */
        private void maskJoinsAndWhere(JoinsProvider joinsProvider, WhereProvider whereProvider) {
            if (joinsProvider.getJoins() != null) {
                joinsProvider.getJoins().forEach(join -> {
                    join.getOnExpressions().forEach(expression -> {
                        expression.accept(SqlMasker.this.maskExpressionVisitor);
                    });
                });
            }
            if (whereProvider.getWhere() != null) {
                whereProvider.getWhere().accept(SqlMasker.this.maskExpressionVisitor);
            }
        }

    }

    private class MaskSelectVisitor extends SelectVisitorAdapter {

        /**
         * 单一的 SELECT 查询，通常包含 SELECT 子句、FROM 子句、WHERE 子句、GROUP BY、HAVING、ORDER BY 等部分
         * @param plainSelect
         */
        @Override
        public void visit(PlainSelect plainSelect) {
            // SELECT * FROM (SELECT ... )
            if (plainSelect.getFromItem() instanceof ParenthesedSelect) {
                ParenthesedSelect parenthesedSelect = (ParenthesedSelect) plainSelect.getFromItem();
                parenthesedSelect.getSelect().accept(this);
            }
            // SELECT * FROM ... JOIN ...
            if (plainSelect.getJoins() != null) {
                plainSelect.getJoins().forEach(join -> {
                    // SELECT * FROM ... JOIN (SELECT ... )
                    if (join.getRightItem() instanceof ParenthesedSelect) {
                        ((ParenthesedSelect) join.getRightItem()).getSelect().accept(this);
                    }
                    // SELECT * FROM ... JOIN ... ON ...
                    join.getOnExpressions().forEach(expression -> {
                        expression.accept(SqlMasker.this.maskExpressionVisitor);
                    });
                });
            }
            // SELECT * FROM ... WHERE ...
            if (plainSelect.getWhere() != null) {
                plainSelect.getWhere().accept(SqlMasker.this.maskExpressionVisitor);
            }
        }

        /**
         * 被括号包裹的 SELECT 查询，例如作为子查询或在 UNION 操作中
         * @param parenthesedSelect
         */
        @Override
        public void visit(ParenthesedSelect parenthesedSelect) {
            parenthesedSelect.getSelect().accept(this);
        }

        /**
         * 多个 SELECT 查询通过集合操作（如 UNION、INTERSECT、EXCEPT 等）连接而成的组合查询
         * @param setOpList
         */
        @Override
        public void visit(SetOperationList setOpList) {
            setOpList.getSelects().forEach(select -> {
                select.accept(this);
            });
        }

        /**
         * SQL 中 LATERAL 关键字引入的子查询，例如： <br/>
         *     SELECT * FROM t1, LATERAL (SELECT * FROM t2 WHERE t2.id = t1.id) AS t2 <br/>
         * @param subSelect
         */
        @Override
        public void visit(LateralSubSelect subSelect) {
            subSelect.getSelect().accept(this);
        }
        
    }

    /**
     * A visitor to traverse expressions and mask sensitive field comparisons.
     */
    private class MaskExpressionVisitor extends ExpressionVisitorAdapter {

        /**
         * 比较操作符：=, <, <=, >, >=, <>, != (column可以在操作符的左右任意一边)
         * @param expr 二元表达式
         */
        private void _visitBinaryExpression(BinaryExpression expr) {
            maskIfSensitive(expr.getLeftExpression(), expr.getRightExpression());
            // 继续递归处理左右两侧的表达式
            expr.getLeftExpression().accept(this);
            expr.getRightExpression().accept(this);
        }

        @Override
        public void visit(EqualsTo equalsTo) {
            _visitBinaryExpression(equalsTo);
        }

        @Override
        public void visit(NotEqualsTo notEqualsTo) {
            _visitBinaryExpression(notEqualsTo);
        }

        @Override
        public void visit(GreaterThan greaterThan) {
            _visitBinaryExpression(greaterThan);
        }

        @Override
        public void visit(GreaterThanEquals greaterThanEquals) {
            _visitBinaryExpression(greaterThanEquals);
        }

        @Override
        public void visit(MinorThan minorThan) {
            _visitBinaryExpression(minorThan);
        }

        @Override
        public void visit(MinorThanEquals minorThanEquals) {
            _visitBinaryExpression(minorThanEquals);
        }
    }

    /**
     * 如果左右两侧的表达式中任意一侧是敏感字段，则对敏感字段的值进行mask
     * @param left
     * @param right
     */
    private void maskIfSensitive(Expression left, Expression right) {
        if (left instanceof Column) {
            maskFieldValueIfSensitive((Column) left, right);
        } else if (right instanceof Column) {
            maskFieldValueIfSensitive((Column) right, left);
        }
    }

    /**
     * 如果字段是敏感字段，则对字段值进行mask，否则不处理
     * @param column 字段
     * @param expr 表达式
     */
    private void maskFieldValueIfSensitive(Column column, Expression expr) {
        if (column == null || ! sensitiveFields.contains(column.getColumnName().toLowerCase())) {
            return;
        }
        if (expr instanceof StringValue) {
            StringValue stringValue = (StringValue) expr;
            stringValue.setValue(maskSensitiveValue(stringValue.getValue()));
        }
    }

    protected String maskSensitiveValue(String field) {
        return doValueMask(field, this.maskPattern);
    }

    /**
     * 对(敏感)字段值进行mask <br/>
     *    mask规则：<br/>
     *      1. 如果value长度 <= maskPattern长度，则将value全部mask <br/>
     *      2. 如果value长度介于maskPattern长度的1-3倍之间，则将value中间部分mask，多余的字符数平均到首尾保留下来，不能平均时，将多余字符数保留到首位 <br/>
     *      3. 如果value长度 > maskPattern长度的3倍，收尾保留maskPattern长度，中间部分用 maskPattern 循环填充保证替换完后的字段长度跟原来一致 <br/>
     * @param value 原始值
     * @param maskPattern mask模式, 如"***", "####", "????", "*#?●○"
     * @return mask后的值
     */
    static String doValueMask(String value, String maskPattern) {
        if (value == null || maskPattern == null || maskPattern.isEmpty()) {
            return value;
        }

        int fLen = value.length();
        int pLen = maskPattern.length();

        // 规则1：字段长度 <= 掩码长度，全部mask
        if (fLen <= pLen) {
            return maskPattern;
        }

        StringBuilder sb = new StringBuilder(fLen);

        // 规则2：字段长度在掩码长度1-3倍之间
        if (fLen <= pLen * 3) {
            int remain = fLen - pLen;
            int front = (remain + 1) / 2;  // 不能平均时多余字符保留到首位
            sb.append(value, 0, front)
                    .append(maskPattern)
                    .append(value, fLen - remain/2, fLen);
            return sb.toString();
        }

        // 规则3：字段长度大于掩码长度3倍
        int midLen = fLen - 2 * pLen;
        sb.append(value, 0, pLen);  // 前缀

        // 循环填充中间部分
        for (int i = 0; i < midLen / pLen; i++) {
            sb.append(maskPattern);
        }
        if (midLen % pLen > 0) {
            sb.append(maskPattern, 0, midLen % pLen);
        }

        sb.append(value, fLen - pLen, fLen);  // 后缀
        return sb.toString();
    }

}

@FunctionalInterface
interface JoinsProvider {
    List<Join> getJoins();
}

@FunctionalInterface
interface WhereProvider {
    Expression getWhere();
}

