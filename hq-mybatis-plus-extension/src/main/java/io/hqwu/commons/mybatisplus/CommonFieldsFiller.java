package io.hqwu.commons.mybatisplus;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import lombok.Getter;
import lombok.Setter;
import org.apache.ibatis.reflection.MetaObject;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * MyBatis-Plus通用字段自动填充处理器
 * <p>
 * 该类实现了{@link MetaObjectHandler}接口,用于在执行数据库插入和更新操作时自动填充实体类的公共字段。
 * 主要功能包括:
 * <ul>
 *   <li>插入时自动填充创建人ID和更新人ID</li>
 *   <li>更新时自动填充更新人ID</li>
 *   <li>支持自定义字段间的值复制填充</li>
 *   <li>支持通过{@link Supplier}动态生成字段值(如ID生成器)</li>
 * </ul>
 * </p>
 *
 * <p>使用示例:</p>
 * <pre>{@code
 * CommonFieldsFiller filler = new CommonFieldsFiller()
 *     .setCurrentUserIdSupplier(() -> getCurrentUserId())
 *     .addFillPair(Order.class, "merchantId", "createUserId")
 *     .addFillWithId(Order.class, "orderId", () -> idGenerator.nextId());
 * }</pre>
 *
 * @author taige (Wu, Hongqiang)
 * @see MetaObjectHandler
 * @see Supplier
 * @since 2021-09-28
 */
@Setter
@Getter
public class CommonFieldsFiller implements MetaObjectHandler {

    /**
     * 当前用户ID提供者
     * <p>用于获取当前操作用户的ID,在插入和更新操作时自动填充创建人和更新人字段</p>
     */
    private Supplier<String> currentUserIdSupplier;

    /**
     * 创建人字段名称
     * <p>默认值为"createUserId",可通过{@link #setCreateUserField(String)}方法自定义</p>
     */
    private String createUserField = "createUserId";

    /**
     * 更新人字段名称
     * <p>默认值为"updateUserId",可通过{@link #setUpdateUserField(String)}方法自定义</p>
     */
    private String updateUserField = "updateUserId";

    /**
     * 字段间复制填充配置映射
     * <p>键为实体类类型,值为字段映射关系(目标字段名 -> 源字段名)</p>
     * <p>用于在插入时将一个字段的值复制到另一个字段</p>
     */
    private Map<Class<?>, Map<String, String>> fillFromKey = new HashMap<>();

    /**
     * ID字段填充器映射
     * <p>键为实体类类型,值为字段填充器映射(字段名 -> 值提供者)</p>
     * <p>用于在插入时通过{@link Supplier}动态生成字段值,如ID生成器</p>
     */
    private Map<Class<?>, Map<String, Supplier<?>>> idFillers = new HashMap<>();

    /**
     * 设置当前用户ID提供者
     *
     * @param currentUserIdSupplier 当前用户ID提供者,用于获取当前操作用户的ID
     * @return 当前实例,支持链式调用
     */
    public CommonFieldsFiller setCurrentUserIdSupplier(Supplier<String> currentUserIdSupplier) {
        this.currentUserIdSupplier = currentUserIdSupplier;
        return this;
    }

    /**
     * 插入操作时的字段自动填充
     * <p>执行以下填充操作:</p>
     * <ul>
     *   <li>使用当前用户ID填充创建人和更新人字段</li>
     *   <li>根据配置的字段映射关系进行字段间值复制</li>
     *   <li>使用配置的ID生成器填充ID字段</li>
     * </ul>
     *
     * @param metaObject 元对象,包含实体对象的元数据信息
     */
    @Override
    public void insertFill(MetaObject metaObject) {
        if (currentUserIdSupplier != null) {
            String currentUserId = currentUserIdSupplier.get();
            this.strictInsertFill(metaObject, createUserField, String.class, currentUserId);
            this.strictInsertFill(metaObject, updateUserField, String.class, currentUserId);
        }
        Map<String, String> keyPairs = fillFromKey.get(metaObject.getOriginalObject().getClass());
        if (keyPairs != null) {
            keyPairs.forEach((destField, srcField) -> {
                Object value = metaObject.getValue(srcField);
                this.fillStrategy(metaObject, destField, value);
            });
        }
        Map<String, Supplier<?>> supplierMap = idFillers.get(metaObject.getOriginalObject().getClass());
        if (supplierMap != null) {
            supplierMap.forEach((fieldName, supplier) -> {
                Object value = supplier.get();
                this.fillStrategy(metaObject, fieldName, value);
            });
        }
    }

    /**
     * 更新操作时的字段自动填充
     * <p>使用当前用户ID填充更新人字段</p>
     *
     * @param metaObject 元对象,包含实体对象的元数据信息
     */
    @Override
    public void updateFill(MetaObject metaObject) {
        if (currentUserIdSupplier != null) {
            this.strictUpdateFill(metaObject, updateUserField, currentUserIdSupplier, String.class);
        }
    }

    /**
     * 添加字段间复制填充配置
     * <p>在插入操作时,将源字段的值复制到目标字段</p>
     *
     * @param clz           实体类类型
     * @param destFieldName 目标字段名称
     * @param srcFieldName  源字段名称
     * @return 当前实例,支持链式调用
     */
    public CommonFieldsFiller addFillPair(Class<?> clz, String destFieldName, String srcFieldName) {
        Map<String, String> pairs = fillFromKey.computeIfAbsent(clz, k -> new HashMap<>());
        pairs.put(destFieldName, srcFieldName);
        return this;
    }

    /**
     * 添加ID字段填充器
     * <p>在插入操作时,使用提供的{@link Supplier}动态生成字段值</p>
     *
     * @param clz        实体类类型
     * @param fieldName  字段名称
     * @param idSupplier ID值提供者,用于动态生成字段值
     * @return 当前实例,支持链式调用
     */
    public CommonFieldsFiller addFillWithId(Class<?> clz, String fieldName, Supplier<?> idSupplier) {
        Map<String, Supplier<?>> supplierMap = idFillers.computeIfAbsent(clz, k -> new HashMap<>());
        supplierMap.put(fieldName, idSupplier);
        return this;
    }
}
