package io.hqwu.commons.cp.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * LogUtil SQL掩码测试
 * @author taige (Wu, Hongqiang)
 */
@DisplayName("LogUtil SQL掩码测试")
class SqlMaskerTest {

    private Set<String> sensitiveFields;

    @BeforeEach
    void setUp() {
        sensitiveFields = new HashSet<>();
        sensitiveFields.add("password");
        sensitiveFields.add("mobile");
        sensitiveFields.add("id_card");
        sensitiveFields.add("MOBILE"); // 测试大写字段名
        sensitiveFields.add("Password"); // 测试首字母大写字段名
    }

    // 掩码模式数据
    private static Stream<String> provideMaskPatterns() {
        return Stream.of("****", "###", "?", "●●●●", "XXXX");
    }

    // INSERT语句测试数据
    private static Stream<Arguments> provideInsertTestCases() {
        return Stream.of(
                // 基本INSERT
                Arguments.of(
                        "基本INSERT语句",
                        "InSeRt InTo users (name, password) VALUES ('张三', '123456789012')",
                        (MaskFunction) (pattern) -> String.format("InSeRt InTo users (name, password) VALUES ('张三', '1234%s9012')", pattern)
                ),
                // 带换行和空格
                Arguments.of(
                        "带换行和空格的INSERT语句",
                        "INSERT \n  INTO \n  users \n  (\n    name,\n    password,\n    mobile\n  )\n  vAlUeS\n  (\n    '张三',\n    '123456789012',\n    '13812345678'\n  )",
                        (MaskFunction) (pattern) -> String.format("INSERT \n  INTO \n  users \n  (\n    name,\n    password,\n    mobile\n  )\n  vAlUeS\n  (\n    '张三',\n    '1234%s9012',\n    '1381%s5678'\n  )", pattern, pattern)
                ),
                // 带注释
                Arguments.of(
                        "带注释的INSERT语句",
                        "InSeRt InTo users -- 插入用户信息\n(name, /* 用户姓名 */ password /*密码*/, mobile) VaLuEs ('张三', '123456789012', '13812345678')",
                        (MaskFunction) (pattern) -> String.format("InSeRt InTo users -- 插入用户信息\n(name, /* 用户姓名 */ password /*密码*/, mobile) VaLuEs ('张三', '1234%s9012', '1381%s5678')", pattern, pattern)
                ),
                // 使用反引号
                Arguments.of(
                        "使用反引号的INSERT语句",
                        "InSeRt InTo users (`name`, `password`, `mobile`) VALUES ('张三', '123456789012', '13812345678')",
                        (MaskFunction) (pattern) -> String.format("InSeRt InTo users (`name`, `password`, `mobile`) VALUES ('张三', '1234%s9012', '1381%s5678')", pattern, pattern)
                )
        );
    }

