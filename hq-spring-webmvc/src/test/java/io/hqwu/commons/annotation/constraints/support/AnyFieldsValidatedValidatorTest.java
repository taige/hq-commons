package io.hqwu.commons.annotation.constraints.support;

import io.hqwu.commons.annotation.constraints.AnyFieldsValidated;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ValidationException;
import jakarta.validation.Validator;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.test.context.BootstrapWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.support.DefaultTestContextBootstrapper;

import java.util.*;

import static io.hqwu.commons.annotation.constraints.AnyFieldsValidated.AnyField;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@BootstrapWith(DefaultTestContextBootstrapper.class)
@ExtendWith(SpringExtension.class)
@ImportAutoConfiguration(ValidationAutoConfiguration.class)
class AnyFieldsValidatedValidatorTest {

    @Autowired
    private Validator validator;

    @Test
    void testNotBlank() {
        // 使用 ValidPojo 的 AlipayConstraints 分组进行 NotBlank 测试
        // 涉及字段: aliValidStr4, aliWxStr5, upAliStr8

        // Case 1: 所有字段为空时验证失败
        ValidPojo invalidPojo = new ValidPojo();
        Set<ConstraintViolation<ValidPojo>> violations = validator.validate(invalidPojo, ValidPojo.AlipayConstraints.class);
        assertEquals(1, violations.size());
        // 验证消息包含 NotBlank 相关信息
        String msg = violations.iterator().next().getMessage();
        assertTrue(msg.contains("NotBlank") || msg.contains("not be blank"));

        // Case 2: 设置 aliValidStr4 后验证成功
        ValidPojo validPojo1 = new ValidPojo();
        validPojo1.setAliValidStr4("value");
        violations = validator.validate(validPojo1, ValidPojo.AlipayConstraints.class);
        assertTrue(violations.isEmpty());

        // Case 3: 设置 aliWxStr5 后验证成功
        ValidPojo validPojo2 = new ValidPojo();
        validPojo2.setAliWxStr5("value");
        violations = validator.validate(validPojo2, ValidPojo.AlipayConstraints.class);
        assertTrue(violations.isEmpty());

        // Case 4: 字段为空白字符时验证失败
        ValidPojo emptyPojo = new ValidPojo();
        emptyPojo.setAliValidStr4("   ");
        violations = validator.validate(emptyPojo, ValidPojo.AlipayConstraints.class);
        assertEquals(1, violations.size());
    }

    @Test
    void testNotEmpty() {
        // String 类型测试使用 ValidPojo (默认分组为 NotEmpty)
        ValidPojo pojo = new ValidPojo();
        // dftValidStr1 和 dftValidStr2 在默认分组
        assertEquals(1, validator.validate(pojo).size());

        pojo.setDftValidStr1("");
        assertEquals(1, validator.validate(pojo).size());

        pojo.setDftValidStr1("a");
        assertTrue(validator.validate(pojo).isEmpty());

        // Collection 类型测试仍需使用 NotEmptyPojo
        NotEmptyPojo listPojo = new NotEmptyPojo();
        listPojo.listField = new ArrayList<>();
        assertEquals(1, validator.validate(listPojo).size());

        listPojo.listField.add("item");
        assertTrue(validator.validate(listPojo).isEmpty());

        // Map 类型测试仍需使用 NotEmptyPojo
        NotEmptyPojo mapPojo = new NotEmptyPojo();
        mapPojo.mapField = new HashMap<>();
        assertEquals(1, validator.validate(mapPojo).size());

        mapPojo.mapField.put("k", "v");
        assertTrue(validator.validate(mapPojo).isEmpty());
    }

    @Test
    void testNotNull() {
        NotNullPojo pojo = new NotNullPojo();
        assertEquals(1, validator.validate(pojo).size());

        pojo.objField = new Object();
        assertTrue(validator.validate(pojo).isEmpty());
    }

    @Test
    void testGroups() {
        ValidPojo pojo = new ValidPojo();

        // 验证 AlipayConstraints (NotBlank)
        // 字段: aliValidStr4, aliWxStr5, upAliStr8
        Set<ConstraintViolation<ValidPojo>> violations = validator.validate(pojo, ValidPojo.AlipayConstraints.class);
        assertEquals(1, violations.size());

        pojo.setAliValidStr4("val");
        violations = validator.validate(pojo, ValidPojo.AlipayConstraints.class);
        assertTrue(violations.isEmpty());

        // 验证 WechatConstraints (NotBlank)
        // 字段: aliWxStr5, upWxStr6, wxValidStr9
        pojo.setAliValidStr4(null); // 重置
        // 此时 aliWxStr5, upWxStr6, wxValidStr9 均为 null
        violations = validator.validate(pojo, ValidPojo.WechatConstraints.class);
        assertEquals(1, violations.size());

        pojo.setWxValidStr9("val");
        violations = validator.validate(pojo, ValidPojo.WechatConstraints.class);
        assertTrue(violations.isEmpty());

        // 交叉分组测试
        // 设置一个属于 Alipay 但不属于 Wechat 的字段
        pojo.setWxValidStr9(null);
        pojo.setAliValidStr4("val"); // 属于 Alipay, 不属于 Wechat
        violations = validator.validate(pojo, ValidPojo.WechatConstraints.class);
        assertEquals(1, violations.size()); // Wechat 验证应失败
    }

    @Test
    void testUnsupportedConstraint() {
        UnsupportedPojo pojo = new UnsupportedPojo();
        assertThrows(ValidationException.class, () -> validator.validate(pojo));
    }

    @Test
    void testNullObject() {
        AnyFieldsValidatedValidator manualValidator = new AnyFieldsValidatedValidator();

        ConstraintValidatorContext context = mock(ConstraintValidatorContext.class);
        ConstraintValidatorContext.ConstraintViolationBuilder builder = mock(ConstraintValidatorContext.ConstraintViolationBuilder.class);

        when(context.buildConstraintViolationWithTemplate(anyString())).thenReturn(builder);
        when(builder.addConstraintViolation()).thenReturn(context);

        boolean result = manualValidator.isValid(null, context);

        assertFalse(result);
        verify(context).disableDefaultConstraintViolation();
        verify(context).buildConstraintViolationWithTemplate("this object requires not null");
    }

    @Test
    void testExceptionHandling() {
        AnyFieldsValidatedValidator manualValidator = new AnyFieldsValidatedValidator();

        AnyFieldsValidated annotation = mock(AnyFieldsValidated.class);
        doReturn(jakarta.validation.constraints.NotBlank.class).when(annotation).constraint();
        manualValidator.initialize(annotation);

        // 传入 mock 的 context，触发异常处理逻辑
        ConstraintValidatorContext context = mock(ConstraintValidatorContext.class);

        boolean result = manualValidator.isValid(new Object(), context);

        assertFalse(result);
    }

    @AnyFieldsValidated(constraint = NotEmpty.class)
    static class NotEmptyPojo {
        @AnyField
        String stringField;

        @AnyField
        List<String> listField;

        @AnyField
        Map<String, String> mapField;
    }

    @AnyFieldsValidated(constraint = NotNull.class)
    static class NotNullPojo {
        @AnyField
        Object objField;
    }

    @AnyFieldsValidated(constraint = Size.class)
    static class UnsupportedPojo {
        @AnyField
        String field;
    }
}
