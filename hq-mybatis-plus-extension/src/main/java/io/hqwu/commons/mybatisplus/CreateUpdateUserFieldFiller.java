package io.hqwu.commons.mybatisplus;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;

import java.util.function.Supplier;

/**
 * Created with IntelliJ IDEA for hq-commons-parent
 *
 * @author taige (Wu, Hongqiang)
 * Date: 2021-09-28
 * Time: 17:27
 */
public class CreateUpdateUserFieldFiller implements MetaObjectHandler {

    private final Supplier<String> currentUserSupplier;

    private String createUserField = "createUserId";

    private String updateUserField = "updateUserId";

    public CreateUpdateUserFieldFiller(Supplier<String> currentUserSupplier) {
        this.currentUserSupplier = currentUserSupplier;
    }

    @Override
    public void insertFill(MetaObject metaObject) {
        String currentUserId = currentUserSupplier.get();
        this.strictInsertFill(metaObject, createUserField, String.class, currentUserId);
        this.strictInsertFill(metaObject, updateUserField, String.class, currentUserId);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, updateUserField, currentUserSupplier, String.class);
    }

    public void setCreateUserField(String createUserField) {
        this.createUserField = createUserField;
    }

    public void setUpdateUserField(String updateUserField) {
        this.updateUserField = updateUserField;
    }

    public String getCreateUserField() {
        return createUserField;
    }

    public String getUpdateUserField() {
        return updateUserField;
    }

}