    // UPDATE语句测试数据
    private static Stream<Arguments> provideUpdateTestCases() {
        return Stream.of(
                // 基本UPDATE
                Arguments.of(
                        "基本UPDATE语句",
                        "UpDaTe Users SeT PASSWORD='123456789012', Mobile='13812345678' WhErE ID_CARD='330102199001011234'",
                        (MaskFunction) (pattern) -> String.format("UpDaTe Users SeT PASSWORD='1234%s9012', Mobile='1381%s5678' WhErE ID_CARD='3301%s1234'", pattern, pattern, pattern)
                ),
                // 带子查询的UPDATE
                Arguments.of(
                        "带子查询的UPDATE语句",
                        "UpDaTe users SET password='123456789012' wHeRe id_card=(SeLeCt id_card FrOm temp_users WhErE mobile='13812345678')",
                        (MaskFunction) (pattern) -> String.format("UpDaTe users SET password='1234%s9012' wHeRe id_card=(SeLeCt id_card FrOm temp_users WhErE mobile='1381%s5678')", pattern, pattern)
                ),
                // 带IN条件的UPDATE
                Arguments.of(
                        "带IN条件的UPDATE语句",
                        "UpDaTe users SeT password='123456789012' WhErE mobile In ('13812345678', '13912345678')",
                        (MaskFunction) (pattern) -> String.format("UpDaTe users SeT password='1234%s9012' WhErE mobile In ('1381%s5678', '1391%s5678')", pattern, pattern, pattern)
                ),
                // 带LIKE条件的UPDATE
                Arguments.of(
                        "带LIKE条件的UPDATE语句",
                        "UpDaTe users SeT mobile='13812345678' WhErE password LiKe '123456%'",
                        (MaskFunction) (pattern) -> String.format("UpDaTe users SeT mobile='1381%s5678' WhErE password LiKe '123456%%'", pattern)
                ),
                // 带AND/OR条件的复杂WHERE子句
                Arguments.of(
                        "带AND/OR条件的复杂WHERE子句",
                        "UpDaTe users SeT name='李四' WhErE (mobile='13812345678' Or mobile='13912345678') AnD (password='123456789012' Or id_card='330102199001011234')",
                        (MaskFunction) (pattern) -> String.format("UpDaTe users SeT name='李四' WhErE (mobile='1381%s5678' Or mobile='1391%s5678') AnD (password='1234%s9012' Or id_card='3301%s1234')", pattern, pattern, pattern, pattern)
                )
        );
    }

    // 定义一个函数式接口来处理掩码模式
    @FunctionalInterface
    interface MaskFunction {
        String apply(String maskPattern);
    }

    @Nested
    @DisplayName("1. INSERT语句测试")
    class InsertStatementTests {

        @ParameterizedTest(name = "1.1 {0} - 使用掩码模式 {1}")
        @MethodSource("io.hqwu.commons.cp.util.SqlMaskerTest#provideInsertTestData")
        void testInsert(String testName, String sql, MaskFunction expectedGenerator, String maskPattern) {
            String expected = expectedGenerator.apply(maskPattern);
            assertEquals(expected, SqlMasker.maskSensitiveFields(sql, sensitiveFields, maskPattern));
        }
    }

    // INSERT语句参数化测试数据
    private static Stream<Arguments> provideInsertTestData() {
        return provideInsertTestCases().flatMap(args ->
                provideMaskPatterns().map(pattern ->
                        Arguments.of(
                                args.get()[0],
                                args.get()[1],
                                args.get()[2],
                                pattern
                        )
                )
        );
    }

    @Nested
    @DisplayName("2. UPDATE语句测试")
    class UpdateStatementTests {

        @ParameterizedTest(name = "2.1 {0} - 使用掩码模式 {1}")
        @MethodSource("io.hqwu.commons.cp.util.SqlMaskerTest#provideUpdateTestData")
        void testUpdate(String testName, String sql, MaskFunction expectedGenerator, String maskPattern) {
            String expected = expectedGenerator.apply(maskPattern);
            assertEquals(expected, SqlMasker.maskSensitiveFields(sql, sensitiveFields, maskPattern));
        }
    }

    // UPDATE语句参数化测试数据
    private static Stream<Arguments> provideUpdateTestData() {
        return provideUpdateTestCases().flatMap(args ->
                provideMaskPatterns().map(pattern ->
                        Arguments.of(
                                args.get()[0],
                                args.get()[1],
                                args.get()[2],
                                pattern
                        )
                )
        );
    }

