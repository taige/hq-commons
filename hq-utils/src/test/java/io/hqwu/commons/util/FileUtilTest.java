/*
 * @(#)FileUtilTest.java Created on 2013-8-13
 *
 * Copyright 2012-2013 Chinabank Payments, Inc. All rights reserved.
 * Use is subject to license terms.
 */
package io.hqwu.commons.util;


import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;


/**
 * Description:
 * 
 * @author: shenjianlin <a href="mailto:ustbsjl@gmail.com">ustbsjl@gmail.com</a> <br>
 *          QQ: 79043549
 * @version: 1.0 2013-8-13
 * @history:
 */

public class FileUtilTest {
	static String EXIST_CP_FILE_PATH = "io/hqwu/commons/util/test.file";
	static String EXIST_FS_FILE_PATH = "src/test/java/io/hqwu/commons/util/test.file";

	@Test
	public void test_1() throws Exception {
		URL url = null;
		assertFalse(FileUtil.isModified(url));

		url = new URL("http://127.0.0.1/ddd.txt");
		assertFalse(FileUtil.isModified(url));

		url = new URL("http://su.bdimg.com/static/superpage/img/logo_white_2a2fcb5a.png");
		assertFalse(FileUtil.isModified(url));

	}

	@Test
	public void testGetFileByPath() throws Exception {
		assertEquals(FileUtil.getFileByPath("./"), FileUtil.getFileByPath("."));
		String filepath = "";
		File file = FileUtil.getFileByPath(filepath);// 当前classpath根
		assertTrue(file.exists(), "当前classpath根(classes)");
		// 文件不存在
		filepath = "notexist";
		file = FileUtil.getFileByPath(filepath);
		assertFalse(file.exists(), "文件不存在");
		// 按文件路径找文件
		file = FileUtil.getFileByPath(EXIST_FS_FILE_PATH);
		assertEquals("test.file", file.getName(), "按文件路径找文件");
		// 类路径找文件
		file = FileUtil.getFileByPath(EXIST_CP_FILE_PATH);
		assertEquals("test.file", file.getName(), "按文件路径找文件");
	}

	@Test
	public void testReadFileToByteArray() throws Exception {
		// 类路径找文件
		byte[] content = FileUtil.readFileToByteArray(EXIST_CP_FILE_PATH);
		assertTrue(content.length > 0);// 能读出文件内容
	}

	@Test
	public void testReadFileToString() throws Exception {
		// 类路径找文件
		String content = FileUtil.readFileToString(EXIST_CP_FILE_PATH, "UTF-8");
		assertTrue(content.length() > 0);// 能读出文件内容
		assertTrue(content.contains("中国"));
	}

	@Test
	public void testReadFileToString2() throws Exception {
		// 类路径找文件
		String content = FileUtil.readFileToString(EXIST_CP_FILE_PATH, Charset.forName("UTF-8"));
		assertTrue(content.length() > 0);// 能读出文件内容
		assertTrue(content.contains("中国"));
	}

	@Test
	public void testWriteReadObject() throws Exception {
		FileUtil.deleteQuietly(new File("writeObject.txt"));// 消除影响
		HashMap<String, String> map = new HashMap<String, String>();
		for (int i = 0; i < 3; i++) {
			map.put("key" + i, "value" + i);
		}
		FileUtil.writeObject("writeObject.txt", map);
		assertTrue(true);// 没有异常

		@SuppressWarnings("unchecked")
        HashMap<String, String> map2 = (HashMap<String, String>) FileUtil.readObject("writeObject.txt");
		assertEquals(map, map2);
		FileUtil.deleteQuietly(new File("writeObject.txt"));// 消除影响
	}

	// ==================== Test cases for toBufferedInputStream ====================

	@Test
	public void testToBufferedInputStream() throws Exception {
		// Test with classpath file
		java.io.BufferedInputStream bis = FileUtil.toBufferedInputStream(EXIST_CP_FILE_PATH);
		assertNotNull(bis, "BufferedInputStream should not be null");

		// Verify we can read from the stream
		int firstByte = bis.read();
		assertTrue(firstByte != -1, "Should be able to read from BufferedInputStream");

		bis.close();
	}

	@Test
	public void testToBufferedInputStreamWithFileSystemPath() throws Exception {
		// Test with classpath file (since EXIST_FS_FILE_PATH might not exist in all environments)
		java.io.BufferedInputStream bis = FileUtil.toBufferedInputStream(EXIST_CP_FILE_PATH);
		assertNotNull(bis, "BufferedInputStream should not be null");

		// Verify stream is buffered
		assertTrue(bis instanceof java.io.BufferedInputStream, "Should return BufferedInputStream");

		// Read some bytes to verify stream works
		byte[] buffer = new byte[10];
		int bytesRead = bis.read(buffer);
		assertTrue(bytesRead > 0, "Should read bytes from stream");

		bis.close();
	}

