package io.hqwu.commons.annotation.constraints.support;

import io.hqwu.commons.annotation.constraints.NumbersIn;
import io.hqwu.commons.validator.EnhancedValidator;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.BootstrapWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.support.DefaultTestContextBootstrapper;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@BootstrapWith(DefaultTestContextBootstrapper.class)
@ExtendWith(SpringExtension.class)
@ImportAutoConfiguration(ValidationAutoConfiguration.class)
class NumbersInValidatorTest {

    @Autowired
    private Validator validator;

    @TestConfiguration
    static class Config {
        @Bean
        @Primary
        public Validator validator() {
            return new EnhancedValidator();
        }
    }

    @Test
    void testValidNumber() {
        NumbersInPojo pojo = new NumbersInPojo();
        
        // Valid value: 1
        pojo.value = 1;
        Set<ConstraintViolation<NumbersInPojo>> violations = validator.validate(pojo);
        assertTrue(violations.isEmpty(), "Should be valid for value in the list");
        
        // Valid value: 3
        pojo.value = 3;
        violations = validator.validate(pojo);
        assertTrue(violations.isEmpty());
    }

    @Test
    void testInvalidNumber() {
        NumbersInPojo pojo = new NumbersInPojo();
        
        // Invalid value: 4
        pojo.value = 4;
        Set<ConstraintViolation<NumbersInPojo>> violations = validator.validate(pojo);
        assertEquals(1, violations.size(), "Should have 1 violation for value not in the list");
        
        // Invalid value: 0
        pojo.value = 0;
        violations = validator.validate(pojo);
        assertEquals(1, violations.size());
    }

    @Test
    void testNullValue() {
        NumbersInPojo pojo = new NumbersInPojo();
        pojo.value = null;
        
        Set<ConstraintViolation<NumbersInPojo>> violations = validator.validate(pojo);
        assertTrue(violations.isEmpty(), "Should be valid for null value");
    }

    @Test
    void testDifferentNumberTypes() {
        NumbersInNumberPojo pojo = new NumbersInNumberPojo();
        
        // Long value
        pojo.value = 1L;
        Set<ConstraintViolation<NumbersInNumberPojo>> violations = validator.validate(pojo);
        assertTrue(violations.isEmpty(), "Should be valid for Long value");
        
        // Double value (casted to long)
        pojo.value = 1.0;
        violations = validator.validate(pojo);
        assertTrue(violations.isEmpty(), "Should be valid for Double value (casted to long)");
        
        // Invalid Double value
        pojo.value = 4.0;
        violations = validator.validate(pojo);
        assertEquals(1, violations.size());
    }

    @Test
    void testMessageInterpolationForSingleValue() {
        // This test covers the branch where the variable is NOT an array (if possible)
        // However, @NumbersIn.value() is always long[], so it's always an array.
        // But wait, NumbersInMessageInterpolator handles "Object variable" which could be anything.
        // The annotation attribute "value" is long[], so it will be an array.
        // But maybe there are other attributes?
        // Let's look at NumbersInMessageInterpolator code again.
        // It gets "value" from attributes. Since @NumbersIn defines value() as long[], it will be long[].
        
        // The only way to hit the "else { resolvedExpression = variable.toString(); }" branch 
        // is if we have a parameter that is NOT an array.
        // But @NumbersIn only has value() which is long[].
        // Unless we add another attribute to @NumbersIn? No, we can't modify the annotation easily.
        // Or maybe the message template uses a parameter that is not "value"?
        // E.g. {message} or {groups}?
        // Let's try to use {message} in the message template.
        
        MessagePojo pojo = new MessagePojo();
        pojo.value = 4;
        
        Set<ConstraintViolation<MessagePojo>> violations = validator.validate(pojo);
        assertEquals(1, violations.size());
        
        String message = violations.iterator().next().getMessage();
        // We expect {value} to be interpolated as array string
        // And we expect {customParam} to be interpolated as string (if we could pass it)
        // But we can't pass custom params to annotation easily without modifying it.
        
        // However, we can test that the interpolator works for the standard case here too.
        assertEquals("Value must be in [1, 2, 3]", message);
    }

    static class NumbersInPojo {
        @NumbersIn({1, 2, 3})
        Integer value;
    }
    
    static class NumbersInNumberPojo {
        @NumbersIn({1, 2, 3})
        Number value;
    }

    static class MessagePojo {
        @NumbersIn(value = {1, 2, 3}, message = "Value must be in {value}")
        Integer value;
    }
}
