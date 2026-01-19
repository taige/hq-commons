package io.hqwu.commons.annotation.constraints.support;

import io.hqwu.commons.validator.EnhancedValidator;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.Size;
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
class SizeValidatorForBlankCharSequenceTest {

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
    void testValidSize() {
        SizePojo pojo = new SizePojo();
        
        // Valid size (length 3, min 2, max 5)
        pojo.value = "abc";
        Set<ConstraintViolation<SizePojo>> violations = validator.validate(pojo);
        assertTrue(violations.isEmpty(), "Should be valid for correct size");
        
        // Valid size (length 2, min 2)
        pojo.value = "ab";
        violations = validator.validate(pojo);
        assertTrue(violations.isEmpty());
        
        // Valid size (length 5, max 5)
        pojo.value = "abcde";
        violations = validator.validate(pojo);
        assertTrue(violations.isEmpty());
    }

    @Test
    void testInvalidSize() {
        SizePojo pojo = new SizePojo();
        
        // Too short (length 1, min 2)
        pojo.value = "a";
        Set<ConstraintViolation<SizePojo>> violations = validator.validate(pojo);
        assertEquals(1, violations.size(), "Should have 1 violation for too short string");
        
        // Too long (length 6, max 5)
        pojo.value = "abcdef";
        violations = validator.validate(pojo);
        assertEquals(1, violations.size(), "Should have 1 violation for too long string");
    }

    @Test
    void testNullValue() {
        SizePojo pojo = new SizePojo();
        pojo.value = null;
        
        // StringUtil.isBlank(null) is true -> valid
        Set<ConstraintViolation<SizePojo>> violations = validator.validate(pojo);
        assertTrue(violations.isEmpty(), "Should be valid for null value");
    }

    @Test
    void testEmptyValue() {
        SizePojo pojo = new SizePojo();
        pojo.value = "";
        
        // StringUtil.isBlank("") is true -> valid
        Set<ConstraintViolation<SizePojo>> violations = validator.validate(pojo);
        assertTrue(violations.isEmpty(), "Should be valid for empty string");
    }

    @Test
    void testBlankValue() {
        SizePojo pojo = new SizePojo();
        pojo.value = "   ";
        
        // StringUtil.isBlank("   ") is true -> valid
        // Note: Standard @Size validator would count length 3 and fail (since min=2 is met but if min was 4 it would fail)
        // But here we explicitly test that blank strings are valid regardless of length constraints?
        // Wait, StringUtil.isBlank returns true for whitespace.
        // So even if length is 3 and min=2, max=5, it is valid by super.isValid too.
        // Let's test a case where standard validator would fail but this one passes.
        // E.g. min=5, value="   " (length 3). Standard fails, this passes.
        
        SizeMin5Pojo min5Pojo = new SizeMin5Pojo();
        min5Pojo.value = "   "; // length 3
        
        Set<ConstraintViolation<SizeMin5Pojo>> violations = validator.validate(min5Pojo);
        assertTrue(violations.isEmpty(), "Should be valid for blank string even if length < min");
    }

    static class SizePojo {
        @Size(min = 2, max = 5)
        String value;
    }
    
    static class SizeMin5Pojo {
        @Size(min = 5)
        String value;
    }
}
