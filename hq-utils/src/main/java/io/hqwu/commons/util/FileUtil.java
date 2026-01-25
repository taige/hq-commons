package io.hqwu.commons.util;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.SerializationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Hashtable;
import java.util.Map;

/**
 * 文件操作工具类，扩展自 {@link org.apache.commons.io.FileUtils}。
 * <p>
 * 本类提供了增强的文件读写与管理功能，主要涵盖：
 * <ul>
 *     <li><b>资源加载：</b> 兼容文件系统路径与类路径（ClassPath）的文件读取与定位。</li>
 *     <li><b>对象序列化：</b> 集成 {@link org.apache.commons.lang3.SerializationUtils} 实现对象的持久化与反序列化。</li>
 *     <li><b>流转换：</b> 支持将指定路径的文件转换为 {@link java.io.InputStream} 或 {@link java.io.BufferedInputStream}。</li>
 *     <li><b>变更监控：</b> 提供文件修改检测机制，支持对配置文件及字节码文件的更新状态进行跟踪。</li>
 * </ul>
 *
 * @author shenjianlin
 * @version 1.0 2013-8-12
 */
public class FileUtil extends FileUtils {
    private static final Logger _log = LoggerFactory.getLogger(FileUtil.class);

    /**
     * 指定文件名读取文件全部内容，可以按文件系统路径也可以是ClassPath路径。<br>
     * 首先尝试从文件系统路径加载，找不到从ClassPath加载
     * 
     * @param filepath
     *            文件路径
     * @return 文件全部内容
     * @throws java.io.IOException
     *             I/O异常时抛出，如找不到文件等
     */
    public static byte[] readFileToByteArray(String filepath)
            throws IOException {
        File file = getFileByPath(filepath);
        return readFileToByteArray(file);
    }

    /**
     * 指定文件名读取文件全部内容，可以按文件系统路径也可以是ClassPath路径。<br>
     * 首先尝试从文件系统路径加载，找不到从ClassPath加载
     *
     * @param filepath
     *            文件路径
     * @param encoding
     *            字符集编码，用于解码读取的字节流
     * @return 文件内容，字符串形式
     * @throws java.io.IOException
     *             I/O异常时抛出，如找不到文件等
     * @see #readFileToByteArray(java.io.File)
     * @see #readFileToByteArray(String)
     */
    public static String readFileToString(String filepath, String encoding)
            throws IOException {
        File file = getFileByPath(filepath);
        return readFileToString(file, encoding);
    }

    /**
     * 指定文件名读取文件全部内容，可以按文件系统路径也可以是ClassPath路径。<br>
     * 首先尝试从文件系统路径加载，找不到从ClassPath加载
     *
     * @param filepath
     *            文件路径
     * @param encoding
     *            字符集编码，用于解码读取的字节流
     * @return 文件内容，字符串形式
     * @throws java.io.IOException
     *             I/O异常时抛出，如找不到文件等
     * @see #readFileToByteArray(java.io.File)
     * @see #readFileToByteArray(String)
     */
    public static String readFileToString(String filepath, Charset encoding)
            throws IOException {
        File file = getFileByPath(filepath);
        return readFileToString(file, encoding);
    }

    /**
     * 按文件路径创建文件对象，首先尝试从文件系统路径查找，找不到从ClassPath查找
     *
     * @param filepath
     *            文件路径
     * @return 文件对象，不抛出FileNotFoundException
     */
    public static File getFileByPath(String filepath) {
        File file = new File(filepath);
        if (!file.exists()) {// 不存在尝试从ClassPath加载
            file = getFileFromClassPath(filepath);
        } else {
            _log.debug("# getFileByPath({})={}", filepath,
                    file.getAbsoluteFile());
        }
        return file;
    }

    /**
     * 从classpath中查找文件
     *
     * @param filepath
     *            文件路径
     * @return 文件对象，不抛出FileNotFoundException
     */
    public static File getFileFromClassPath(String filepath) {
        File file = new File(filepath);
        URL url = ClassLoader.getSystemResource(filepath);
        if (url != null) {
            file = new File(url.getFile());
            _log.debug("# getFileFromClassPath({})={}", filepath,
                    file.getAbsoluteFile());
        } else {
            _log.warn("# getFileFromClassPath({})...not exist!", filepath);
        }
        return file;
    }

    /**
     * 将对象序列化并写入文件
     *
     * @param filename
     *            文件名
     * @param serializable
     *            待序列化对象
     */
    public static boolean writeObject(String filename, Object serializable) {
        try {
            SerializationUtils.serialize((Serializable) serializable,
                    new FileOutputStream(filename));
            _log.info("# writeObject({},{})...ok", filename, serializable);
            return true;
        } catch (IOException e) {
            _log.warn("# writeObject() fail with {}", e);
        }
        return false;
    }

    /**
     * 从文件读取内容并反序列化成对象
     *
     * @param filename
     *            文件名
     * @return 反序列化对象
     * @throws java.io.FileNotFoundException
     *             当文件找不到时抛出
     */
    public static Object readObject(String filename) throws IOException {
        byte[] bObj = readFileToByteArray(filename);
        Object obj = SerializationUtils.deserialize(bObj);
        _log.info("# readObject{{}}={}", filename, obj.getClass().getName());
        return obj;
    }

    /**
     * 将文件以流方式打开，以文件路径和classpath方式查找
     *
     * @param filename
     *            文件名
     * @return 文件流
     * @throws java.io.IOException
     *             发生IO异常时
     */
    public static final InputStream toInputStream(String filename)
            throws IOException {
        return new FileInputStream(getFileByPath(filename));
    }

    /**
     * 将文件以缓冲流方式打开，以文件路径和classpath方式查找
     *
     * @param filename
     *            文件名
     * @return BufferedInputStream文件流
     * @throws java.io.IOException
     *             发生IO异常时
     */
    public static final BufferedInputStream toBufferedInputStream(
            String filename) throws IOException {
        return new BufferedInputStream(new FileInputStream(
                getFileByPath(filename)));
    }
    private static Map<String, Long> _timestamp = new Hashtable<String, Long>();   //缓存上次更新时间(配置文件和class文件)

    //---------------------------------------------------------------
    public static boolean isModified(URL fileUrl)           {
        if (fileUrl==null)  return false;
        String filename = fileUrl.getFile();
        return  isModified(filename);
    }
    //---------------------------------------------------------------
    public static boolean isModified(String filename)           {
        Long o = _timestamp.get(filename);
        long    t = 0;
        if (o!=null)
            t = o.longValue();
        boolean b = isModified(filename, t);
        return  b;
    }
    public static boolean isModified(String fname, long lasttime)   {
        File ff = new File(fname);
        long    tt = ff.lastModified();
        boolean modified = ff.exists() && (tt > lasttime);
        if  (modified) {
            // modify by shenjl new Long() -->  Long.valueOf
            _timestamp.put(fname, Long.valueOf(tt));    //已经更新
        }
        return  modified;
    }
    //---------------------------------------------------------------
    // 2003-4-30 10:56 更新，同时比较java源代码文件和class字节码文件
    public static boolean isModified(String srcFile, String clsFile)    {
        File fileJava   = new File(srcFile);
        File fileClass  = new File(clsFile);
        boolean b = fileJava.exists() && (!fileClass.exists() || fileJava.lastModified() > fileClass.lastModified());
        return  b;
    }
    //---------------------------------------------------------------
    //已经检查，需要更新，重新加载
    public static void markToUpdate(String fileId)  {
        _timestamp.remove(fileId);
    }


}
