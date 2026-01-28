package io.hqwu.commons.util;

import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Created with IntelliJ IDEA for hq-commons
 *
 * @author taige (Wu, Hongqiang)
 * Date: 2021-01-15
 * Time: 3:29 p.m.
 */
class Base62UtilTest {
    private static final Logger LOGGER = new Logger();

    @Test
    void test_encode_0() {
        LOGGER.debug(Base62Util.decode("100000"));  // 916132832
        LOGGER.debug(Base62Util.encode(Long.MAX_VALUE));  // 916132832
    }

    @Test
    void test_encode_1() {
        HashMap<Long, List<String>> map = new HashMap<>();

        for (int i = 0; ; i++) {
            String in = Base62Util.encode(i+1);
//            in = String.format("%05d", i+1);

//            int crc = 0xff & CRC8.calcCrc8(in.getBytes());
            long crc = Base62Util.crc32(in.getBytes());
            String b62_crc = Base62Util.encode(crc, 6);
            String enc = in + b62_crc;
            LOGGER.debug("原文：%s, CRC8：%s %s", in, b62_crc, enc);

            if (map.containsKey(crc)) {
//                LOGGER.debug(" === %d -> %s, %s", crc, in, map.get(crc));
                List<String> list = map.get(crc);
                list.add(in);
            } else {
                ArrayList<String> list = new ArrayList<>();
                list.add(in);
                map.put(crc, list);
            }
            if (map.size() >= 256) {
                LOGGER.debug("i = " + i);
                break;
            }
        }
        map.entrySet().stream().sorted(Comparator.comparingLong(Map.Entry::getKey)).forEach(entry -> {
            long crc = entry.getKey();
            List<String> list = entry.getValue();
            String hex=Long.toHexString(crc);
            String b62_crc = Base62Util.encode(crc, 8);
            LOGGER.debug("%d/%s/%s -> %s", crc, hex, b62_crc, list);
        });
    }

    @Test
    void test_encode_crc() {
        HashMap<Long, List<String>> map = new HashMap<>();

        for (int i = 0; i < 100000; i++) {
            long expect = i + 1;
            String in = Base62Util.encode(expect, true);
            LOGGER.debug(i + " = " + in);
            long num = Base62Util.decode(in, true);
            assertEquals(expect, num);
        }

    }

    @Test
    void test_decode_1() {
        String in = Base62Util.encode(100, true);
        LOGGER.debug("100 = " + in);
    }

    @Test
    void test_decode_2() {
        Base62Util.decode("1c1WWBm3", true);
    }

    // ==================== Additional tests for branch coverage ====================

    // Line 62: Test encode with appendCRC = true (return b62 + crc6)
    @Test
    void testEncodeWithAppendCRCTrue() {
        String encoded = Base62Util.encode(12345L, true);
        LOGGER.debug("Encoded with CRC: " + encoded);

        org.junit.jupiter.api.Assertions.assertNotNull(encoded);
        org.junit.jupiter.api.Assertions.assertTrue(encoded.length() > 6); // Should have CRC appended

        // Verify we can decode it back
        long decoded = Base62Util.decode(encoded, true);
        org.junit.jupiter.api.Assertions.assertEquals(12345L, decoded);
    }

    // Line 64: Test encode with appendCRC = false (else branch)
    @Test
    void testEncodeWithAppendCRCFalse() {
        String encoded = Base62Util.encode(12345L, false);
        LOGGER.debug("Encoded without CRC: " + encoded);

        org.junit.jupiter.api.Assertions.assertNotNull(encoded);
        // Should be the same as encode(12345L)
        org.junit.jupiter.api.Assertions.assertEquals(Base62Util.encode(12345L), encoded);
    }

