package io.hqwu.commons.cp.util;

import io.hqwu.commons.util.Logger;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.*;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.conditional.OrExpression;
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
 * 基于 JSQLParser 实现的 SQL 敏感字段脱敏工具类。
 * <br/>
 * 对 SQL 中的敏感字段进行mask <br/>
 * 支持以下SQL的mask： <br/>
 *     - INSERT语句:  <br/>
 *      ✅  1. 单行INSERT语句，ex. <br/>
 *                INSERT INTO users (id, name, password, email) VALUES
 *                                  (1, 'John', 'secret123', 'john@example.com') <br/>
 *      ✅  2. 多行INSERT语句，ex. <br/>
 *                INSERT INTO users (id, name, password, email) VALUES
 *                                  (1, 'John', 'secret123', 'john@example.com'),
 *                                  (2, 'Mary', 'secret456', 'mary@example.com') <br/>
 *      ✅  3. INSERT ... SELECT 语句，ex. <br/>
 *                INSERT INTO backup_users (id, name, password, email)
 *                                   SELECT id, name, password, email FROM users WHERE password = '' OR password='simple_password' <br/>
 *      ✅  4. INSERT ... ON DUPLICATE KEY UPDATE（MySQL 特有），ex. <br/>
 *                INSERT INTO table_name (id, password) VALUES (123, 'secret') ON DUPLICATE KEY UPDATE password = 'new_secret' <br/>
 *      - UPDATE语句:  <br/>
 *      ✅  1. 基础UPDATE语句，ex. <br/>
 *                UPDATE users SET enabled = true, password = 'new_password' WHERE id = 1 <br/>
 *                UPDATE users SET enabled = false WHERE password = '' OR password='simple_password' <br/>
 *      ✅  2. 带JOIN的UPDATE, ex. <br/>
 *                UPDATE users u JOIN temp_users t ON u.id = t.id SET u.password = t.password, t.sync = true WHERE u.password = 'simple_password' <br/>
 *      ✅  3. UPDATE ... CASE（条件更新），ex. <br/>
 *                UPDATE table_name
 *                  SET column1 = CASE
 *                      WHEN condition1 THEN 'secret1'
 *                      WHEN condition2 THEN 'secret2'
 *                      ELSE column1
 *                  END
 *                  WHERE condition;  <br/>
 *       - SELECT语句:  <br/>
 *      ✅  1. 基础SELECT语句，ex. <br/>
 *                SELECT column1, column2 FROM table_name WHERE password = '' OR password='simple_password' <br/>
 *      ✅  2. 带JOIN的SELECT, ex. <br/>
 *                SELECT t1.column1, t2.column2 FROM table1 t1 JOIN table2 t2 ON t1.id = t2.id WHERE password = '' OR password='simple_password' <br/>
 *      ✅  3. 支持UNION, UNION ALL <br/>
 *       - DELETE语句:  <br/>
 *      ✅  1. 基础DELETE语句，ex. <br/>
 *                DELETE FROM users WHERE password = '' OR password='simple_password' <br/>
 *      ✅  2. DELETE ... FROM ... JOIN（部分数据库支持，如 MySQL）<br/>
 *                DELETE t1, t2 FROM table1 t1 JOIN table2 t2 ON t1.id = t2.id WHERE password = '' OR password='simple_password' <br/>
 * 对于需要脱敏字段的判断，需要支持以下几种情况： <br/>
 *      1. 普通字段名（password）<br/>
 *      2. 带表别名（u.password）<br/>
 *      3. 带数据库前缀（db1.users.password）<br/>
 * 对于上述语句 WHERE condition 子句的mask，支持以下几种情况： <br/>
 *      ✅  1. 比较操作符：=, <, <=, >, >=, <>, != (column可以在操作符的左右任意一边)<br/>
 *      ✅  2. 逻辑操作符: AND, OR, NOT <br/>
 *      ✅  3. 空值判断：IS NULL, IS NOT NULL 【不需要特殊实现，不会有敏感值出现在该condition中】<br/>
 *      ✅  4. LIKE操作符: 首尾的 % _ 不被mask，其余部分mask，如 '%secret%' mask结果是 '%s****t%' (maskPattern="****") <br/>
 *      ✅  5. 范围操作符: BETWEEN, IN<br/>
 *      ✅  6. 子查询: (SELECT ... FROM ... WHERE ...) <br/>
 *              WHERE salary > (SELECT AVG(salary) FROM employees WHERE password = 'simple_password') <br/>
 *      ✅  7. EXISTS操作符: EXISTS (SELECT ... FROM ... WHERE ...) <br/>
 *              WHERE EXISTS (SELECT 1 FROM users WHERE users.customer_id = customers.id and users.password = 'simple_password') <br/>
 * 脱敏后的sql语句，除了脱敏字段的值，其他内容跟输入的sql保持一致<br/>
 * <br/>
 * 未实现/暂不支持的语法: <br/>
 *     - 敏感字段值使用了函数：INSERT INTO users (password) VALUES (MD5('secret')); <br/>
 *     - 敏感字段使用了函数：SELECT * FROM users WHERE LOWER(password) = 'secret'; <br/>
 *     - 非字符串类型的敏感字段：UPDATE users SET key = 0x1234ABCD; <br/>
 *     - 转义符(') MySQL转义符(\) 当成普通字符处理，即忽略语义层面的字符串；<br/>
 *          比如：VALUES('''1234567') - 语义层面是长度为8的字符串 "'1234567"，<br/>
 *               但脱敏时按长度为9的字符串处理，脱敏结果为：VALUES('''1****67')，<br/>
 *               可能导致输出的SQL不是合法的。（仅用于日志脱敏，故暂忽略）
 * @author taige (Wu, Hongqiang)
 * @since 2025-03-11
 */
public class SqlMasker {
    private static final Logger LOGGER = new Logger();

    /**
     * 脱敏模式，如"***", "####", "????", "*#?●○"
     */
    protected final String maskPattern;

    /**
     * 敏感字段集合（小写）
     */
    private final Set<String> sensitiveFields;

    private final MaskStatementVisitor maskStatementVisitor;

    private final MaskSelectVisitor maskSelectVisitor;

    private final MaskExpressionVisitor maskExpressionVisitor;

    /**
     * 构造函数
     * @param maskPattern 脱敏模式：如"***", "####", "????", "*#?●○"
     *                    脱敏算法见 {@link #doValueMask(String, String)} 的注释
     * @param sensitiveFields 敏感字段集合：字段名不区分大小写，且会)
     */
    public SqlMasker(String maskPattern, Set<String> sensitiveFields) {
        this.maskPattern = maskPattern;
        // 将敏感字段转换为小写以确保大小写不敏感
        this.sensitiveFields = sensitiveFields == null ? null : sensitiveFields.stream()
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
        // 【性能优化】快速检查：如果原始 SQL 字符串中连敏感字段名都没有，直接返回
        // 避免昂贵的 SQL 解析 (Parse) 开销
        if (! containsAnySensitiveField(sql)) {
            return sql;
        }

        try {
            // Parse SQL into a Statement object
            Statement statement = CCJSqlParserUtil.parse(sql,
                    // 开启 MySQL 风格的反斜杠转义支持
                    parser -> parser.withBackslashEscapeCharacter(true));
            // Use a custom visitor to traverse and modify the SQL AST
            statement.accept(this.maskStatementVisitor);
            // Return the modified SQL statement
            return statement.toString();
        } catch (JSQLParserException e) {
            LOGGER.warn("Failed to mask sensitive fields in SQL: {}", sql, e);
            return sql;
        }
    }

    /**
     * 快速检查 SQL 是否包含任意敏感字段名（不区分大小写）
     * 即使是误判（例如注释中包含，或者单词子串包含）也无所谓，只要不漏判即可，
     * 这样可以过滤掉绝大部分无关 SQL，大幅提升性能。
     */
    private boolean containsAnySensitiveField(String sql) {
        if (sensitiveFields == null || sensitiveFields.isEmpty() || sql == null) {
            return false;
        }
        // 转换为全小写进行匹配（假设 sensitiveFields 构造时已转为小写）
        String lowerSql = sql.toLowerCase();
        for (String field : sensitiveFields) {
            if (lowerSql.contains(field)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 脱敏的主入口，UPDATE, INSERT, SELECT, DELETE 语句的处理
     */
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
            if (select instanceof Values values) {
                List<Column> columns = insert.getColumns();
                for (int i = 0; i < columns.size(); i++) {
                    Column column = columns.get(i);
                    ExpressionList<? extends Expression> expressions = values.getExpressions();
                    if (expressions instanceof ParenthesedExpressionList) {
                        // 1.1 单行INSERT语句，INSERT ... VALUES (1, '2', '3')
                        maskFieldValueIfSensitive(column, expressions.get(i));
                        continue;
                    }
                    for (Expression expression : expressions) {
                        if (expression instanceof ExpressionList<? extends Expression> valueList) {
                            // 2.1 多行INSERT语句，INSERT ... VALUES (1, '2', '3'), (4, '5', '6')
                            maskFieldValueIfSensitive(column, valueList.get(i));
                        } else {
                            // 1.2 INSERT (column1) VALUES ('2')
                            // 2.2 INSERT (column1) VALUES ('2'), ('5')
                            maskFieldValueIfSensitive(column, expression);
                        }
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
         * @param updateSets updateSets
         */
        private void maskUpdateSets(List<UpdateSet> updateSets) {
            if (updateSets == null || updateSets.isEmpty()) {
                return;
            }
            updateSets.forEach(updateSet -> {
                ExpressionList<Column> columns = updateSet.getColumns();
                ExpressionList<? extends Expression> expressions = updateSet.getValues();
                if (expressions.get(0) instanceof ParenthesedSelect) {
                    // 1.1 基础UPDATE语句 UPDATE ... SET (a, b, c) = (VALUES '1', '2', '3')
                    if (((ParenthesedSelect) expressions.get(0)).getSelect() instanceof Values) {
                        expressions = ((Values) ((ParenthesedSelect) expressions.get(0)).getSelect()).getExpressions();
                    }
                }
                for (int i = 0; i < columns.size(); i++) {
                    Column column = columns.get(i);
                    Expression expression = expressions.get(i);
                    if (expression instanceof CaseExpression) {
                        // 3. UPDATE ... SET column1 = CASE ... END
                        maskCaseExpression(column, (CaseExpression) expression);
                    } else {
                        // 1.2 基础UPDATE语句 UPDATE ... SET column2 = (SELECT ... )
                        // 1.3 基础UPDATE语句 UPDATE ... SET column1 = value1 ...
                        maskExpressionIfSensitive(column, expression);
                    }
                }
            });
        }

        /**
         * 处理 CASE 表达式
         * @param column   column
         * @param caseExpr CaseExpression
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
                    maskExpressionIfSensitive(switchColumn, whenExpr);
                } else {
                    // 3.2 UPDATE ... SET column1 = CASE WHEN password='secret' THEN ... ELSE ...END
                    whenExpr.accept(SqlMasker.this.maskExpressionVisitor);
                }
                
                // 3.3 UPDATE ... SET column1 = CASE ... THEN (SELECT ... ) ELSE ...END    
                // 3.4 UPDATE ... SET column1 = CASE ... THEN 'secret' ELSE ...END
                maskExpressionIfSensitive(column, when.getThenExpression());
            }
            // 3.5 UPDATE ... SET column1 = CASE ... ELSE (SELECT ... ) END
            // 3.6 UPDATE ... SET column1 = CASE ... ELSE 'secret' END
            maskExpressionIfSensitive(column, caseExpr.getElseExpression());
        }

        /**
         * 处理 JOIN 和 WHERE 条件中的敏感字段
         * @param joinsProvider JoinsProvider
         * @param whereProvider WhereProvider
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

    /**
     * mask expression if it is a sensitive field
     * @param column     Column
     * @param expression Expression
     */
    private void maskExpressionIfSensitive(Column column, Expression expression) {
        if (expression instanceof Select) {
            ((Select) expression).accept(this.maskSelectVisitor);
        } else if (expression != null) {
            maskFieldValueIfSensitive(column, expression);
        }
    }

    /**
     * A visitor to traverse SELECT statements and mask sensitive field comparisons.
     */
    private class MaskSelectVisitor extends SelectVisitorAdapter {

        /**
         * 单一的 SELECT 查询，通常包含 SELECT 子句、FROM 子句、WHERE 子句、GROUP BY、HAVING、ORDER BY 等部分
         * @param plainSelect PlainSelect
         */
        @Override
        public void visit(PlainSelect plainSelect) {
            // 1. SELECT column1, (SELECT ... ) FROM ...
            plainSelect.getSelectItems().stream()
                .map(SelectItem::getExpression)
                .filter(selectItem -> selectItem instanceof ParenthesedSelect)
                .forEach(selectItem -> {
                    ((ParenthesedSelect) selectItem).accept(this);
                });
            // 2. SELECT * FROM (SELECT ... )
            if (plainSelect.getFromItem() instanceof ParenthesedSelect parenthesedSelect) {
                parenthesedSelect.getSelect().accept(this);
            }
            // 3. SELECT * FROM ... JOIN ...
            if (plainSelect.getJoins() != null) {
                plainSelect.getJoins().forEach(join -> {
                    // 3.1 SELECT * FROM ... JOIN (SELECT ... )
                    if (join.getRightItem() instanceof ParenthesedSelect) {
                        ((ParenthesedSelect) join.getRightItem()).getSelect().accept(this);
                    }
                    // 3.2 SELECT * FROM ... JOIN ... ON ...
                    join.getOnExpressions().forEach(expression -> {
                        expression.accept(SqlMasker.this.maskExpressionVisitor);
                    });
                });
            }
            // 0. SELECT * FROM ... WHERE ...
            if (plainSelect.getWhere() != null) {
                plainSelect.getWhere().accept(SqlMasker.this.maskExpressionVisitor);
            }
        }

        /**
         * 4. 被括号包裹的 SELECT 查询，例如作为子查询或在 UNION 操作中
         * @param parenthesedSelect ParenthesedSelect
         */
        @Override
        public void visit(ParenthesedSelect parenthesedSelect) {
            parenthesedSelect.getSelect().accept(this);
        }

        /**
         * 5. 多个 SELECT 查询通过集合操作（如 UNION、INTERSECT、EXCEPT 等）连接而成的组合查询
         * @param setOpList SetOperationList
         */
        @Override
        public void visit(SetOperationList setOpList) {
            setOpList.getSelects().forEach(select -> {
                select.accept(this);
            });
        }

        /**
         * 6. SQL 中 LATERAL 关键字引入的子查询，例如： <br/>
         *     SELECT * FROM t1, LATERAL (SELECT * FROM t2 WHERE t2.id = t1.id) AS t2 <br/>
         * @param subSelect LateralSubSelect
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

        @Override
        public void visit(AndExpression andExpression) {
            _visitBinaryExpression(andExpression);
        }

        @Override
        public void visit(OrExpression orExpression) {
            _visitBinaryExpression(orExpression);
        }
        
        /**
         * LIKE操作符: 首尾的 % _ 不被mask，其余部分mask，如 '%secret%' mask结果是 '%s****t%' (maskPattern="****")
         */
        @Override
        public void visit(LikeExpression likeExpression) {
            Expression left = likeExpression.getLeftExpression();
            Expression right = likeExpression.getRightExpression();
            Expression escape = likeExpression.getEscape();
            if (left instanceof Column && right instanceof StringValue) {
                if (escape instanceof StringValue) {
                    maskFieldValueIfSensitive((Column) left, right, ((StringValue) escape).getValue());
                } else {
                    maskFieldValueIfSensitive((Column) left, right, "");
                }
            }
        }

        @Override
        public void visit(Between between) {
            Expression left = between.getLeftExpression();
            Expression start = between.getBetweenExpressionStart();
            Expression end = between.getBetweenExpressionEnd();
            if (left instanceof Column) {
                maskExpressionIfSensitive((Column) left, start);
                maskExpressionIfSensitive((Column) left, end);
            }
        }

        @Override
        public void visit(InExpression inExpression) {
            Expression left = inExpression.getLeftExpression();
            Expression right = inExpression.getRightExpression();
            if (right instanceof ParenthesedSelect) {
                // IN (SELECT ... )
                ((ParenthesedSelect) right).getSelect().accept(SqlMasker.this.maskSelectVisitor);
            } else if (right instanceof ExpressionList) {
                // IN ('1', '2', '3')
                if (left instanceof Column) {
                    for (Expression inItem : ((ExpressionList<? extends Expression>) right)) {
                        maskFieldValueIfSensitive((Column) left, inItem);
                    }
                }
            }
        }

        @Override
        public void visit(Parenthesis parenthesis) {
            super.visit(parenthesis);
        }
    }

    /**
     * 如果左右两侧的表达式中任意一侧是敏感字段，则对敏感字段的值进行mask
     * @param left  left Expression
     * @param right right Expression
     */
    private void maskIfSensitive(Expression left, Expression right) {
        if (left instanceof Column) {
            maskExpressionIfSensitive((Column) left, right);
        } else if (right instanceof Column) {
            maskExpressionIfSensitive((Column) right, left);
        }
    }

    /**
     * 如果字段是敏感字段，则对字段值进行mask，否则不处理
     * @param column 字段
     * @param expr 表达式(非Like操作符的右侧表达式)
     */
    private void maskFieldValueIfSensitive(Column column, Expression expr) {
        maskFieldValueIfSensitive(column, expr, null);
    }

    /**
     * 判断字段名是否是敏感字段（不区分大小写，且支持部分匹配）
     * @param lowerCaseFieldName 小写的字段名
     * @return                   是否是敏感字段
     */
    private boolean isSensitiveField(String lowerCaseFieldName) {
        for (String field : sensitiveFields) {
            if (lowerCaseFieldName.contains(field)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 如果字段是敏感字段，则对字段值进行mask，否则不处理
     * @param column 字段
     * @param expr 表达式
     * @param escapeCharInLikeExpr 转义字符 <br/>
     *    - 如果为null，表示非like操作符 <br/>
     *    - 如果为""，表示like没有转义字符 <br/>
     *    - 如果为其他字符，表示like有转义字符，转义字符是该字符
     */
    private void maskFieldValueIfSensitive(Column column, Expression expr, String escapeCharInLikeExpr) {
        if (column == null || ! isSensitiveField(column.getColumnName().toLowerCase())) {
            return;
        }

        // 递归解包 Parenthesis
        // 针对 VALUES ('3') 被解析为 Parenthesis -> StringValue 的情况
        // while() 是因为 VALUES ((('3'))) 的递归情况
        while (expr instanceof Parenthesis) {
            expr = ((Parenthesis) expr).getExpression();
        }

        if (expr instanceof StringValue stringValue) {
            stringValue.setValue(escapeCharInLikeExpr != null
                    ? maskLikeValue(stringValue.getValue(), escapeCharInLikeExpr)
                    : maskSensitiveValue(stringValue.getValue()));
        }
    }

    /**
     * 保留首尾的通配符 % _ ，其余部分mask。 <br/>
     * 有转义字符的话，则被转义的% _连同转义字符本身都视为普通字符。
     * @param likeValue LIKE操作符的右侧的值
     * @param escapeStr 转义字符 null或""表示没有转义字符，其他字符表示转义字符
     * @return          脱敏后的值
     */
    String maskLikeValue(String likeValue, String escapeStr) {
        if (likeValue == null || likeValue.isEmpty()) {
            return likeValue;
        }
        
        // 如果没有转义字符，使用一个不可能出现在字符串中的字符作为转义字符
        char escapeChar = escapeStr == null || escapeStr.isEmpty() ? '\0' : escapeStr.charAt(0);
        
        int len = likeValue.length();
        
        // 1. 保留前缀的 % 和 _，Find prefix wildcards (start index)
        int start = 0;
        while (start < len) {
            char c = likeValue.charAt(start);
            if (c == escapeChar && start + 1 < len) {  // 先判断，以防ESCAPE=%或_（但这不是最佳实践）
                break; // Escaped char found, so prefix wildcards end
            }
            if (c == '%' || c == '_') {
                start++;
            } else {
                break; // Literal found
            }
        }
        
        // 2. 保留后缀的 % 和 _，Find suffix wildcards (end index)
        // We need to scan from 'start' to determine escaping correctly
        int end = len;
        int lastLiteralEnd = start;
        int i = start;
        while (i < len) {
            char c = likeValue.charAt(i);
            if (c == escapeChar && i + 1 < len) {  // Only treat as escape if followed by another char
                // This char and the next are literals (escaped)
                i++; // Skip escape char
                if (i < len) {
                    i++; // Skip escaped char
                }
                lastLiteralEnd = i;
            } else if (c == '%' || c == '_') {
                // Wildcard
                i++;
                // Do not update lastLiteralEnd
            } else {
                // Literal
                i++;
                lastLiteralEnd = i;
            }
        }
        end = lastLiteralEnd;

        // 中间部分进行mask
        String maskedValue = start < end ? maskSensitiveValue(likeValue.substring(start, end)) : "";

        // 拼接前缀、mask后的中间部分、后缀
        return likeValue.substring(0, start) + maskedValue + likeValue.substring(end);
    }

    /**
     * 对(敏感)字段值进行mask
     * @param field 字段值
     * @return mask后的值
     */
    protected String maskSensitiveValue(String field) {
        return doValueMask(field, this.maskPattern);
    }

    /**
     * 对(敏感)字段值进行mask <br/>
     *    mask规则：<br/>
     *      1. 如果value长度 <= maskPattern长度，则将value全部mask <br/>
     *         比如：'' => '****', 'abc' => '****' <br/>
     *      2. 如果value长度介于maskPattern长度的1-3倍之间，则将value中间部分mask，多余的字符数平均到首尾保留下来，不能平均时，将多余字符数保留到首位 <br/>
     *         比如：'1234567' => '12****7', '1234567890' => '123****890' <br/>
     *      3. 如果value长度 > maskPattern长度的3倍，结尾保留maskPattern长度，中间部分用 maskPattern 循环填充保证替换完后的字段长度跟原来一致 <br/>
     *         比如：'1234567890123456' => '1234********3456' <br/>
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
        sb.append(maskPattern.repeat(Math.max(0, midLen / pLen)));
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
