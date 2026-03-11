package io.hqwu.commons.mybatisplus;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import lombok.Data;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CommonFieldsFillerTest {

    private CommonFieldsFiller commonFieldsFiller;

    @Data
    @TableName("test_entity")
    static class TestEntity {
        @TableField(fill = FieldFill.INSERT)
        private String createUserId;

        @TableField(fill = FieldFill.INSERT_UPDATE)
        private String updateUserId;

        private String srcField;
        private String destField;
        private String idField;
    }

    @BeforeAll
    static void initTableInfo() {
        // 初始化 MyBatis Configuration 和注册 TableInfo
        Configuration configuration = new Configuration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
        TableInfo tableInfo = TableInfoHelper.initTableInfo(assistant, TestEntity.class);
    }

    @BeforeEach
    void setUp() {
        commonFieldsFiller = new CommonFieldsFiller();
    }

    @Test
    void testInsertFill_WithCurrentUser() {
        String userId = "user123";
        commonFieldsFiller.setCurrentUserIdSupplier(() -> userId);

        TestEntity entity = new TestEntity();
        MetaObject metaObject = SystemMetaObject.forObject(entity);

        commonFieldsFiller.insertFill(metaObject);

        assertEquals(userId, entity.getCreateUserId());
        assertEquals(userId, entity.getUpdateUserId());
    }

    @Test
    void testUpdateFill_WithCurrentUser() {
        String userId = "user456";
        commonFieldsFiller.setCurrentUserIdSupplier(() -> userId);

        TestEntity entity = new TestEntity();
        MetaObject metaObject = SystemMetaObject.forObject(entity);

        commonFieldsFiller.updateFill(metaObject);

        assertNull(entity.getCreateUserId()); // Should not be filled during update
        assertEquals(userId, entity.getUpdateUserId());
    }

    @Test
    void testInsertFill_FillFromKey() {
        TestEntity entity = new TestEntity();
        entity.setSrcField("sourceValue");
        MetaObject metaObject = SystemMetaObject.forObject(entity);

        commonFieldsFiller.addFillPair(TestEntity.class, "destField", "srcField");

        commonFieldsFiller.insertFill(metaObject);

        assertEquals("sourceValue", entity.getDestField());
    }

    @Test
    void testInsertFill_IdFillers() {
        TestEntity entity = new TestEntity();
        MetaObject metaObject = SystemMetaObject.forObject(entity);

        commonFieldsFiller.addFillWithId(TestEntity.class, "idField", () -> "generatedId");

        commonFieldsFiller.insertFill(metaObject);

        assertEquals("generatedId", entity.getIdField());
    }

    @Test
    void testInsertFill_WithoutCurrentUserIdSupplier() {
        // 测试没有设置 currentUserIdSupplier 的情况
        TestEntity entity = new TestEntity();
        MetaObject metaObject = SystemMetaObject.forObject(entity);

        commonFieldsFiller.insertFill(metaObject);

        assertNull(entity.getCreateUserId());
        assertNull(entity.getUpdateUserId());
    }

    @Test
    void testUpdateFill_WithoutCurrentUserIdSupplier() {
        // 测试没有设置 currentUserIdSupplier 的情况
        TestEntity entity = new TestEntity();
        MetaObject metaObject = SystemMetaObject.forObject(entity);

        commonFieldsFiller.updateFill(metaObject);

        assertNull(entity.getUpdateUserId());
    }

    @Test
    void testInsertFill_CombinedScenario() {
        // 测试组合场景：同时有 currentUserIdSupplier、FillFromKey 和 IdFillers
        String userId = "admin";
        commonFieldsFiller.setCurrentUserIdSupplier(() -> userId);
        commonFieldsFiller.addFillPair(TestEntity.class, "destField", "srcField");
        commonFieldsFiller.addFillWithId(TestEntity.class, "idField", () -> "12345");

        TestEntity entity = new TestEntity();
        entity.setSrcField("copyValue");
        MetaObject metaObject = SystemMetaObject.forObject(entity);

        commonFieldsFiller.insertFill(metaObject);

        assertEquals(userId, entity.getCreateUserId());
        assertEquals(userId, entity.getUpdateUserId());
        assertEquals("copyValue", entity.getDestField());
        assertEquals("12345", entity.getIdField());
    }

    @Test
    void testCustomFieldNames() {
        // 测试自定义字段名称
        commonFieldsFiller.setCreateUserField("customCreateUser");
        commonFieldsFiller.setUpdateUserField("customUpdateUser");

        assertEquals("customCreateUser", commonFieldsFiller.getCreateUserField());
        assertEquals("customUpdateUser", commonFieldsFiller.getUpdateUserField());
    }

    @Test
    void testChainedCalls() {
        // 测试链式调用
        CommonFieldsFiller filler = new CommonFieldsFiller()
                .setCurrentUserIdSupplier(() -> "chainUser")
                .addFillPair(TestEntity.class, "destField", "srcField")
                .addFillWithId(TestEntity.class, "idField", () -> "chainId");

        assertNotNull(filler);

        TestEntity entity = new TestEntity();
        entity.setSrcField("chainValue");
        MetaObject metaObject = SystemMetaObject.forObject(entity);

        filler.insertFill(metaObject);

        assertEquals("chainUser", entity.getCreateUserId());
        assertEquals("chainValue", entity.getDestField());
        assertEquals("chainId", entity.getIdField());
    }

    @Test
    void testMultipleFillPairs() {
        // 测试多个字段映射
        TestEntity entity = new TestEntity();
        entity.setSrcField("source1");
        entity.setIdField("source2");
        MetaObject metaObject = SystemMetaObject.forObject(entity);

        commonFieldsFiller.addFillPair(TestEntity.class, "destField", "srcField");
        commonFieldsFiller.addFillPair(TestEntity.class, "createUserId", "idField");

        commonFieldsFiller.insertFill(metaObject);

        assertEquals("source1", entity.getDestField());
        assertEquals("source2", entity.getCreateUserId());
    }

    @Test
    void testMultipleIdFillers() {
        // 测试多个ID生成器
        TestEntity entity = new TestEntity();
        MetaObject metaObject = SystemMetaObject.forObject(entity);

        commonFieldsFiller.addFillWithId(TestEntity.class, "srcField", () -> "id1");
        commonFieldsFiller.addFillWithId(TestEntity.class, "destField", () -> "id2");
        commonFieldsFiller.addFillWithId(TestEntity.class, "idField", () -> "id3");

        commonFieldsFiller.insertFill(metaObject);

        assertEquals("id1", entity.getSrcField());
        assertEquals("id2", entity.getDestField());
        assertEquals("id3", entity.getIdField());
    }

    @Data
    @TableName("another_entity")
    static class AnotherEntity {
        @TableField(fill = FieldFill.INSERT)
        private String createUserId;

        @TableField(fill = FieldFill.INSERT_UPDATE)
        private String updateUserId;

        private String ownField;
    }

    @BeforeAll
    static void initAnotherTableInfo() {
        // 为 AnotherEntity 注册 TableInfo
        Configuration configuration = new Configuration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
        TableInfoHelper.initTableInfo(assistant, AnotherEntity.class);
    }

    @Test
    void testEntityIsolation() {
        // 测试不同实体类的配置隔离
        commonFieldsFiller.addFillPair(TestEntity.class, "destField", "srcField");
        commonFieldsFiller.addFillWithId(TestEntity.class, "idField", () -> "testId");
        commonFieldsFiller.addFillWithId(AnotherEntity.class, "ownField", () -> "anotherId");

        // 测试 TestEntity
        TestEntity entity1 = new TestEntity();
        entity1.setSrcField("value1");
        MetaObject metaObject1 = SystemMetaObject.forObject(entity1);
        commonFieldsFiller.insertFill(metaObject1);

        assertEquals("value1", entity1.getDestField());
        assertEquals("testId", entity1.getIdField());

        // 测试 AnotherEntity（不应该有 TestEntity 的配置）
        AnotherEntity entity2 = new AnotherEntity();
        MetaObject metaObject2 = SystemMetaObject.forObject(entity2);
        commonFieldsFiller.insertFill(metaObject2);

        assertEquals("anotherId", entity2.getOwnField());
    }

    @Test
    void testUpdateFill_DoesNotFillCreateUserId() {
        // 验证 updateFill 不会填充 createUserId
        String userId = "updateUser";
        commonFieldsFiller.setCurrentUserIdSupplier(() -> userId);

        TestEntity entity = new TestEntity();
        entity.setCreateUserId("originalCreator");
        MetaObject metaObject = SystemMetaObject.forObject(entity);

        commonFieldsFiller.updateFill(metaObject);

        assertEquals("originalCreator", entity.getCreateUserId()); // 保持不变
        assertEquals(userId, entity.getUpdateUserId()); // 被更新
    }

    @Test
    void testFillFromKey_WithNullSourceValue() {
        // 测试源字段为 null 的情况
        TestEntity entity = new TestEntity();
        // srcField 保持为 null
        MetaObject metaObject = SystemMetaObject.forObject(entity);

        commonFieldsFiller.addFillPair(TestEntity.class, "destField", "srcField");

        commonFieldsFiller.insertFill(metaObject);

        assertNull(entity.getDestField()); // 应该填充 null 值
    }

    @Test
    void testSettersReturnThis() {
        // 验证 setter 方法返回 this 以支持链式调用
        CommonFieldsFiller result1 = commonFieldsFiller.setCurrentUserIdSupplier(() -> "user");
        assertSame(commonFieldsFiller, result1);

        CommonFieldsFiller result2 = commonFieldsFiller.addFillPair(TestEntity.class, "dest", "src");
        assertSame(commonFieldsFiller, result2);

        CommonFieldsFiller result3 = commonFieldsFiller.addFillWithId(TestEntity.class, "id", () -> "123");
        assertSame(commonFieldsFiller, result3);
    }
}