    // 边界情况测试数据
    private static Stream<Arguments> provideEdgeCases() {
        return Stream.of(
                // 空值处理
                Arguments.of(
                        "空SQL",
                        null,
                        null
                ),
                Arguments.of(
                        "空敏感字段集合",
                        "SELECT * FROM users",
                        "SELECT * FROM users"
                ),
                // 短字符串
                Arguments.of(
                        "短密码",
                        "INSERT INTO users (password) VALUES ('123')",
                        "INSERT INTO users (password) VALUES ('123')"
                ),
                // 超长字符串
                Arguments.of(
                        "超长密码",
                        "INSERT INTO users (password) VALUES ('123456789012345678901234567890')",
                        "INSERT INTO users (password) VALUES ('1234****7890')"
                ),
                // 特殊字符
                Arguments.of(
                        "包含转义字符",
                        "INSERT INTO users (password) VALUES ('123\\'456789012')",
                        "INSERT INTO users (password) VALUES ('123\\'4****9012')"
                ),
                Arguments.of(
                        "包含Unicode字符",
                        "INSERT INTO users (password) VALUES ('123456\\u0020789012')",
                        "INSERT INTO users (password) VALUES ('1234****9012')"
                )
        );
    }

    @Nested
    @DisplayName("3. 参数化测试")
    class ParameterizedTests {

        @ParameterizedTest(name = "3.1 使用掩码模式 {0}")
        @MethodSource("io.hqwu.commons.cp.util.SqlMaskerTest#provideMaskPatterns")
        @DisplayName("不同掩码模式测试")
        void testDifferentMaskPatterns(String maskPattern) {
            String sql = "INSERT INTO users (name, password) VALUES ('张三', '123456789012')";
            String expected = String.format("INSERT INTO users (name, password) VALUES ('张三', '1234%s9012')", maskPattern);
            assertEquals(expected, SqlMasker.maskSensitiveFields(sql, sensitiveFields, maskPattern));
        }

        @ParameterizedTest(name = "3.2 边界情况: {0}")
        @MethodSource("io.hqwu.commons.cp.util.SqlMaskerTest#provideEdgeCases")
        @DisplayName("边界情况测试")
        void testEdgeCases(String testName, String sql, String expected) {
            assertEquals(expected, SqlMasker.maskSensitiveFields(sql, sensitiveFields, "****"));
        }
    }

    @Nested
    @DisplayName("4. 特殊情况测试")
    class SpecialCaseTests {

        @Test
        @DisplayName("4.1 空值处理")
        void testNullHandling() {
            assertEquals(null, SqlMasker.maskSensitiveFields(null, sensitiveFields, "****"));
            assertEquals("SELECT * FROM users", SqlMasker.maskSensitiveFields("SELECT * FROM users", null, "****"));
            assertEquals("SELECT * FROM users", SqlMasker.maskSensitiveFields("SELECT * FROM users", new HashSet<>(), "****"));
        }

        @Test
        @DisplayName("4.2 转义字符处理")
        void testEscapeCharacters() {
            String sql = "INSERT INTO users (name, password) VALUES ('O\\'Brien', '123\\'456789012')";
            String expected = "INSERT INTO users (name, password) VALUES ('O\\'Brien', '123\\'4****9012')";
            assertEquals(expected, SqlMasker.maskSensitiveFields(sql, sensitiveFields, "****"));
        }

        @Test
        @DisplayName("4.3 多个连续转义字符")
        void testMultipleEscapeCharacters() {
            String sql = "INSERT INTO users (name, password) VALUES ('O\\\\'Brien', '123\\\\456789012')";
            String expected = "INSERT INTO users (name, password) VALUES ('O\\\\'Brien', '123\\\\****9012')";
            assertEquals(expected, SqlMasker.maskSensitiveFields(sql, sensitiveFields, "****"));
        }

        @Test
        @DisplayName("4.4 Unicode字符")
        void testUnicodeCharacters() {
            String sql = "INSERT INTO users (name, password) VALUES ('张三', '123456\\u0020789012')";
            String expected = "INSERT INTO users (name, password) VALUES ('张三', '1234****9012')";
            assertEquals(expected, SqlMasker.maskSensitiveFields(sql, sensitiveFields, "****"));
        }

        @Test
        @DisplayName("4.5 超长敏感信息")
        void testLongSensitiveInfo() {
            String sql = "INSERT INTO users (password) VALUES ('123456789012345678901234567890')";
            String expected = "INSERT INTO users (password) VALUES ('1234****7890')";
            assertEquals(expected, SqlMasker.maskSensitiveFields(sql, sensitiveFields, "****"));
        }

