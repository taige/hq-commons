package io.hqwu.commons.annotation.constraints.support;

import io.hqwu.commons.annotation.constraints.Base64;
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
class Base64ValidatorTest {

    @Autowired
    private Validator validator;

    @Test
    void testValidBase64() {
        Base64Pojo pojo = new Base64Pojo();
        pojo.value = java.util.Base64.getEncoder().encodeToString("test".getBytes());
        
        Set<ConstraintViolation<Base64Pojo>> violations = validator.validate(pojo);
        assertTrue(violations.isEmpty(), "Should be valid for correct Base64 string");
    }

    @Test
    void testInvalidBase64() {
        Base64Pojo pojo = new Base64Pojo();
        pojo.value = "invalid_base64_string!@#";
        
        Set<ConstraintViolation<Base64Pojo>> violations = validator.validate(pojo);
        assertEquals(1, violations.size(), "Should have 1 violation for invalid Base64 string");
    }

    @Test
    void testNullValue() {
        Base64Pojo pojo = new Base64Pojo();
        pojo.value = null;
        
        Set<ConstraintViolation<Base64Pojo>> violations = validator.validate(pojo);
        assertEquals(1, violations.size(), "Should have 1 violation for null value");
    }

    @Test
    void testEmptyValue() {
        Base64Pojo pojo = new Base64Pojo();
        pojo.value = "";
        
        Set<ConstraintViolation<Base64Pojo>> violations = validator.validate(pojo);
        assertEquals(1, violations.size(), "Should have 1 violation for empty string");
    }

    @Test
    void testBlankValue() {
        Base64Pojo pojo = new Base64Pojo();
        pojo.value = "   ";
        
        Set<ConstraintViolation<Base64Pojo>> violations = validator.validate(pojo);
        assertEquals(1, violations.size(), "Should have 1 violation for blank string");
    }

    static class Base64Pojo {
        @Base64
        String value;
    }
}