    // Line 76: Test encode with negative number (throws IllegalArgumentException)
    @Test
    void testEncodeWithNegativeNumber() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Base62Util.encode(-1L, 4);
        });

        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Base62Util.encode(-100L, 4);
        });
    }

    // Line 100: Test decode with blank string
    @Test
    void testDecodeWithBlankString() {
        org.junit.jupiter.api.Assertions.assertThrows(NumberFormatException.class, () -> {
            Base62Util.decode("");
        });

        org.junit.jupiter.api.Assertions.assertThrows(NumberFormatException.class, () -> {
            Base62Util.decode(null);
        });

        org.junit.jupiter.api.Assertions.assertThrows(NumberFormatException.class, () -> {
            Base62Util.decode("   ");
        });
    }

    // Line 100: Test decode with string exceeding MAX_VALUE_LEN
    @Test
    void testDecodeWithTooLongString() {
        // MAX_VALUE_LEN is 11, create a string with 12 characters
        String tooLong = "AzL8n0Y58m7X"; // 12 chars

        org.junit.jupiter.api.Assertions.assertThrows(NumberFormatException.class, () -> {
            Base62Util.decode(tooLong);
        });
    }

    // Line 100: Test decode with string equal to MAX_VALUE_LEN but greater than MAX_VALUE
    @Test
    void testDecodeWithMaxLengthButGreaterValue() {
        // MAX_VALUE is "AzL8n0Y58m7" (11 chars)
        // Create a string with same length but greater value
        String greaterValue = "AzL8n0Y58m8"; // Last char '8' > '7'

        org.junit.jupiter.api.Assertions.assertThrows(NumberFormatException.class, () -> {
            Base62Util.decode(greaterValue);
        });

        // Test with "ZZZZZZZZZZZ" which is definitely greater
        org.junit.jupiter.api.Assertions.assertThrows(NumberFormatException.class, () -> {
            Base62Util.decode("ZZZZZZZZZZZ");
        });
    }

    // Line 100: Test decode with valid MAX_VALUE
    @Test
    void testDecodeWithMaxValue() {
        // MAX_VALUE should be decodable
        long result = Base62Util.decode(Base62Util.MAX_VALUE);
        LOGGER.debug("MAX_VALUE decoded: " + result);
        org.junit.jupiter.api.Assertions.assertTrue(result > 0);
    }

    // Line 108: Test decode with invalid character (not in Base62 table)
    @Test
    void testDecodeWithInvalidCharacter() {
        // Characters not in "0-9A-Za-z"
        org.junit.jupiter.api.Assertions.assertThrows(NumberFormatException.class, () -> {
            Base62Util.decode("123#456");
        });

        org.junit.jupiter.api.Assertions.assertThrows(NumberFormatException.class, () -> {
            Base62Util.decode("ABC@DEF");
        });

        org.junit.jupiter.api.Assertions.assertThrows(NumberFormatException.class, () -> {
            Base62Util.decode("test-value");
        });

        org.junit.jupiter.api.Assertions.assertThrows(NumberFormatException.class, () -> {
            Base62Util.decode("hello world");
        });

        // Special characters
        org.junit.jupiter.api.Assertions.assertThrows(NumberFormatException.class, () -> {
            Base62Util.decode("!@#$%");
        });
    }

    // Line 119: Test decode with checkCRC=true but string too short (< 7 chars)
    @Test
    void testDecodeWithCRCButTooShort() {
        // CRC_LEN is 6, so minimum length should be 7 (1 payload + 6 CRC)
        org.junit.jupiter.api.Assertions.assertThrows(NumberFormatException.class, () -> {
            Base62Util.decode("123456", true); // Only 6 chars
        });

        org.junit.jupiter.api.Assertions.assertThrows(NumberFormatException.class, () -> {
            Base62Util.decode("12345", true); // Only 5 chars
        });

        org.junit.jupiter.api.Assertions.assertThrows(NumberFormatException.class, () -> {
            Base62Util.decode("", true); // Empty string
        });
    }

    // Line 127: Test decode with checkCRC=true but CRC mismatch
    @Test
    void testDecodeWithCRCMismatch() {
        // Create a valid encoded string with CRC
        String validEncoded = Base62Util.encode(999L, true);
        LOGGER.debug("Valid encoded: " + validEncoded);

        // Tamper with the CRC part (last 6 characters)
        String tamperedCRC = validEncoded.substring(0, validEncoded.length() - 6) + "AAAAAA";

        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Base62Util.decode(tamperedCRC, true);
        });
    }

    // Line 127: Test decode with checkCRC=true and valid CRC
    @Test
    void testDecodeWithValidCRC() {
        long original = 88888L;
        String encoded = Base62Util.encode(original, true);
        LOGGER.debug("Encoded with valid CRC: " + encoded);

        long decoded = Base62Util.decode(encoded, true);
        org.junit.jupiter.api.Assertions.assertEquals(original, decoded);
    }

    // Line 130: Test decode with checkCRC=false (else branch)
    @Test
    void testDecodeWithCheckCRCFalse() {
        // Encode without CRC
        String encoded = Base62Util.encode(54321L);
        LOGGER.debug("Encoded without CRC: " + encoded);

        // Decode with checkCRC=false
        long decoded = Base62Util.decode(encoded, false);
        org.junit.jupiter.api.Assertions.assertEquals(54321L, decoded);

        // Should be same as calling decode(String)
        long decoded2 = Base62Util.decode(encoded);
        org.junit.jupiter.api.Assertions.assertEquals(decoded, decoded2);
    }

    // Additional edge case tests
    @Test
    void testEncodeDecodeZero() {
        String encoded = Base62Util.encode(0L);
        LOGGER.debug("Encoded 0: " + encoded);

        long decoded = Base62Util.decode(encoded);
        org.junit.jupiter.api.Assertions.assertEquals(0L, decoded);
    }

    @Test
    void testEncodeDecodeSmallNumbers() {
        for (long i = 1; i <= 100; i++) {
            String encoded = Base62Util.encode(i);
            long decoded = Base62Util.decode(encoded);
            org.junit.jupiter.api.Assertions.assertEquals(i, decoded, "Failed for number: " + i);
        }
    }

    @Test
    void testEncodeDecodeLargeNumbers() {
        long[] testValues = {
            Long.MAX_VALUE / 2,
            1000000000L,
            9999999999L,
            123456789012345L
        };

        for (long value : testValues) {
            String encoded = Base62Util.encode(value);
            long decoded = Base62Util.decode(encoded);
            org.junit.jupiter.api.Assertions.assertEquals(value, decoded, "Failed for number: " + value);
        }
    }

    @Test
    void testEncodeWithMinLength() {
        String encoded = Base62Util.encode(5L, 8);
        LOGGER.debug("Encoded 5 with minLength 8: " + encoded);

        org.junit.jupiter.api.Assertions.assertEquals(8, encoded.length());
        org.junit.jupiter.api.Assertions.assertTrue(encoded.startsWith("0"));

        long decoded = Base62Util.decode(encoded);
        org.junit.jupiter.api.Assertions.assertEquals(5L, decoded);
    }

    @Test
    void testCRCConsistency() {
        // Test that the same number always produces the same CRC
        for (int i = 0; i < 10; i++) {
            String encoded1 = Base62Util.encode(12345L, true);
            String encoded2 = Base62Util.encode(12345L, true);
            org.junit.jupiter.api.Assertions.assertEquals(encoded1, encoded2, "CRC encoding should be consistent");
        }
    }
}