        @Test
        @DisplayName("4.6 值包含SQL关键字")
        void testValueContainsSqlKeywords() {
            String sql = "INSERT INTO users (password) VALUES ('SELECT123456FROM789012')";
            String expected = "INSERT INTO users (password) VALUES ('1234****9012')";
            assertEquals(expected, SqlMasker.maskSensitiveFields(sql, sensitiveFields, "****"));
        }

        @Test
        @DisplayName("4.7 字段名包含敏感字段")
        void testFieldNameContainsSensitiveField() {
            String sql = "UPDATE users SET user_password='123456789012', mobile_phone='13812345678'";
            String expected = "UPDATE users SET user_password='1234****9012', mobile_phone='1381****5678'";
            assertEquals(expected, SqlMasker.maskSensitiveFields(sql, sensitiveFields, "****"));
        }
    }

    @Nested
    @DisplayName("5. DELETE语句测试")
    class DeleteStatementTests {

        @Test
        @DisplayName("5.1 基本的DELETE语句")
        void testBasicDelete() {
            String sql = "DeLeTe FrOm users WhErE mobile='13812345678' AnD password='123456789012'";
            String expected = "DeLeTe FrOm users WhErE mobile='1381****5678' AnD password='1234****9012'";
            assertEquals(expected, SqlMasker.maskSensitiveFields(sql, sensitiveFields, "****"));
        }

        @Test
        @DisplayName("5.2 带有IN和OR条件的DELETE语句")
        void testDeleteWithInAndOr() {
            String sql = "DELETE FROM users WHERE mobile IN ('13812345678', '13912345678') OR password='123456789012' OR id_card='330102199001011234'";
            String expected = "DELETE FROM users WHERE mobile IN ('1381****5678', '1391****5678') OR password='1234****9012' OR id_card='3301****1234'";
            assertEquals(expected, SqlMasker.maskSensitiveFields(sql, sensitiveFields, "****"));
        }

        @Test
        @DisplayName("5.3 带有子查询的DELETE语句")
        void testDeleteWithSubquery() {
            String sql = "DELETE FROM users WHERE id IN (SELECT user_id FROM orders WHERE mobile='13812345678' AND password='123456789012')";
            String expected = "DELETE FROM users WHERE id IN (SELECT user_id FROM orders WHERE mobile='1381****5678' AND password='1234****9012')";
            assertEquals(expected, SqlMasker.maskSensitiveFields(sql, sensitiveFields, "****"));
        }
    }

    @Nested
    @DisplayName("6. SELECT语句测试")
    class SelectStatementTests {

        @Test
        @DisplayName("6.1 带有ORDER BY和LIMIT的SELECT语句")
        void testSelectWithOrderByAndLimit() {
            String sql = "SeLeCt * FrOm users WhErE mobile='13812345678' AnD password='123456789012' OrDeR By id LiMiT 1";
            String expected = "SeLeCt * FrOm users WhErE mobile='1381****5678' AnD password='1234****9012' OrDeR By id LiMiT 1";
            assertEquals(expected, SqlMasker.maskSensitiveFields(sql, sensitiveFields, "****"));
        }

        @Test
        @DisplayName("6.2 带有JOIN的SELECT语句")
        void testSelectWithJoin() {
            String sql = "SeLeCt u.*, o.* FrOm users u LeFt JoIn orders o On u.id=o.user_id WhErE u.mobile='13812345678' AnD u.password='123456789012'";
            String expected = "SeLeCt u.*, o.* FrOm users u LeFt JoIn orders o On u.id=o.user_id WhErE u.mobile='1381****5678' AnD u.password='1234****9012'";
            assertEquals(expected, SqlMasker.maskSensitiveFields(sql, sensitiveFields, "****"));
        }

