package io.hqwu.commons.annotation.constraints.support;

import io.hqwu.commons.utils.ConstraintViolationUtil;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.CustomLog;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.test.context.BootstrapWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.support.DefaultTestContextBootstrapper;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Created with IntelliJ IDEA for hq-commons-parent
 *
 * @author taige (Wu, Hongqiang)
 * Date: 2021-11-16
 * Time: 17:00
 */
@BootstrapWith(DefaultTestContextBootstrapper.class)
@ExtendWith(SpringExtension.class)
@ImportAutoConfiguration(ValidationAutoConfiguration.class)
@CustomLog
class AnyFieldsValidatedValidatorTest {

    @Autowired
    private Validator validator;

    @Test
    void test_() {
        {
            ValidPojo validPojo = new ValidPojo()
                    .setDftValidStr1("1")
                    .setStr3("3");
            Set<ConstraintViolation<Object>> violations = validator.validate(validPojo);
            assertEquals(0, violations.size());
        }
        {
            ValidPojo validPojo = new ValidPojo()
                    .setDftValidStr2("1")
                    .setStr3("3");
            Set<ConstraintViolation<Object>> violations = validator.validate(validPojo);
            assertEquals(0, violations.size());
        }
        {
            ValidPojo invalidPojo = new ValidPojo()
                    .setStr3("3");
            Set<ConstraintViolation<Object>> violations = validator.validate(invalidPojo);
            assertEquals(1, violations.size());
            String msg = violations.stream().map(v -> ConstraintViolationUtil.toString(v, true)).collect(
                    Collectors.joining(", ", String.format("object is not valid(%d): ", violations.size()), ""));
            LOGGER.debug(msg);
        }
        {
            ValidPojo pojo = new ValidPojo()
                    .setAliWxStr5("3")
                    ;
            Set<ConstraintViolation<Object>> violations = validator.validate(pojo, ValidPojo.AlipayConstraints.class);
            assertEquals(0, violations.size());
        }
        {
            ValidPojo pojo = new ValidPojo()
                    .setAliValidStr4("4")
                    ;
            Set<ConstraintViolation<Object>> violations = validator.validate(pojo, ValidPojo.AlipayConstraints.class);
            assertEquals(0, violations.size());
        }
        {
            ValidPojo pojo = new ValidPojo()
                    .setUpAliStr8("4")
                    ;
            Set<ConstraintViolation<Object>> violations = validator.validate(pojo, ValidPojo.AlipayConstraints.class);
            assertEquals(0, violations.size());
        }
        {
            ValidPojo pojo = new ValidPojo()
                    .setUpWxStr6("4")
                    ;
            Set<ConstraintViolation<Object>> violations = validator.validate(pojo, ValidPojo.AlipayConstraints.class);
            assertEquals(1, violations.size());
            String msg = violations.stream().map(v -> ConstraintViolationUtil.toString(v, false)).collect(
                    Collectors.joining(", ", String.format("object is not valid(%d): ", violations.size()), ""));
            LOGGER.debug(msg);
        }
    }

}