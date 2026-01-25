package io.hqwu.commons.util;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test cases for CoderUtil
 */
public class CoderUtilTest {

    // ==================== Test cases for encryptMD5(byte[] data) ====================

    @Test
    public void testEncryptMD5Basic() throws Exception {
        // Test basic MD5 encryption
        String input = "Hello World";
        byte[] data = input.getBytes(StandardCharsets.UTF_8);

        byte[] result = CoderUtil.encryptMD5(data);

        assertNotNull(result, "MD5 result should not be null");
        assertEquals(16, result.length, "MD5 hash should be 16 bytes");
    }

    @Test
    public void testEncryptMD5EmptyString() throws Exception {
        // Test MD5 with empty string
        byte[] data = "".getBytes(StandardCharsets.UTF_8);

        byte[] result = CoderUtil.encryptMD5(data);

        assertNotNull(result);
        assertEquals(16, result.length);

        // MD5 of empty string is d41d8cd98f00b204e9800998ecf8427e
        String hex = bytesToHex(result);
        assertEquals("d41d8cd98f00b204e9800998ecf8427e", hex);
    }

    @Test
    public void testEncryptMD5EmptyArray() throws Exception {
        // Test MD5 with empty byte array
        byte[] data = new byte[0];

        byte[] result = CoderUtil.encryptMD5(data);

        assertNotNull(result);
        assertEquals(16, result.length);

        // Should be same as empty string
        String hex = bytesToHex(result);
        assertEquals("d41d8cd98f00b204e9800998ecf8427e", hex);
    }

    @Test
    public void testEncryptMD5KnownValue() throws Exception {
        // Test MD5 with known value
        String input = "test";
        byte[] data = input.getBytes(StandardCharsets.UTF_8);

        byte[] result = CoderUtil.encryptMD5(data);

        // MD5 of "test" is 098f6bcd4621d373cade4e832627b4f6
        String hex = bytesToHex(result);
        assertEquals("098f6bcd4621d373cade4e832627b4f6", hex);
    }

    @Test
    public void testEncryptMD5ChineseCharacters() throws Exception {
        // Test MD5 with Chinese characters
        String input = "中文测试";
        byte[] data = input.getBytes(StandardCharsets.UTF_8);

        byte[] result = CoderUtil.encryptMD5(data);

        assertNotNull(result);
        assertEquals(16, result.length);

        // Verify the hash is consistent
        byte[] result2 = CoderUtil.encryptMD5(data);
        assertArrayEquals(result, result2, "MD5 should be deterministic");
    }

    @Test
    public void testEncryptMD5LongData() throws Exception {
        // Test MD5 with long data
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            sb.append("Long test data ");
        }
        byte[] data = sb.toString().getBytes(StandardCharsets.UTF_8);

        byte[] result = CoderUtil.encryptMD5(data);