        @Test
        @DisplayName("6.3 UNION查询")
        void testUnionSelect() {
            String sql = "SELECT id_card FROM users WHERE mobile='13812345678' UNION SELECT id_card FROM temp_users WHERE mobile='13912345678'";
            String expected = "SELECT id_card FROM users WHERE mobile='1381****5678' UNION SELECT id_card FROM temp_users WHERE mobile='1391****5678'";
            assertEquals(expected, SqlMasker.maskSensitiveFields(sql, sensitiveFields, "****"));
        }

        @Test
        @DisplayName("6.4 多层嵌套子查询")
        void testNestedSubqueries() {
            String sql = "SELECT * FROM users WHERE mobile IN (SELECT mobile FROM temp_users WHERE password IN (SELECT password FROM history WHERE id_card='330102199001011234'))";
            String expected = "SELECT * FROM users WHERE mobile IN (SELECT mobile FROM temp_users WHERE password IN (SELECT password FROM history WHERE id_card='3301****1234'))";
            assertEquals(expected, SqlMasker.maskSensitiveFields(sql, sensitiveFields, "****"));
        }
    }

    @Nested
    @DisplayName("7. 复杂SQL场景测试")
    class ComplexSqlTests {

        @Test
        @DisplayName("7.1 WITH子句和ON DUPLICATE KEY UPDATE")
        void testWithClauseAndDuplicateKeyUpdate() {
            String sql = "WITH t AS (SELECT password FROM users WHERE mobile='13812345678') " +
                    "INSERT INTO `users` (\"name\", `password`) " +
                    "VALUES (\"O'Brien\", (SELECT password FROM t)) " +
                    "ON DUPLICATE KEY UPDATE password='123456789012'";
            String expected = "WITH t AS (SELECT password FROM users WHERE mobile='1381****5678') " +
                    "INSERT INTO `users` (\"name\", `password`) " +
                    "VALUES (\"O'Brien\", (SELECT password FROM t)) " +
                    "ON DUPLICATE KEY UPDATE password='1234****9012'";
            assertEquals(expected, SqlMasker.maskSensitiveFields(sql, sensitiveFields, "****"));
        }

        @Test
        @DisplayName("7.2 批量INSERT")
        void testBatchInsert() {
            String sql = "INSERT INTO users (name, password, mobile) VALUES " +
                    "('张三', '123456789012', '13812345678'), " +
                    "('李四', '123456789012', '13912345678')";
            String expected = "INSERT INTO users (name, password, mobile) VALUES " +
                    "('张三', '1234****9012', '1381****5678'), " +
                    "('李四', '1234****9012', '1391****5678')";
            assertEquals(expected, SqlMasker.maskSensitiveFields(sql, sensitiveFields, "****"));
        }

        @Test
        @DisplayName("7.3 带表别名前缀的UPDATE")
        void testUpdateWithTableAlias() {
            String sql = "UPDATE t1 SET t1.password='123456789012', t2.mobile='13812345678' FROM users t1 JOIN temp_users t2";
            String expected = "UPDATE t1 SET t1.password='1234****9012', t2.mobile='1381****5678' FROM users t1 JOIN temp_users t2";
            assertEquals(expected, SqlMasker.maskSensitiveFields(sql, sensitiveFields, "****"));
        }
    }

