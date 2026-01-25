package io.hqwu.commons.utils;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Spliterator;
import java.util.Spliterators;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class ConstraintViolationUtilTest {

    @Mock
    private ConstraintViolation<Object> violation;

    @Mock
    private Path path;

    @Mock
    private Path.Node node;

    // Inner class for testing reflection and annotations
    static class TestBean {
        private String username;

        @JsonProperty("email_address")
        private String email;

        @JsonProperty("  ") // Empty value, should fallback to field name
        private String emptyJsonProp;
    }

    /**
     * Helper to setup the mock violation object.
     * Uses lenient() because not all tests consume all mocked methods.
     */
    private void setupViolation(Object leafBean, String nodeName, Object invalidValue, String message) {
        lenient().when(violation.getLeafBean()).thenReturn(leafBean);
        lenient().when(violation.getInvalidValue()).thenReturn(invalidValue);
        lenient().when(violation.getMessage()).thenReturn(message);

        lenient().when(node.getName()).thenReturn(nodeName);
        
        // Mock the path stream to return our single node
        lenient().when(path.spliterator()).thenReturn(
                Spliterators.spliterator(Collections.singletonList(node), Spliterator.ORDERED)
        );
        lenient().when(violation.getPropertyPath()).thenReturn(path);
    }

    @Test
    @DisplayName("Should format simple field violation correctly")
    void testToString_NormalField() {
        TestBean bean = new TestBean();
        setupViolation(bean, "username", "bad_user", "must be valid");

        String result = ConstraintViolationUtil.toString(violation, false);

        assertEquals("`username` must be valid (got:bad_user)", result);
    }

    @Test
    @DisplayName("Should use @JsonProperty value if present")
    void testToString_WithJsonProperty() {
        TestBean bean = new TestBean();
        setupViolation(bean, "email", "bad@email", "invalid format");

        String result = ConstraintViolationUtil.toString(violation, false);

        assertEquals("`email_address` invalid format (got:bad@email)", result);
    }

    @Test
    @DisplayName("Should fallback to field name if @JsonProperty value is blank")
    void testToString_WithEmptyJsonProperty() {
        TestBean bean = new TestBean();
        setupViolation(bean, "emptyJsonProp", "val", "error");

        String result = ConstraintViolationUtil.toString(violation, false);

        assertEquals("`emptyJsonProp` error (got:val)", result);
    }

    @Test
    @DisplayName("Should handle field not found (NoSuchFieldException) gracefully")
    void testToString_FieldNotFound() {
        TestBean bean = new TestBean();
        // "password" field does not exist in TestBean
        setupViolation(bean, "password", "123", "too weak");

        String result = ConstraintViolationUtil.toString(violation, false);

        assertEquals("`password` too weak (got:123)", result);
    }

    @Test
    @DisplayName("Should handle class-level violations (empty node name)")
    void testToString_ClassLevel() {
        TestBean bean = new TestBean();
        // Assuming StringUtil.isNotBlank(null) returns false
        setupViolation(bean, null, "obj", "global error");

        String result = ConstraintViolationUtil.toString(violation, false);

        assertEquals("`TestBean` global error (got:obj)", result);
    }

    @Test
    @DisplayName("Should include full class name in debug mode")
    void testToString_DebugMode() {
        TestBean bean = new TestBean();
        setupViolation(bean, "username", "val", "error");

        String result = ConstraintViolationUtil.toString(violation, true);

        String expectedField = TestBean.class.getName() + ".username";
        assertEquals("`" + expectedField + "` error (got:val)", result);
    }

    @Test
    @DisplayName("Should format Object array values correctly")
    void testToString_ArrayValue() {
        TestBean bean = new TestBean();
        String[] invalidArr = {"a", "b"};
        setupViolation(bean, "username", invalidArr, "error");

        String result = ConstraintViolationUtil.toString(violation, false);

        assertEquals("`username` error (got:{a,b})", result);
    }

    @Test
    @DisplayName("Should handle class-level violations in debug mode (覆盖第44行 debug=true 分支)")
    void testToString_ClassLevelDebugMode() {
        TestBean bean = new TestBean();
        // Empty/null field name triggers class-level violation logic
        setupViolation(bean, null, "obj", "class validation error");

        String result = ConstraintViolationUtil.toString(violation, true);

        // In debug mode with empty field name, should use full class name
        assertEquals("`" + TestBean.class.getName() + "` class validation error (got:obj)", result);
    }

    @Test
    @DisplayName("Should handle null invalidValue (覆盖第47行 invalidValue==null 分支)")
    void testToString_NullInvalidValue() {
        TestBean bean = new TestBean();
        setupViolation(bean, "username", null, "cannot be null");

        String result = ConstraintViolationUtil.toString(violation, false);

        assertEquals("`username` cannot be null (got:null)", result);
    }
}