        assertNotNull(result);
        assertEquals(16, result.length);
    }

    @Test
    public void testEncryptMD5BinaryData() throws Exception {
        // Test MD5 with binary data
        byte[] data = new byte[256];
        for (int i = 0; i < 256; i++) {
            data[i] = (byte) i;
        }

        byte[] result = CoderUtil.encryptMD5(data);

        assertNotNull(result);
        assertEquals(16, result.length);
    }

    @Test
    public void testEncryptMD5Consistency() throws Exception {
        // Test that same input produces same output
        String input = "consistency test";
        byte[] data = input.getBytes(StandardCharsets.UTF_8);

        byte[] result1 = CoderUtil.encryptMD5(data);
        byte[] result2 = CoderUtil.encryptMD5(data);
        byte[] result3 = CoderUtil.encryptMD5(data);

        assertArrayEquals(result1, result2, "MD5 should be consistent");
        assertArrayEquals(result2, result3, "MD5 should be consistent");
    }

    @Test
    public void testEncryptMD5DifferentInputs() throws Exception {
        // Test that different inputs produce different outputs
        byte[] data1 = "test1".getBytes(StandardCharsets.UTF_8);
        byte[] data2 = "test2".getBytes(StandardCharsets.UTF_8);

        byte[] result1 = CoderUtil.encryptMD5(data1);
        byte[] result2 = CoderUtil.encryptMD5(data2);

        assertFalse(bytesToHex(result1).equals(bytesToHex(result2)),
                    "Different inputs should produce different MD5 hashes");
    }

    @Test
    public void testEncryptMD5SpecialCharacters() throws Exception {
        // Test MD5 with special characters
        String input = "!@#$%^&*()_+-={}[]|\\:;\"'<>,.?/~`";
        byte[] data = input.getBytes(StandardCharsets.UTF_8);

        byte[] result = CoderUtil.encryptMD5(data);

        assertNotNull(result);
        assertEquals(16, result.length);
    }

    @Test
    public void testEncryptMD5MatchesStandardImplementation() throws Exception {
        // Verify our implementation matches standard MessageDigest
        String input = "verification test";
        byte[] data = input.getBytes(StandardCharsets.UTF_8);

        byte[] result = CoderUtil.encryptMD5(data);

        // Compare with direct MessageDigest call
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        md5.update(data);
        byte[] expected = md5.digest();

        assertArrayEquals(expected, result, "Should match standard MD5 implementation");
    }

    // ==================== Test cases for encryptSHA(byte[] data) ====================

    @Test
    public void testEncryptSHABasic() throws Exception {
        // Test basic SHA encryption
        String input = "Hello World";
        byte[] data = input.getBytes(StandardCharsets.UTF_8);

        byte[] result = CoderUtil.encryptSHA(data);

        assertNotNull(result, "SHA result should not be null");
        assertEquals(20, result.length, "SHA-1 hash should be 20 bytes");
    }

    @Test
    public void testEncryptSHAEmptyString() throws Exception {
        // Test SHA with empty string
        byte[] data = "".getBytes(StandardCharsets.UTF_8);

        byte[] result = CoderUtil.encryptSHA(data);

        assertNotNull(result);
        assertEquals(20, result.length);

        // SHA-1 of empty string is da39a3ee5e6b4b0d3255bfef95601890afd80709
        String hex = bytesToHex(result);
        assertEquals("da39a3ee5e6b4b0d3255bfef95601890afd80709", hex);
    }

    @Test
    public void testEncryptSHAEmptyArray() throws Exception {
        // Test SHA with empty byte array
        byte[] data = new byte[0];

        byte[] result = CoderUtil.encryptSHA(data);

        assertNotNull(result);
        assertEquals(20, result.length);

        // Should be same as empty string
        String hex = bytesToHex(result);
        assertEquals("da39a3ee5e6b4b0d3255bfef95601890afd80709", hex);
    }

    @Test
    public void testEncryptSHAKnownValue() throws Exception {
        // Test SHA with known value
        String input = "test";
        byte[] data = input.getBytes(StandardCharsets.UTF_8);

        byte[] result = CoderUtil.encryptSHA(data);

        // SHA-1 of "test" is a94a8fe5ccb19ba61c4c0873d391e987982fbbd3
        String hex = bytesToHex(result);
        assertEquals("a94a8fe5ccb19ba61c4c0873d391e987982fbbd3", hex);
    }

    @Test
    public void testEncryptSHAChineseCharacters() throws Exception {
        // Test SHA with Chinese characters
        String input = "中文测试";
        byte[] data = input.getBytes(StandardCharsets.UTF_8);

        byte[] result = CoderUtil.encryptSHA(data);

        assertNotNull(result);
        assertEquals(20, result.length);

        // Verify the hash is consistent
        byte[] result2 = CoderUtil.encryptSHA(data);
        assertArrayEquals(result, result2, "SHA should be deterministic");
    }

    @Test
    public void testEncryptSHALongData() throws Exception {
        // Test SHA with long data
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            sb.append("Long test data ");
        }
        byte[] data = sb.toString().getBytes(StandardCharsets.UTF_8);

        byte[] result = CoderUtil.encryptSHA(data);

        assertNotNull(result);
        assertEquals(20, result.length);
    }

    @Test
    public void testEncryptSHABinaryData() throws Exception {
        // Test SHA with binary data
        byte[] data = new byte[256];
        for (int i = 0; i < 256; i++) {
            data[i] = (byte) i;
        }

        byte[] result = CoderUtil.encryptSHA(data);

        assertNotNull(result);
        assertEquals(20, result.length);
    }

    @Test
    public void testEncryptSHAConsistency() throws Exception {
        // Test that same input produces same output
        String input = "consistency test";
        byte[] data = input.getBytes(StandardCharsets.UTF_8);

        byte[] result1 = CoderUtil.encryptSHA(data);
        byte[] result2 = CoderUtil.encryptSHA(data);
        byte[] result3 = CoderUtil.encryptSHA(data);

        assertArrayEquals(result1, result2, "SHA should be consistent");
        assertArrayEquals(result2, result3, "SHA should be consistent");
    }

    @Test
    public void testEncryptSHADifferentInputs() throws Exception {
        // Test that different inputs produce different outputs
        byte[] data1 = "test1".getBytes(StandardCharsets.UTF_8);
        byte[] data2 = "test2".getBytes(StandardCharsets.UTF_8);

        byte[] result1 = CoderUtil.encryptSHA(data1);
        byte[] result2 = CoderUtil.encryptSHA(data2);

        assertFalse(bytesToHex(result1).equals(bytesToHex(result2)),
                    "Different inputs should produce different SHA hashes");
    }

    @Test
    public void testEncryptSHASpecialCharacters() throws Exception {
        // Test SHA with special characters
        String input = "!@#$%^&*()_+-={}[]|\\:;\"'<>,.?/~`";
        byte[] data = input.getBytes(StandardCharsets.UTF_8);

        byte[] result = CoderUtil.encryptSHA(data);

        assertNotNull(result);
        assertEquals(20, result.length);
    }

    @Test
    public void testEncryptSHAMatchesStandardImplementation() throws Exception {
        // Verify our implementation matches standard MessageDigest
        String input = "verification test";
        byte[] data = input.getBytes(StandardCharsets.UTF_8);

        byte[] result = CoderUtil.encryptSHA(data);

        // Compare with direct MessageDigest call
        MessageDigest sha = MessageDigest.getInstance("SHA");
        sha.update(data);
        byte[] expected = sha.digest();

        assertArrayEquals(expected, result, "Should match standard SHA implementation");
    }

    // ==================== Comparison tests ====================

    @Test
    public void testMD5VsSHAOutputLength() throws Exception {
        // Verify MD5 and SHA produce different length outputs
        String input = "compare test";
        byte[] data = input.getBytes(StandardCharsets.UTF_8);

        byte[] md5Result = CoderUtil.encryptMD5(data);
        byte[] shaResult = CoderUtil.encryptSHA(data);

        assertEquals(16, md5Result.length, "MD5 produces 16 bytes");
        assertEquals(20, shaResult.length, "SHA-1 produces 20 bytes");
        assertNotEquals(md5Result.length, shaResult.length);
    }

    @Test
    public void testMD5VsSHADifferentHashes() throws Exception {
        // Verify MD5 and SHA produce different hashes for same input
        String input = "same input test";
        byte[] data = input.getBytes(StandardCharsets.UTF_8);

        byte[] md5Result = CoderUtil.encryptMD5(data);
        byte[] shaResult = CoderUtil.encryptSHA(data);

        // Since they're different lengths, they can't be equal
        assertNotEquals(bytesToHex(md5Result), bytesToHex(shaResult));
    }

    // ==================== Edge case tests ====================

    @Test
    public void testEncryptMD5SingleByte() throws Exception {
        // Test with single byte
        byte[] data = new byte[]{42};

        byte[] result = CoderUtil.encryptMD5(data);

        assertNotNull(result);
        assertEquals(16, result.length);
    }

    @Test
    public void testEncryptSHASingleByte() throws Exception {
        // Test with single byte
        byte[] data = new byte[]{42};

        byte[] result = CoderUtil.encryptSHA(data);

        assertNotNull(result);
        assertEquals(20, result.length);
    }

    @Test
    public void testEncryptMD5NullBytes() throws Exception {
        // Test with null bytes
        byte[] data = new byte[]{0, 0, 0, 0};

        byte[] result = CoderUtil.encryptMD5(data);

        assertNotNull(result);
        assertEquals(16, result.length);
    }

    @Test
    public void testEncryptSHANullBytes() throws Exception {
        // Test with null bytes
        byte[] data = new byte[]{0, 0, 0, 0};

        byte[] result = CoderUtil.encryptSHA(data);

        assertNotNull(result);
        assertEquals(20, result.length);
    }

    // ==================== Helper methods ====================

    /**
     * Convert byte array to hexadecimal string
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
