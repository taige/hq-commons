package io.hqwu.commons.servlet.support;

import jakarta.servlet.ReadListener;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link CachedBodyServletInputStream} 的单元测试类。
 * <p>
 * 测试覆盖：
 * <ul>
 *   <li>基本读取操作：单字节读取、流结束检测</li>
 *   <li>流状态检测：isFinished、isReady</li>
 *   <li>重复读取能力：验证可重复读取特性</li>
 *   <li>边界条件：空数组、大数据量</li>
 *   <li>异常处理：不支持的操作</li>
 * </ul>
 * </p>
 *
 * @author taige (Wu, Hongqiang)
 */
@DisplayName("CachedBodyServletInputStream 单元测试")
class CachedBodyServletInputStreamTest {

    // ==================== 基本读取操作测试 ====================

    @Test
    @DisplayName("测试read方法 - 读取单个字节")
    void testReadSingleByte() throws IOException {
        byte[] content = "Hello".getBytes(StandardCharsets.UTF_8);
        CachedBodyServletInputStream inputStream = new CachedBodyServletInputStream(content);

        assertEquals('H', inputStream.read());
        assertEquals('e', inputStream.read());
        assertEquals('l', inputStream.read());
        assertEquals('l', inputStream.read());
        assertEquals('o', inputStream.read());
    }

    @Test
    @DisplayName("测试read方法 - 读取到流末尾")
    void testReadUntilEnd() throws IOException {
        byte[] content = "AB".getBytes(StandardCharsets.UTF_8);
        CachedBodyServletInputStream inputStream = new CachedBodyServletInputStream(content);

        assertEquals('A', inputStream.read());
        assertEquals('B', inputStream.read());
        assertEquals(-1, inputStream.read()); // 流结束返回 -1
        assertEquals(-1, inputStream.read()); // 继续读取仍然返回 -1
    }

    @Test
    @DisplayName("测试read方法 - 读取完整内容")
    void testReadFullContent() throws IOException {
        String testContent = "This is a test message";
        byte[] content = testContent.getBytes(StandardCharsets.UTF_8);
        CachedBodyServletInputStream inputStream = new CachedBodyServletInputStream(content);

        for (int i = 0; i < content.length; i++) {
            assertEquals(content[i], inputStream.read());
        }
        assertEquals(-1, inputStream.read()); // 确认流已结束
    }


    // ==================== 流状态检测测试 ====================

    @Test
    @DisplayName("测试isFinished方法 - 初始状态未完成")
    void testIsFinishedInitialState() {
        byte[] content = "Test".getBytes(StandardCharsets.UTF_8);
        CachedBodyServletInputStream inputStream = new CachedBodyServletInputStream(content);

        assertFalse(inputStream.isFinished());
    }

    @Test
    @DisplayName("测试isFinished方法 - 读取后检测")
    void testIsFinishedAfterPartialRead() throws IOException {
        byte[] content = "Test".getBytes(StandardCharsets.UTF_8);
        CachedBodyServletInputStream inputStream = new CachedBodyServletInputStream(content);

        inputStream.read(); // 读取一个字节
        assertFalse(inputStream.isFinished()); // 还有数据未读取

        // 读取剩余所有字节
        while (inputStream.read() != -1) {
            // 继续读取
        }
        assertTrue(inputStream.isFinished()); // 所有数据已读取完毕
    }

    @Test
    @DisplayName("测试isFinished方法 - 空流")
    void testIsFinishedEmptyStream() {
        byte[] content = new byte[0];
        CachedBodyServletInputStream inputStream = new CachedBodyServletInputStream(content);

        assertTrue(inputStream.isFinished());
    }

    @Test
    @DisplayName("测试isReady方法 - 始终返回true")
    void testIsReady() throws IOException {
        byte[] content = "Test".getBytes(StandardCharsets.UTF_8);
        CachedBodyServletInputStream inputStream = new CachedBodyServletInputStream(content);

        assertTrue(inputStream.isReady()); // 初始状态
        inputStream.read();
        assertTrue(inputStream.isReady()); // 读取后
        while (inputStream.read() != -1) {
            // 继续读取
        }
        assertTrue(inputStream.isReady()); // 读取完毕后
    }

