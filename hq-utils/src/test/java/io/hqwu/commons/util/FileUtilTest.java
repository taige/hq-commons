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
}