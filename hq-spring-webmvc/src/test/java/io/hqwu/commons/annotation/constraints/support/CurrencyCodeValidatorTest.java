package io.hqwu.commons.annotation.constraints.support;

import io.hqwu.commons.annotation.constraints.CurrencyCode;
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
class CurrencyCodeValidatorTest {

    @Autowired
    private Validator validator;

    @Test
    void testValidCurrencyCode() {
        CurrencyPojo pojo = new CurrencyPojo();
        pojo.currency = "USD";
        
        Set<ConstraintViolation<CurrencyPojo>> violations = validator.validate(pojo);
        assertTrue(violations.isEmpty(), "Should be valid for correct currency code");
        
        pojo.currency = "CNY";
        violations = validator.validate(pojo);
        assertTrue(violations.isEmpty());
    }

    @Test
    void testInvalidCurrencyCode() {
        CurrencyPojo pojo = new CurrencyPojo();
        pojo.currency = "INVALID";
        
        Set<ConstraintViolation<CurrencyPojo>> violations = validator.validate(pojo);
        assertEquals(1, violations.size(), "Should have 1 violation for invalid currency code");
    }

    @Test
    void testNullValue() {
        CurrencyPojo pojo = new CurrencyPojo();
        pojo.currency = null;
        
        // Default allowEmpty is false
        Set<ConstraintViolation<CurrencyPojo>> violations = validator.validate(pojo);
        assertEquals(1, violations.size(), "Should have 1 violation for null value when allowEmpty=false");
    }

    @Test
    void testEmptyValue() {
        CurrencyPojo pojo = new CurrencyPojo();
        pojo.currency = "";
        
        // Default allowEmpty is false
        Set<ConstraintViolation<CurrencyPojo>> violations = validator.validate(pojo);
        assertEquals(1, violations.size(), "Should have 1 violation for empty string when allowEmpty=false");
    }

    @Test
    void testAllowEmpty() {
        AllowEmptyPojo pojo = new AllowEmptyPojo();
        
        // Test null
        pojo.currency = null;
        Set<ConstraintViolation<AllowEmptyPojo>> violations = validator.validate(pojo);
        assertTrue(violations.isEmpty(), "Should be valid for null value when allowEmpty=true");
        
        // Test empty string
        pojo.currency = "";
        violations = validator.validate(pojo);
        assertTrue(violations.isEmpty(), "Should be valid for empty string when allowEmpty=true");
        
        // Test blank string
        pojo.currency = "   ";
        violations = validator.validate(pojo);
        assertTrue(violations.isEmpty(), "Should be valid for blank string when allowEmpty=true");
        
        // Test valid currency
        pojo.currency = "EUR";
        violations = validator.validate(pojo);
        assertTrue(violations.isEmpty());
        
        // Test invalid currency
        pojo.currency = "XXX_INVALID";
        violations = validator.validate(pojo);
        assertEquals(1, violations.size());
    }

    static class CurrencyPojo {
        @CurrencyCode
        String currency;
    }

    static class AllowEmptyPojo {
        @CurrencyCode(allowEmpty = true)
        String currency;
    }
}
