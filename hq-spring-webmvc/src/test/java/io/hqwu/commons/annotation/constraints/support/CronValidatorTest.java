package io.hqwu.commons.annotation.constraints.support;

import io.hqwu.commons.annotation.constraints.Cron;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.test.context.BootstrapWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.support.DefaultTestContextBootstrapper;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@BootstrapWith(DefaultTestContextBootstrapper.class)
@ExtendWith(SpringExtension.class)
@ImportAutoConfiguration(ValidationAutoConfiguration.class)
class CronValidatorTest {

    @Autowired
    private Validator validator;

    @Test
    void testValidCron() {
        CronPojo pojo = new CronPojo();
        // Valid cron: every second
        pojo.expression = "* * * * * *";
        
        Set<ConstraintViolation<CronPojo>> violations = validator.validate(pojo);
        assertTrue(violations.isEmpty(), "Should be valid for correct Cron expression");
        
        // Valid cron: every day at midnight
        pojo.expression = "0 0 0 * * *";
        violations = validator.validate(pojo);
        assertTrue(violations.isEmpty());
    }

    @Test
    void testInvalidCron() {
        CronPojo pojo = new CronPojo();
        // Invalid cron: too few fields
        pojo.expression = "* * *";
        
        Set<ConstraintViolation<CronPojo>> violations = validator.validate(pojo);
        assertEquals(1, violations.size(), "Should have 1 violation for invalid Cron expression");
        
        // Invalid cron: invalid characters
        pojo.expression = "invalid cron";
        violations = validator.validate(pojo);
        assertEquals(1, violations.size());
    }

    @Test
    void testNullValue() {
        CronPojo pojo = new CronPojo();
        pojo.expression = null;
        
        // CronExpression.isValidExpression(null) returns false
        Set<ConstraintViolation<CronPojo>> violations = validator.validate(pojo);
        assertEquals(1, violations.size(), "Should have 1 violation for null value");
    }

    @Test
    void testEmptyValue() {
        CronPojo pojo = new CronPojo();
        pojo.expression = "";
        
        // CronExpression.isValidExpression("") returns false
        Set<ConstraintViolation<CronPojo>> violations = validator.validate(pojo);
        assertEquals(1, violations.size(), "Should have 1 violation for empty string");
    }

    static class CronPojo {
        @Cron
        String expression;
    }
}