    @Test
    void testMaskField() {
        // 1. 空值和边界情况
        assertNull(SqlMasker.maskField(null, "****"));
        assertEquals("123", SqlMasker.maskField("123", null));
        assertEquals("123", SqlMasker.maskField("123", ""));
        assertEquals("####", SqlMasker.maskField("", "####"));

        // 2. 规则1：字段长度 <= 掩码长度
        assertEquals("*", SqlMasker.maskField("1", "*"));
        assertEquals("##", SqlMasker.maskField("1", "##"));
        assertEquals("##", SqlMasker.maskField("12", "##"));
        assertEquals("###", SqlMasker.maskField("12", "###"));
        assertEquals("###", SqlMasker.maskField("123", "###"));
        assertEquals("****", SqlMasker.maskField("123", "****"));
        assertEquals("****", SqlMasker.maskField("1234", "****"));
        assertEquals("●●●●●", SqlMasker.maskField("1234", "●●●●●"));
        assertEquals("●●●●●", SqlMasker.maskField("12345", "●●●●●"));
        assertEquals("??????", SqlMasker.maskField("12345", "??????"));
        assertEquals("??????", SqlMasker.maskField("123456", "??????"));

        // 3. 规则2：字段长度在掩码长度1-3倍之间, i.e. 1x < fieldLength <= 3x
        // 3.1 偶数长度掩码模式（4个字符）的情况
        assertEquals("1****", SqlMasker.maskField("12345", "****"));               // 1x+1，首1尾0
        assertEquals("1****6", SqlMasker.maskField("123456", "****"));             // 1.5倍，剩余2个字符平均分配
        assertEquals("12####78", SqlMasker.maskField("12345678", "####"));         // 2倍，首2尾2
        assertEquals("123####89", SqlMasker.maskField("123456789", "####"));       // 2.25倍，剩余5个字符，首3末2
        assertEquals("1234????901", SqlMasker.maskField("12345678901", "????"));   // 2.75倍，剩余7个字符，首4末3
        assertEquals("1234????9012", SqlMasker.maskField("123456789012", "????")); // 3倍，首尾各4个字符

        // 3.2 奇数长度掩码模式（3个字符）的情况
        assertEquals("1***", SqlMasker.maskField("1234", "***"));               // 1x+1，首1尾0
        assertEquals("1***5", SqlMasker.maskField("12345", "***"));             // 1.67倍，剩余2个字符平均分配
        assertEquals("12###6", SqlMasker.maskField("123456", "###"));           // 2倍，首2末1
        assertEquals("12###67", SqlMasker.maskField("1234567", "###"));         // 2.33倍，剩余4个字符，首2末2
        assertEquals("123●●●78", SqlMasker.maskField("12345678", "●●●"));       // 2.67倍，剩余5个字符，首3末2
        assertEquals("123●●●789", SqlMasker.maskField("123456789", "●●●"));     // 3倍，首尾各3个字符

        // 4. 规则3：字段长度大于掩码长度3倍
        // 4.1 偶数长度掩码模式
        assertEquals("1234*?#●*0123", SqlMasker.maskField("1234567890123", "*?#●"));     // 3x+1，首4尾4，中间5个mask字符
        assertEquals("1234?●#*?●#2345", SqlMasker.maskField("123456789012345", "?●#*")); // 3x+3，首尾各4个字符，中间7个mask字符
        assertEquals("1234●?*#●?*#●4567", SqlMasker.maskField("12345678901234567", "●?*#")); // 4x+1，首尾各4个字符，中间9个mask字符

        // 4.2 奇数长度掩码模式
        assertEquals("123#?*#890", SqlMasker.maskField("1234567890", "#?*"));     // 3x+1，首尾各3个字符，中间4个mask字符
        assertEquals("12345●#?*#●#34567", SqlMasker.maskField("12345678901234567", "●#?*#")); // 3x+2，首尾各5个字符，中间7个mask字符
        assertEquals("123#?*#?*#123", SqlMasker.maskField("1234567890123", "#?*"));     // 4x+1，首尾各3个字符，中间7个mask字符

        // 5. 特殊字符处理
        assertEquals("1 2 XXXX5 6", SqlMasker.maskField("1 2 3 4 5 6", "XXXX"));
        assertEquals("1\t2?????5\t6", SqlMasker.maskField("1\t2\t3\t4\t5\t6", "???"));
        assertEquals("    ", SqlMasker.maskField("123", "    "));
        assertEquals("    ", SqlMasker.maskField("1234", "    "));

        // 6. Unicode字符处理
        assertEquals("1####", SqlMasker.maskField("1你好世界", "####"));
        assertEquals("1○○○界", SqlMasker.maskField("1你好世界", "○○○"));
    }
    
}
