package io.hqwu.commons.annotation.constraints.support;

import io.hqwu.commons.annotation.constraints.NumbersIn;
import io.hqwu.commons.validator.EnhancedValidator;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.NotNull;
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
class NumbersInArrayValidatorTest {

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
    void testValidArray() {
        NumbersInArrayPojo pojo = new NumbersInArrayPojo();
        
        // Array contains one valid value
        pojo.values = new Integer[]{1, 4};
        Set<ConstraintViolation<NumbersInArrayPojo>> violations = validator.validate(pojo);
        assertTrue(violations.isEmpty(), "Should be valid when array contains at least one valid value");
        
        // Array contains multiple valid values
        pojo.values = new Integer[]{1, 2, 3};
        violations = validator.validate(pojo);
        assertTrue(violations.isEmpty());
        
        // Array contains only valid values
        pojo.values = new Integer[]{2};
        violations = validator.validate(pojo);
        assertTrue(violations.isEmpty());
    }

    @Test
    void testInvalidArray() {
        NumbersInArrayPojo pojo = new NumbersInArrayPojo();
        
        // Array contains no valid values
        pojo.values = new Integer[]{4, 5, 6};
        Set<ConstraintViolation<NumbersInArrayPojo>> violations = validator.validate(pojo);
        assertEquals(1, violations.size(), "Should have 1 violation when array contains no valid values");
        
        // Empty array
        pojo.values = new Integer[]{};
        violations = validator.validate(pojo);
        assertEquals(1, violations.size(), "Should have 1 violation for empty array");
    }

    @Test
    void testNullValue() {
        NumbersInArrayPojo pojo = new NumbersInArrayPojo();
        pojo.values = null;
        
        Set<ConstraintViolation<NumbersInArrayPojo>> violations = validator.validate(pojo);
        assertTrue(violations.isEmpty(), "Should be valid for null value");
    }

    @Test
    void testDifferentNumberTypes() {
        NumbersInNumberArrayPojo numberPojo = new NumbersInNumberArrayPojo();
        numberPojo.values = new Long[]{1L, 4L};
        Set<ConstraintViolation<NumbersInNumberArrayPojo>> violations = validator.validate(numberPojo);
        assertTrue(violations.isEmpty(), "Should be valid for Long array");
        
        numberPojo.values = new Double[]{1.0, 4.0}; // 1.0 cast to long is 1
        violations = validator.validate(numberPojo);
        assertTrue(violations.isEmpty(), "Should be valid for Double array (casted to long)");
    }

    @Test
    void testMessageInterpolation() {
        MessagePojo pojo = new MessagePojo();
        pojo.number = 40; // Invalid value

        Set<ConstraintViolation<MessagePojo>> violations = validator.validate(pojo);
        assertEquals(1, violations.size());

        String message = violations.iterator().next().getMessage();
        assertEquals("Value must be in [10, 20, 30]", message, "Message should be correctly interpolated");
    }

    @Test
    void testNonNumbersInAnnotation() {
        // Test that other annotations (like @NotNull) are handled by the default interpolator
        // and not by NumbersInMessageInterpolator's special logic
        OtherAnnotationPojo pojo = new OtherAnnotationPojo();
        pojo.value = null;

        Set<ConstraintViolation<OtherAnnotationPojo>> violations = validator.validate(pojo);
        assertEquals(1, violations.size());
        
        // The message should be the default one for @NotNull, not affected by our interpolator
        String message = violations.iterator().next().getMessage();
        // Default message for NotNull is usually "must not be null"
        assertTrue(message.contains("must not be null") || message.contains("不能为null"), 
                   "Should use default interpolation for non-NumbersIn annotations");
    }

    static class NumbersInArrayPojo {
        @NumbersIn({1, 2, 3})
        Integer[] values;
    }
    
    static class NumbersInNumberArrayPojo {
        @NumbersIn({1, 2, 3})
        Number[] values;
    }

    static class MessagePojo {
        @NumbersIn(value = {10, 20, 30}, message = "Value must be in {value}")
        Integer number;
    }

    static class OtherAnnotationPojo {
        @NotNull
        String value;
    }
}
