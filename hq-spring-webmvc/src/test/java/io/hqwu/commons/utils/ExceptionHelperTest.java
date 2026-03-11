package io.hqwu.commons.utils;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;

import java.lang.reflect.Field;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExceptionHelperTest {

    @Mock
    private Environment mockEnvironment;

    private ExceptionHelper exceptionHelper;

    private static final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @BeforeEach
    void setUp() throws Exception {
        // Reset static fields before each test to ensure isolation
        resetStaticField(ExceptionHelper.class, "env");
        resetStaticField(ExceptionHelper.class, "debug");
        exceptionHelper = new ExceptionHelper();
    }

    @AfterEach
    void tearDown() throws Exception {
        // Clean up static fields after each test
        resetStaticField(ExceptionHelper.class, "env");
        resetStaticField(ExceptionHelper.class, "debug");
    }

    private void resetStaticField(Class<?> clazz, String fieldName) throws Exception {
        Field field = clazz.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(null, null);
        if (field.getType().equals(Boolean.class)) {
             Field debugField = ExceptionHelper.class.getDeclaredField("debug");
             debugField.setAccessible(true);
             debugField.set(null, Boolean.FALSE);
        }
    }

    @Test
    void getRootMessage_shouldReturnFirstLineOfRootCauseMessage() {
        Exception rootCause = new Exception("Root cause message\nSecond line");
        Throwable throwable = new RuntimeException("Wrapper exception", rootCause);

        String message = ExceptionHelper.getRootMessage(throwable);

        assertThat(message).isEqualTo("Root cause message");
    }

    @Test
    void getRootMessage_shouldReturnNullWhenRootCauseMessageIsNull() {
        Exception rootCause = new Exception((String) null);
        Throwable throwable = new RuntimeException("Wrapper exception", rootCause);

        String message = ExceptionHelper.getRootMessage(throwable);

        assertThat(message).isNull();
    }

    @Test
    void getRootMessage_shouldReturnMessageFromSingleException() {
        Throwable throwable = new RuntimeException("Simple exception message");

        String message = ExceptionHelper.getRootMessage(throwable);

        assertThat(message).isEqualTo("Simple exception message");
    }

    @Test
    void debug_shouldReturnFalseWhenEnvIsNull() {
        assertThat(ExceptionHelper.debug()).isFalse();
    }

    @Test
    void setEnvironment_shouldSetDebugToTrueWhenPropertyIsTrue() {
        when(mockEnvironment.containsProperty("debug")).thenReturn(true);
        when(mockEnvironment.getProperty("debug", Boolean.class)).thenReturn(Boolean.TRUE);

        exceptionHelper.setEnvironment(mockEnvironment);

        assertThat(ExceptionHelper.debug()).isTrue();
    }

    @Test
    void setEnvironment_shouldSetDebugToFalseWhenPropertyIsFalse() {
        when(mockEnvironment.containsProperty("debug")).thenReturn(true);
        when(mockEnvironment.getProperty("debug", Boolean.class)).thenReturn(Boolean.FALSE);

        exceptionHelper.setEnvironment(mockEnvironment);

        assertThat(ExceptionHelper.debug()).isFalse();
    }

    @Test
    void setEnvironment_shouldKeepDebugFalseWhenPropertyIsNotPresent() {
        when(mockEnvironment.containsProperty("debug")).thenReturn(false);

        exceptionHelper.setEnvironment(mockEnvironment);

        assertThat(ExceptionHelper.debug()).isFalse();
    }

    @Test
    void formatConstraintViolation_shouldReturnSimpleMessageWhenDebugIsFalse() {
        // Ensure debug is false
        when(mockEnvironment.containsProperty("debug")).thenReturn(false);
        exceptionHelper.setEnvironment(mockEnvironment);
        assertThat(ExceptionHelper.debug()).isFalse();

        TestBean bean = new TestBean();
        Set<ConstraintViolation<TestBean>> violations = validator.validate(bean);
        ConstraintViolation<TestBean> violation = violations.iterator().next();

        String formattedMessage = ExceptionHelper.formatConstraintViolation(violation);

        // Corrected assertion based on actual library output
        assertThat(formattedMessage).isEqualTo("`name` must not be null (got:null)");
    }

    @Test
    void formatConstraintViolation_shouldReturnDetailedMessageWhenDebugIsTrue() {
        // Ensure debug is true
        when(mockEnvironment.containsProperty("debug")).thenReturn(true);
        when(mockEnvironment.getProperty("debug", Boolean.class)).thenReturn(Boolean.TRUE);
        exceptionHelper.setEnvironment(mockEnvironment);
        assertThat(ExceptionHelper.debug()).isTrue();

        TestBean bean = new TestBean();
        Set<ConstraintViolation<TestBean>> violations = validator.validate(bean);
        ConstraintViolation<TestBean> violation = violations.iterator().next();

        String formattedMessage = ExceptionHelper.formatConstraintViolation(violation);

        // Corrected assertion based on actual library output
        assertThat(formattedMessage)
                .contains("ExceptionHelperTest$TestBean.name")
                .contains("must not be null")
                .contains("(got:null)");
    }

    @Data
    private static class TestBean {
        @NotNull
        private String name;
    }
}
