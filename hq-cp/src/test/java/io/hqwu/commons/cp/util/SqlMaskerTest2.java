package io.hqwu.commons.cp.util;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;

/**
 * SQL脱敏测试类
 *
 * @author taige
 */
@DisplayName("SQL脱敏测试")
public class SqlMaskerTest2 {
    private static final Logger log = LoggerFactory.getLogger(SqlMaskerTest.class);
    private static final String MASK_PATTERN = "****";
    private static MockedStatic<SqlMasker> sqlMaskerMock;

    @BeforeAll
    static void setup() {
        sqlMaskerMock = Mockito.mockStatic(SqlMasker.class, Mockito.CALLS_REAL_METHODS);
        sqlMaskerMock.when(() -> SqlMasker.doValueMask(any(), any())).thenReturn(MASK_PATTERN);
    }

    @AfterAll
    static void tearDown() {
        sqlMaskerMock.close();
    }

    private static Set<String> setOf(String... values) {
        Set<String> set = new HashSet<>();
        Collections.addAll(set, values);
        return set;
    }

    private static Stream<Arguments> insertSqlProvider() {
        return Stream.of(
            // 单行INSERT
            Arguments.of(
                "INSERT INTO users (id, name, password, email) VALUES (1, 'John', 'secret123', 'john@example.com')",
                "INSERT INTO users (id, name, password, email) VALUES (1, 'John', '****', 'john@example.com')",
                setOf("password"),
                "单行INSERT - 基本场景"
            ),
            // 多行INSERT
            Arguments.of(
                "INSERT INTO users (id, name, password, email) VALUES (1, 'John', 'secret123', 'john@example.com'), (2, 'Mary', 'secret456', 'mary@example.com')",
                "INSERT INTO users (id, name, password, email) VALUES (1, 'John', '****', 'john@example.com'), (2, 'Mary', '****', 'mary@example.com')",
                setOf("password"),
                "多行INSERT - 多值场景"
            ),
            // INSERT ... SELECT
            Arguments.of(
                "INSERT INTO backup_users (id, name, password, email) SELECT id, name, password, email FROM users WHERE password = 'simple_password'",
                "INSERT INTO backup_users (id, name, password, email) SELECT id, name, password, email FROM users WHERE password = '****'",
                setOf("password"),
                "INSERT SELECT - 子查询场景"
            ),
            // INSERT ... ON DUPLICATE KEY UPDATE
            Arguments.of(
                "INSERT INTO users (id, password) VALUES (123, 'secret') ON DUPLICATE KEY UPDATE password = 'new_secret'",
                "INSERT INTO users (id, password) VALUES (123, '****') ON DUPLICATE KEY UPDATE password = '****'",
                setOf("password"),
                "INSERT DUPLICATE - MySQL特有语法"
            )
        );
    }

    private static Stream<Arguments> updateSqlProvider() {
        return Stream.of(
            // 基础UPDATE
            Arguments.of(
                "UPDATE users SET enabled = true, password = 'new_password' WHERE id = 1",
                "UPDATE users SET enabled = true, password = '****' WHERE id = 1",
                setOf("password"),
                "基础UPDATE - 简单场景"
            ),
            // WHERE中包含敏感字段
            Arguments.of(
                "UPDATE users SET enabled = false WHERE password = 'simple_password'",
                "UPDATE users SET enabled = false WHERE password = '****'",
                setOf("password"),
                "UPDATE条件 - WHERE中的敏感字段"
            ),
            // 带JOIN的UPDATE
            Arguments.of(
                "UPDATE users u JOIN temp_users t ON u.id = t.id SET u.password = 'new_pass', t.sync = true WHERE u.password = 'old_pass'",
                "UPDATE users u JOIN temp_users t ON u.id = t.id SET u.password = '****', t.sync = true WHERE u.password = '****'",
                setOf("password"),
                "UPDATE JOIN - 多表更新"
            ),
            // CASE表达式
            Arguments.of(
                "UPDATE users SET password = CASE WHEN id < 100 THEN 'pass1' WHEN id < 200 THEN 'pass2' ELSE 'pass3' END",
                "UPDATE users SET password = CASE WHEN id < 100 THEN '****' WHEN id < 200 THEN '****' ELSE '****' END",
                setOf("password"),
                "UPDATE CASE - 条件更新"
            )
        );
    }

    private static Stream<Arguments> selectSqlProvider() {
        return Stream.of(
            // 基础SELECT
            Arguments.of(
                "SELECT id, name FROM users WHERE password = 'secret123'",
                "SELECT id, name FROM users WHERE password = '****'",
                setOf("password"),
                "基础SELECT - 简单查询"
            ),
            // JOIN查询
            Arguments.of(
                "SELECT t1.name, t2.email FROM users t1 JOIN info t2 ON t1.id = t2.user_id WHERE t1.password = 'secret' OR t2.phone = '12345'",
                "SELECT t1.name, t2.email FROM users t1 JOIN info t2 ON t1.id = t2.user_id WHERE t1.password = '****' OR t2.phone = '****'",
                setOf("password", "phone"),
                "SELECT JOIN - 多表查询"
            ),
            // UNION查询
            Arguments.of(
                "SELECT id FROM users WHERE password = 'pass1' UNION ALL SELECT id FROM temp_users WHERE password = 'pass2'",
                "SELECT id FROM users WHERE password = '****' UNION ALL SELECT id FROM temp_users WHERE password = '****'",
                setOf("password"),
                "SELECT UNION - 联合查询"
            ),
            // 带Schema的查询
            Arguments.of(
                "SELECT * FROM db1.users WHERE db1.users.password = 'secret'",
                "SELECT * FROM db1.users WHERE db1.users.password = '****'",
                setOf("password"),
                "SELECT SCHEMA - 带数据库前缀"
            )
        );
    }

