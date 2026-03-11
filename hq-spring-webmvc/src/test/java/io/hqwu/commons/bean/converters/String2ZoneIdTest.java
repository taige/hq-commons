package io.hqwu.commons.bean.converters;

import org.junit.jupiter.api.Test;

import java.time.ZoneId;
import java.time.zone.ZoneRulesException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Test class for String2ZoneId
 *
 * @author taige
 * @since 2026-01-28
 */
class String2ZoneIdTest {

    private final String2ZoneId converter = new String2ZoneId();

    @Test
    void testValueOf_ValidZoneId() {
        // Test with valid time zone ID
        ZoneId result = converter.valueOf("Asia/Shanghai", null, null, null, null);
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("Asia/Shanghai");
    }

    @Test
    void testValueOf_NullInput() {
        // Test null input returns null
        ZoneId result = converter.valueOf(null, null, null, null, null);
        assertThat(result).isNull();
    }

    @Test
    void testValueOf_InvalidZoneId() {
        // Test invalid zone ID throws ZoneRulesException
        assertThatThrownBy(() -> converter.valueOf("Invalid/Zone", null, null, null, null))
                .isInstanceOf(ZoneRulesException.class);
    }

    @Test
    void testValueOf_Caching() {
        // Test that same zone ID returns cached instance
        ZoneId first = converter.valueOf("Asia/Tokyo", null, null, null, null);
        ZoneId second = converter.valueOf("Asia/Tokyo", null, null, null, null);

        assertThat(first).isSameAs(second);
    }
}