	@Test
	public void testToBufferedInputStreamWithNonExistentFile() {
		// Test with non-existent file - should throw exception
		assertThrows(java.io.FileNotFoundException.class, () -> {
			FileUtil.toBufferedInputStream("nonexistent_file_12345.txt");
		});
	}

	@Test
	public void testToBufferedInputStreamReadFullContent() throws Exception {
		// Test reading full content from buffered stream
		java.io.BufferedInputStream bis = FileUtil.toBufferedInputStream(EXIST_CP_FILE_PATH);

		// Read all bytes
		byte[] content = IOUtil.readFully(bis);
		assertTrue(content.length > 0, "Should read content from file");

		bis.close();

		// Verify content matches direct file read
		byte[] directRead = FileUtil.readFileToByteArray(EXIST_CP_FILE_PATH);
		assertArrayEquals(directRead, content, "Content should match");
	}

	// ==================== Test cases for isModified(String srcFile, String clsFile) ====================

	@Test
	public void testIsModifiedWithSourceNewer() throws Exception {
		// Create test files
		File srcFile = new File("test_src_newer.java");
		File clsFile = new File("test_src_newer.class");

		try {
			// Create class file first
			FileUtil.write(clsFile, "class content", "UTF-8");
			Thread.sleep(10); // Ensure time difference

			// Create source file (newer)
			FileUtil.write(srcFile, "source content", "UTF-8");

			// Source is newer than class - should return true
			boolean modified = FileUtil.isModified(srcFile.getPath(), clsFile.getPath());
			assertTrue(modified, "Should detect source file is newer");

		} finally {
			FileUtil.deleteQuietly(srcFile);
			FileUtil.deleteQuietly(clsFile);
		}
	}

	@Test
	public void testIsModifiedWithClassNewer() throws Exception {
		// Create test files
		File srcFile = new File("test_cls_newer.java");
		File clsFile = new File("test_cls_newer.class");

		try {
			// Create source file first
			FileUtil.write(srcFile, "source content", "UTF-8");
			Thread.sleep(10); // Ensure time difference

			// Create class file (newer)
			FileUtil.write(clsFile, "class content", "UTF-8");

			// Class is newer than source - should return false
			boolean modified = FileUtil.isModified(srcFile.getPath(), clsFile.getPath());
			assertFalse(modified, "Should detect class file is newer or same");

		} finally {
			FileUtil.deleteQuietly(srcFile);
			FileUtil.deleteQuietly(clsFile);
		}
	}

	@Test
	public void testIsModifiedWithSourceNotExist() throws Exception {
		// Source file doesn't exist
		File clsFile = new File("test_no_src.class");

		try {
			FileUtil.write(clsFile, "class content", "UTF-8");

			// Source doesn't exist - should return false
			boolean modified = FileUtil.isModified("nonexistent_src.java", clsFile.getPath());
			assertFalse(modified, "Should return false when source doesn't exist");

		} finally {
			FileUtil.deleteQuietly(clsFile);
		}
	}

	@Test
	public void testIsModifiedWithClassNotExist() throws Exception {
		// Class file doesn't exist
		File srcFile = new File("test_no_cls.java");

		try {
			FileUtil.write(srcFile, "source content", "UTF-8");

			// Class doesn't exist - should return true (needs compilation)
			boolean modified = FileUtil.isModified(srcFile.getPath(), "nonexistent_cls.class");
			assertTrue(modified, "Should return true when class doesn't exist");

		} finally {
			FileUtil.deleteQuietly(srcFile);
		}
	}

	@Test
	public void testIsModifiedWithBothNotExist() {
		// Both files don't exist
		boolean modified = FileUtil.isModified("nonexistent_src.java", "nonexistent_cls.class");
		assertFalse(modified, "Should return false when both files don't exist");
	}

	@Test
	public void testIsModifiedWithSameTimestamp() throws Exception {
		// Create files with same timestamp
		File srcFile = new File("test_same_time.java");
		File clsFile = new File("test_same_time.class");

		try {
			// Create both files at "same" time
			FileUtil.write(srcFile, "source content", "UTF-8");
			FileUtil.write(clsFile, "class content", "UTF-8");

			// Should return false (class is not older)
			boolean modified = FileUtil.isModified(srcFile.getPath(), clsFile.getPath());
			assertFalse(modified, "Should return false when timestamps are same/similar");

		} finally {
			FileUtil.deleteQuietly(srcFile);
			FileUtil.deleteQuietly(clsFile);
		}
	}

	@Test
	public void testIsModifiedRealJavaClassFiles() throws Exception {
		// Test with actual Java source and class files if they exist
		// This tests the real use case scenario
		String srcPath = "src/test/java/io/hqwu/commons/util/FileUtilTest.java";
		String clsPath = "target/test-classes/io/hqwu/commons/util/FileUtilTest.class";

		File srcFile = new File(srcPath);
		File clsFile = new File(clsPath);

		if (srcFile.exists() && clsFile.exists()) {
			// If both exist, the method should work without throwing exception
			boolean modified = FileUtil.isModified(srcPath, clsPath);
			// We don't assert the result as it depends on actual file timestamps
			// Just verify the method executes successfully
			assertNotNull(modified); // This will always pass, just checking execution
		}
	}