    // ==================== 边界条件测试 ====================

    @Test
    @DisplayName("测试边界条件 - 空字节数组")
    void testEmptyByteArray() throws IOException {
        byte[] content = new byte[0];
        CachedBodyServletInputStream inputStream = new CachedBodyServletInputStream(content);

        assertEquals(-1, inputStream.read());
        assertTrue(inputStream.isFinished());
        assertTrue(inputStream.isReady());
    }

    @Test
    @DisplayName("测试边界条件 - 单字节数组")
    void testSingleByteArray() throws IOException {
        byte[] content = new byte[]{65}; // 'A'
        CachedBodyServletInputStream inputStream = new CachedBodyServletInputStream(content);

        assertFalse(inputStream.isFinished());
        assertEquals(65, inputStream.read());
        assertTrue(inputStream.isFinished());
        assertEquals(-1, inputStream.read());
    }

    @Test
    @DisplayName("测试边界条件 - 大数据量")
    void testLargeDataVolume() throws IOException {
        // 创建 1KB 的数据
        byte[] content = new byte[1024];
        for (int i = 0; i < content.length; i++) {
            content[i] = (byte) (i % 256);
        }

        CachedBodyServletInputStream inputStream = new CachedBodyServletInputStream(content);

        assertFalse(inputStream.isFinished());

        // 验证能够读取所有数据
        int count = 0;
        int b;
        while ((b = inputStream.read()) != -1) {
            count++;
        }

        assertEquals(content.length, count);
        assertTrue(inputStream.isFinished());
    }

    @Test
    @DisplayName("测试边界条件 - 包含特殊字符的内容")
    void testSpecialCharacters() throws IOException {
        String testContent = "特殊字符测试: \n\r\t\0 !@#$%^&*()";
        byte[] content = testContent.getBytes(StandardCharsets.UTF_8);
        CachedBodyServletInputStream inputStream = new CachedBodyServletInputStream(content);

        byte[] buffer = new byte[content.length];
        for (int i = 0; i < content.length; i++) {
            int b = inputStream.read();
            if (b == -1) {
                fail("Stream ended prematurely");
            }
            buffer[i] = (byte) b;
        }

        assertArrayEquals(content, buffer);
        assertTrue(inputStream.isFinished());
    }

    @Test
    @DisplayName("测试边界条件 - UTF-8编码的多字节字符")
    void testMultiByteCharacters() throws IOException {
        String testContent = "你好世界 🌍 Hello World";
        byte[] content = testContent.getBytes(StandardCharsets.UTF_8);
        CachedBodyServletInputStream inputStream = new CachedBodyServletInputStream(content);

        byte[] buffer = new byte[content.length];
        int offset = 0;
        int b;

        while ((b = inputStream.read()) != -1) {
            buffer[offset++] = (byte) b;
        }

        assertEquals(content.length, offset);
        assertEquals(testContent, new String(buffer, StandardCharsets.UTF_8));
    }

    // ==================== 异常处理测试 ====================

    @Test
    @DisplayName("测试setReadListener方法 - 抛出UnsupportedOperationException")
    void testSetReadListenerThrowsException() {
        byte[] content = "Test".getBytes(StandardCharsets.UTF_8);
        CachedBodyServletInputStream inputStream = new CachedBodyServletInputStream(content);

        ReadListener mockListener = new ReadListener() {
            @Override
            public void onDataAvailable() {
            }

            @Override
            public void onAllDataRead() {
            }

            @Override
            public void onError(Throwable t) {
            }
        };

        assertThrows(UnsupportedOperationException.class, () -> {
            inputStream.setReadListener(mockListener);
        });
    }

    @Test
    @DisplayName("测试setReadListener方法 - null参数也抛出异常")
    void testSetReadListenerWithNullThrowsException() {
        byte[] content = "Test".getBytes(StandardCharsets.UTF_8);
        CachedBodyServletInputStream inputStream = new CachedBodyServletInputStream(content);

        assertThrows(UnsupportedOperationException.class, () -> {
            inputStream.setReadListener(null);
        });
    }