    private static Stream<Arguments> deleteSqlProvider() {
        return Stream.of(
            // 基础DELETE
            Arguments.of(
                "DELETE FROM users WHERE password = 'secret123'",
                "DELETE FROM users WHERE password = '****'",
                setOf("password"),
                "基础DELETE - 简单删除"
            ),
            // 多表DELETE
            Arguments.of(
                "DELETE u, h FROM users u JOIN history h ON u.id = h.user_id WHERE u.password = 'secret' OR h.backup_pass = 'old'",
                "DELETE u, h FROM users u JOIN history h ON u.id = h.user_id WHERE u.password = '****' OR h.backup_pass = '****'",
                setOf("password", "backup_pass"),
                "DELETE JOIN - 多表删除"
            )
        );
    }

    private static Stream<Arguments> whereConditionProvider() {
        return Stream.of(
            // 比较操作符
            Arguments.of(
                "SELECT * FROM users WHERE password = 'secret' AND phone != '12345' AND salary > '1000' AND age >= '18'",
                "SELECT * FROM users WHERE password = '****' AND phone != '****' AND salary > '****' AND age >= '****'",
                setOf("password", "phone", "salary", "age"),
                "WHERE比较 - 各种比较操作符"
            ),
            // 空值判断
            Arguments.of(
                "SELECT * FROM users WHERE password IS NULL OR phone IS NOT NULL",
                "SELECT * FROM users WHERE password IS NULL OR phone IS NOT NULL",
                setOf("password", "phone"),
                "WHERE空值 - IS NULL判断"
            ),
            // LIKE操作符
            Arguments.of(
                "SELECT * FROM users WHERE password LIKE '%secret%' AND phone LIKE '138%'",
                "SELECT * FROM users WHERE password LIKE '%****%' AND phone LIKE '****%'",
                setOf("password", "phone"),
                "WHERE LIKE - 模糊匹配"
            ),
            // 范围操作符
            Arguments.of(
                "SELECT * FROM users WHERE salary BETWEEN '1000' AND '2000' AND phone IN ('123', '456', '789')",
                "SELECT * FROM users WHERE salary BETWEEN '****' AND '****' AND phone IN ('****', '****', '****')",
                setOf("salary", "phone"),
                "WHERE范围 - BETWEEN和IN"
            ),
            // 子查询
            Arguments.of(
                "SELECT * FROM users WHERE salary > (SELECT AVG(salary) FROM employees WHERE password = 'simple_password')",
                "SELECT * FROM users WHERE salary > (SELECT AVG(salary) FROM employees WHERE password = '****')",
                setOf("password", "salary"),
                "WHERE子查询 - 嵌套查询"
            ),
            // EXISTS
            Arguments.of(
                "SELECT * FROM users u WHERE EXISTS (SELECT 1 FROM audit_log a WHERE a.user_id = u.id AND a.password = 'secret')",
                "SELECT * FROM users u WHERE EXISTS (SELECT 1 FROM audit_log a WHERE a.user_id = u.id AND a.password = '****')",
                setOf("password"),
                "WHERE EXISTS - 存在性判断"
            )
        );
    }

    @ParameterizedTest(name = "{3}")
    @MethodSource("insertSqlProvider")
    void testMaskInsert(String sql, String expected, Set<String> sensitiveFields, String testName) {
        log.info("测试用例: {}\n输入SQL: {}\n敏感字段: {}", testName, sql, sensitiveFields);
        String result = SqlMasker.maskSensitiveFields(sql, sensitiveFields, MASK_PATTERN);
        log.info("\n脱敏结果: {}", result);
        assertEquals(expected, result);
    }

    @ParameterizedTest(name = "{3}")
    @MethodSource("updateSqlProvider")
    void testMaskUpdate(String sql, String expected, Set<String> sensitiveFields, String testName) {
        log.info("测试用例: {}\n输入SQL: {}\n敏感字段: {}", testName, sql, sensitiveFields);
        String result = SqlMasker.maskSensitiveFields(sql, sensitiveFields, MASK_PATTERN);
        log.info("\n脱敏结果: {}", result);
        assertEquals(expected, result);
    }

    @ParameterizedTest(name = "{3}")
    @MethodSource("selectSqlProvider")
    void testMaskSelect(String sql, String expected, Set<String> sensitiveFields, String testName) {
        log.info("测试用例: {}\n输入SQL: {}\n敏感字段: {}", testName, sql, sensitiveFields);
        String result = SqlMasker.maskSensitiveFields(sql, sensitiveFields, MASK_PATTERN);
        log.info("\n脱敏结果: {}", result);
        assertEquals(expected, result);
    }

    @ParameterizedTest(name = "{3}")
    @MethodSource("deleteSqlProvider")
    void testMaskDelete(String sql, String expected, Set<String> sensitiveFields, String testName) {
        log.info("测试用例: {}\n输入SQL: {}\n敏感字段: {}", testName, sql, sensitiveFields);
        String result = SqlMasker.maskSensitiveFields(sql, sensitiveFields, MASK_PATTERN);
        log.info("\n脱敏结果: {}", result);
        assertEquals(expected, result);
    }

    @ParameterizedTest(name = "{3}")
    @MethodSource("whereConditionProvider")
    void testMaskWhereConditions(String sql, String expected, Set<String> sensitiveFields, String testName) {
        log.info("测试用例: {}\n输入SQL: {}\n敏感字段: {}", testName, sql, sensitiveFields);
        String result = SqlMasker.maskSensitiveFields(sql, sensitiveFields, MASK_PATTERN);
        log.info("\n脱敏结果: {}", result);
        assertEquals(expected, result);
    }

}
