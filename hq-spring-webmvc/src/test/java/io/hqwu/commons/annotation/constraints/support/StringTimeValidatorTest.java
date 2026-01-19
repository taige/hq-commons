package io.hqwu.commons.annotation.constraints.support;

import io.hqwu.commons.annotation.constraints.StringTime;
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
class StringTimeValidatorTest {

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
    void testValidDate() {
        DatePojo pojo = new DatePojo();
        
        // Valid date
        pojo.date = "2023-10-25";
        Set<ConstraintViolation<DatePojo>> violations = validator.validate(pojo);
        assertTrue(violations.isEmpty(), "Should be valid for correct date format");
    }

    @Test
    void testInvalidDate() {
        DatePojo pojo = new DatePojo();
        
        // Invalid format (missing leading zero)
        pojo.date = "2023-10-5";
        Set<ConstraintViolation<DatePojo>> violations = validator.validate(pojo);
        assertEquals(1, violations.size(), "Should have 1 violation for invalid date format (missing zero)");
        
        // Invalid format (wrong separator)
        pojo.date = "2023/10/25";
        violations = validator.validate(pojo);
        assertEquals(1, violations.size(), "Should have 1 violation for invalid separator");
        
        // Invalid date (month out of range)
        pojo.date = "2023-13-01";
        violations = validator.validate(pojo);
        assertEquals(1, violations.size(), "Should have 1 violation for invalid month");
        
        // Invalid date (day out of range)
        pojo.date = "2023-02-30";
        violations = validator.validate(pojo);
        assertEquals(1, violations.size(), "Should have 1 violation for invalid day");
        
        // Not a date
        pojo.date = "not-a-date";
        violations = validator.validate(pojo);
        assertEquals(1, violations.size());
    }

    @Test
    void testValidDateTime() {
        DateTimePojo pojo = new DateTimePojo();
        
        // Valid datetime
        pojo.dateTime = "2023-10-25 14:30:00";
        Set<ConstraintViolation<DateTimePojo>> violations = validator.validate(pojo);
        assertTrue(violations.isEmpty(), "Should be valid for correct datetime format");
    }

    @Test
    void testInvalidDateTime() {
        DateTimePojo pojo = new DateTimePojo();
        
        // Invalid format (missing seconds)
        pojo.dateTime = "2023-10-25 14:30";
        Set<ConstraintViolation<DateTimePojo>> violations = validator.validate(pojo);
        assertEquals(1, violations.size(), "Should have 1 violation for missing seconds");
    }

    @Test
    void testValidTime() {
        TimePojo pojo = new TimePojo();
        
        // Valid time
        pojo.time = "14:30";
        Set<ConstraintViolation<TimePojo>> violations = validator.validate(pojo);
        assertTrue(violations.isEmpty(), "Should be valid for correct time format");
    }

    @Test
    void testNullValue() {
        DatePojo pojo = new DatePojo();
        pojo.date = null;
        
        Set<ConstraintViolation<DatePojo>> violations = validator.validate(pojo);
        assertTrue(violations.isEmpty(), "Should be valid for null value");
    }

    @Test
    void testEmptyValue() {
        DatePojo pojo = new DatePojo();
        pojo.date = "";
        
        Set<ConstraintViolation<DatePojo>> violations = validator.validate(pojo);
        assertTrue(violations.isEmpty(), "Should be valid for empty string");
    }

    @Test
    void testBlankValue() {
        DatePojo pojo = new DatePojo();
        pojo.date = "   ";
        
        Set<ConstraintViolation<DatePojo>> violations = validator.validate(pojo);
        assertTrue(violations.isEmpty(), "Should be valid for blank string");
    }

    static class DatePojo {
        @StringTime("yyyy-MM-dd")
        String date;
    }
    
    static class DateTimePojo {
        @StringTime("yyyy-MM-dd HH:mm:ss")
        String dateTime;
    }
    
    static class TimePojo {
        @StringTime("HH:mm")
        String time;
    }
}