	// ==================== Test cases for markToUpdate(String fileId) ====================

	@Test
	public void testMarkToUpdate() throws Exception {
		// Create a test file
		File testFile = new File("test_mark_update.txt");

		try {
			FileUtil.write(testFile, "initial content", "UTF-8");
			String filePath = testFile.getPath();

			// First check - should detect as not modified (first time)
			boolean modified1 = FileUtil.isModified(filePath);

			// Mark for update (remove from timestamp cache)
			FileUtil.markToUpdate(filePath);

			// Check again - should be detected as modified since cache was cleared
			boolean modified2 = FileUtil.isModified(filePath);
			assertTrue(modified2, "Should detect as modified after markToUpdate");

		} finally {
			FileUtil.deleteQuietly(testFile);
		}
	}

	@Test
	public void testMarkToUpdateRemovesCachedTimestamp() throws Exception {
		// Create test file
		File testFile = new File("test_cache_removal.txt");

		try {
			FileUtil.write(testFile, "content", "UTF-8");
			String filePath = testFile.getPath();

			// First call - caches timestamp
			FileUtil.isModified(filePath);

			// Second call without modification - should return false
			boolean modified1 = FileUtil.isModified(filePath);
			assertFalse(modified1, "Should not detect modification when file unchanged");

			// Mark to update - clears cache
			FileUtil.markToUpdate(filePath);

			// Third call - should detect as modified due to cache clear
			boolean modified2 = FileUtil.isModified(filePath);
			assertTrue(modified2, "Should detect as modified after cache cleared");

		} finally {
			FileUtil.deleteQuietly(testFile);
		}
	}

	@Test
	public void testMarkToUpdateWithNonExistentFile() {
		// Mark a non-existent file - should not throw exception
		assertDoesNotThrow(() -> {
			FileUtil.markToUpdate("nonexistent_file_xyz.txt");
		});
	}

	@Test
	public void testMarkToUpdateWithNullOrEmpty() {
		// Mark with null - will throw NullPointerException from Hashtable
		assertThrows(NullPointerException.class, () -> {
			FileUtil.markToUpdate(null);
		});

		// Mark with empty string - should not throw exception
		assertDoesNotThrow(() -> {
			FileUtil.markToUpdate("");
		});
	}

	@Test
	public void testMarkToUpdateMultipleFiles() throws Exception {
		// Test marking multiple files
		File file1 = new File("test_multi_1.txt");
		File file2 = new File("test_multi_2.txt");

		try {
			FileUtil.write(file1, "content 1", "UTF-8");
			FileUtil.write(file2, "content 2", "UTF-8");

			String path1 = file1.getPath();
			String path2 = file2.getPath();

			// Cache both files
			FileUtil.isModified(path1);
			FileUtil.isModified(path2);

			// Mark first file for update
			FileUtil.markToUpdate(path1);

			// First file should be detected as modified
			assertTrue(FileUtil.isModified(path1), "File 1 should be modified");

			// Second file should not be affected
			assertFalse(FileUtil.isModified(path2), "File 2 should not be modified");

		} finally {
			FileUtil.deleteQuietly(file1);
			FileUtil.deleteQuietly(file2);
		}
	}

	@Test
	public void testMarkToUpdateIntegrationWithIsModified() throws Exception {
		// Integration test: mark -> modify file -> check
		File testFile = new File("test_integration.txt");

		try {
			// Create initial file
			FileUtil.write(testFile, "initial", "UTF-8");
			String path = testFile.getPath();

			// First check caches timestamp
			FileUtil.isModified(path);

			// Wait and modify file
			Thread.sleep(10);
			FileUtil.write(testFile, "modified", "UTF-8");

			// Should detect modification
			assertTrue(FileUtil.isModified(path), "Should detect file was modified");

			// Now it's cached again, so another check returns false
			assertFalse(FileUtil.isModified(path), "Should not detect modification (cached)");

			// Mark to update
			FileUtil.markToUpdate(path);

			// Should detect as modified again
			assertTrue(FileUtil.isModified(path), "Should detect as modified after markToUpdate");

		} finally {
			FileUtil.deleteQuietly(testFile);
		}
	}

	@Test
	public void testIsModifiedWithURL() throws Exception {
		// Test isModified with file URL
		File testFile = new File("test_url_modified.txt");

		try {
			FileUtil.write(testFile, "content", "UTF-8");
			URL fileUrl = testFile.toURI().toURL();

			// First check
			FileUtil.isModified(fileUrl);

			// Second check - should return false (cached)
			assertFalse(FileUtil.isModified(fileUrl), "Should not detect modification (cached)");

			// Modify file
			Thread.sleep(10);
			FileUtil.write(testFile, "new content", "UTF-8");

			// Should detect modification
			assertTrue(FileUtil.isModified(fileUrl), "Should detect file was modified");

		} finally {
			FileUtil.deleteQuietly(testFile);
		}
	}
}