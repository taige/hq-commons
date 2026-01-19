package io.hqwu.commons.annotation.constraints.support;

import io.hqwu.commons.validator.EnhancedValidator;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.Email;
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
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@BootstrapWith(DefaultTestContextBootstrapper.class)
@ExtendWith(SpringExtension.class)
@ImportAutoConfiguration(ValidationAutoConfiguration.class)
class EmailsValidatorTest {

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
    void testValidEmails() {
        EmailsPojo pojo = new EmailsPojo();
        
        // Single valid email
        pojo.emails = "test@example.com";
        Set<ConstraintViolation<EmailsPojo>> violations = validator.validate(pojo);
        assertTrue(violations.isEmpty(), "Should be valid for single email");
        
        // Multiple valid emails
        pojo.emails = "test1@example.com,test2@example.com";
        violations = validator.validate(pojo);
        printViolations(violations);
        assertTrue(violations.isEmpty(), "Should be valid for multiple emails");
        
        // Multiple valid emails with spaces
        pojo.emails = " test1@example.com ,  test2@example.com ";
        violations = validator.validate(pojo);
        printViolations(violations);
        assertTrue(violations.isEmpty(), "Should be valid for multiple emails with spaces");
    }

    @Test
    void testInvalidEmails() {
        EmailsPojo pojo = new EmailsPojo();
        
        // Single invalid email
        pojo.emails = "invalid-email";
        Set<ConstraintViolation<EmailsPojo>> violations = validator.validate(pojo);
        assertEquals(1, violations.size(), "Should have 1 violation for invalid email");
        
        // One invalid email among valid ones
        pojo.emails = "valid@example.com,invalid-email";
        violations = validator.validate(pojo);
        assertEquals(1, violations.size(), "Should have 1 violation when one email is invalid");
        
        // All invalid emails
        pojo.emails = "invalid1,invalid2";
        violations = validator.validate(pojo);
        assertEquals(1, violations.size());
    }

    @Test
    void testNullValue() {
        EmailsPojo pojo = new EmailsPojo();
        pojo.emails = null;
        
        Set<ConstraintViolation<EmailsPojo>> violations = validator.validate(pojo);
        assertTrue(violations.isEmpty(), "Should be valid for null value");
    }

    @Test
    void testEmptyValue() {
        EmailsPojo pojo = new EmailsPojo();
        pojo.emails = "";
        
        Set<ConstraintViolation<EmailsPojo>> violations = validator.validate(pojo);
        assertTrue(violations.isEmpty(), "Should be valid for empty string");
    }

    @Test
    void testBlankValue() {
        EmailsPojo pojo = new EmailsPojo();
        pojo.emails = "   ";
        
        Set<ConstraintViolation<EmailsPojo>> violations = validator.validate(pojo);
        assertTrue(violations.isEmpty(), "Should be valid for blank string");
    }

    private void printViolations(Set<ConstraintViolation<EmailsPojo>> violations) {
        if (!violations.isEmpty()) {
            String msg = violations.stream()
                    .map(v -> v.getPropertyPath() + " " + v.getMessage())
                    .collect(Collectors.joining(", "));
            System.out.println("Violations: " + msg);
        }
    }

    static class EmailsPojo {
        @Email
        String emails;
    }
}