    // ==================== 可重复读取测试 ====================

    @Test
    @DisplayName("测试可重复读取 - 通过创建新实例实现")
    void testRepeatableRead() throws IOException {
        byte[] content = "Repeatable Content".getBytes(StandardCharsets.UTF_8);

        // 第一次读取
        CachedBodyServletInputStream inputStream1 = new CachedBodyServletInputStream(content);
        byte[] buffer1 = new byte[content.length];
        int read1 = inputStream1.read(buffer1);

        // 第二次读取（使用相同的缓存字节数组创建新实例）
        CachedBodyServletInputStream inputStream2 = new CachedBodyServletInputStream(content);
        byte[] buffer2 = new byte[content.length];
        int read2 = inputStream2.read(buffer2);

        assertEquals(read1, read2);
        assertArrayEquals(buffer1, buffer2);
    }

    // ==================== available() 方法间接测试 ====================

    @Test
    @DisplayName("测试available方法的间接影响 - 通过isFinished")
    void testAvailableIndirectly() throws IOException {
        byte[] content = "Test".getBytes(StandardCharsets.UTF_8);
        CachedBodyServletInputStream inputStream = new CachedBodyServletInputStream(content);

        // 初始状态：有数据可读
        assertFalse(inputStream.isFinished());

        // 读取一些数据
        inputStream.read();
        inputStream.read();
        assertFalse(inputStream.isFinished());

        // 读取完所有数据
        inputStream.read();
        inputStream.read();
        assertTrue(inputStream.isFinished());
    }

    // ==================== 集成场景测试 ====================

    @Test
    @DisplayName("集成测试 - 模拟HTTP请求体读取场景")
    void testHttpRequestBodyScenario() throws IOException {
        String jsonBody = "{\"name\":\"test\",\"value\":123,\"enabled\":true}";
        byte[] content = jsonBody.getBytes(StandardCharsets.UTF_8);
        CachedBodyServletInputStream inputStream = new CachedBodyServletInputStream(content);

        // 验证流状态
        assertTrue(inputStream.isReady());
        assertFalse(inputStream.isFinished());

        // 读取整个请求体
        byte[] buffer = new byte[content.length];
        for (int i = 0; i < content.length; i++) {
            int b = inputStream.read();
            if (b == -1) {
                fail("Stream ended prematurely at position " + i);
            }
            buffer[i] = (byte) b;
        }

        assertEquals(jsonBody, new String(buffer, StandardCharsets.UTF_8));
        assertTrue(inputStream.isFinished());
    }

    @Test
    @DisplayName("集成测试 - 分块读取场景")
    void testChunkedReadScenario() throws IOException {
        String testContent = "This is a longer test content that will be read in chunks";
        byte[] content = testContent.getBytes(StandardCharsets.UTF_8);
        CachedBodyServletInputStream inputStream = new CachedBodyServletInputStream(content);

        StringBuilder result = new StringBuilder();
        int b;

        while ((b = inputStream.read()) != -1) {
            result.append((char) b);
        }

        assertEquals(testContent, result.toString());
        assertTrue(inputStream.isFinished());
    }

    @Test
    @DisplayName("测试isFinished方法 - IOException异常处理 (覆盖第56行)")
    void testIsFinishedWithIOException() throws Exception {
        byte[] content = "Test".getBytes(StandardCharsets.UTF_8);
        CachedBodyServletInputStream inputStream = new CachedBodyServletInputStream(content);

        // 使用反射注入一个会抛出IOException的InputStream
        InputStream faultyStream = new InputStream() {
            @Override
            public int read() throws IOException {
                return -1;
            }

            @Override
            public int available() throws IOException {
                throw new IOException("Simulated IOException for testing");
            }
        };

        // 使用反射替换内部的cachedBodyInputStream
        Field field = CachedBodyServletInputStream.class.getDeclaredField("cachedBodyInputStream");
        field.setAccessible(true);
        field.set(inputStream, faultyStream);

        // 当available()抛出IOException时，isFinished()应该返回true（第56行catch块后的返回）
        assertTrue(inputStream.isFinished(), "isFinished() should return true when IOException is caught");
    }
}
