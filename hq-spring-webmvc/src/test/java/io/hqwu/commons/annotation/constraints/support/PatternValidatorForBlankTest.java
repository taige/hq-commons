package io.hqwu.commons.annotation.constraints.support;

import io.hqwu.commons.validator.EnhancedValidator;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.Pattern;
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
class PatternValidatorForBlankTest {

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
    void testValidPattern() {
        PatternPojo pojo = new PatternPojo();
        
        // Valid pattern (digits only)
        pojo.value = "12345";
        Set<ConstraintViolation<PatternPojo>> violations = validator.validate(pojo);
        assertTrue(violations.isEmpty(), "Should be valid for matching pattern");
    }

    @Test
    void testInvalidPattern() {
        PatternPojo pojo = new PatternPojo();
        
        // Invalid pattern (contains letters)
        pojo.value = "123a45";
        Set<ConstraintViolation<PatternPojo>> violations = validator.validate(pojo);
        assertEquals(1, violations.size(), "Should have 1 violation for non-matching pattern");
    }

    @Test
    void testNullValue() {
        PatternPojo pojo = new PatternPojo();
        pojo.value = null;
        
        // StringUtil.isBlank(null) is true -> valid
        Set<ConstraintViolation<PatternPojo>> violations = validator.validate(pojo);
        assertTrue(violations.isEmpty(), "Should be valid for null value");
    }

    @Test
    void testEmptyValue() {
        PatternPojo pojo = new PatternPojo();
        pojo.value = "";
        
        // StringUtil.isBlank("") is true -> valid
        Set<ConstraintViolation<PatternPojo>> violations = validator.validate(pojo);
        assertTrue(violations.isEmpty(), "Should be valid for empty string");
    }

    @Test
    void testBlankValue() {
        PatternPojo pojo = new PatternPojo();
        pojo.value = "   ";
        
        // StringUtil.isBlank("   ") is true -> valid
        // Standard @Pattern would fail because "   " does not match "\\d+"
        Set<ConstraintViolation<PatternPojo>> violations = validator.validate(pojo);
        assertTrue(violations.isEmpty(), "Should be valid for blank string even if it doesn't match pattern");
    }

    static class PatternPojo {
        @Pattern(regexp = "\\d+")
        String value;
    }
}
