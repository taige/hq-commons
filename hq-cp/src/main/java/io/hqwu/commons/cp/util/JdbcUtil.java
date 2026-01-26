package io.hqwu.commons.cp.util;

import io.hqwu.commons.util.Logger;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.sql.*;
import java.util.Objects;
import java.util.StringTokenizer;

/**
 * JdbcUtil
 * @author wuhongqiang.taige
 */
public class JdbcUtil {
    private static final Logger LOGGER = new Logger();

    public final static String CRLF = System.getProperty("line.separator");

    public static boolean isWrapperFor(Class<?> iface, InvocationHandler handler, Object proxy, Wrapper wrapper) throws SQLException {
        if (iface == null) {
            return false;
        }
        return iface.isInstance(handler)
                || iface.isInstance(proxy)
                || wrapper.isWrapperFor(iface);
    }

    public static <T> T unwrap(Class<T> iface, InvocationHandler handler, Object proxy, Wrapper wrapper) throws SQLException {
        Objects.requireNonNull(iface);
        // 1. 如果要的是 Connection 接口，返回 Proxy 自身
        if (iface.isInstance(proxy)) {
            return iface.cast(proxy);
        }
        // 2. 如果要的是 Handler 类型，返回 this
        if (iface.isInstance(handler)) {
            return iface.cast(handler);
        }
        // 3. 否则，去底层解包
        return wrapper.unwrap(iface);
    }

    /**
     * Create a new instance of <code>Driver</code> with the driver class name.
     */
    public static Driver createDriver(String driverClassName) throws SQLException {
        Class<?> clazz = null;

        try {
            ClassLoader contextLoader = Thread.currentThread().getContextClassLoader();
            if (contextLoader != null) {
                clazz = contextLoader.loadClass(driverClassName);
            }
        } catch (ClassNotFoundException e) {
            // skip. will check later.
        }

        if (clazz == null) {
            try {
                clazz = Class.forName(driverClassName);
            } catch (ClassNotFoundException e) {
                throw new SQLException(e.getMessage(), e);
            }
        }

        try {
            return (Driver) clazz.newInstance();
        } catch (IllegalAccessException e) {
            throw new SQLException(e.getMessage(), e);
        } catch (InstantiationException e) {
            throw new SQLException(e.getMessage(), e);
        }
    }

    public static void closeQuietly(Closeable closeable) {
        try {
            if (closeable != null) {
                closeable.close();
            }
        } catch (IOException ioe) {
            // ignore
        }
    }

    /**
     * @param rs ResultSet
     */
    public static void closeQuietly(ResultSet rs) {
        if (rs != null) {
            try {
                rs.close();
            } catch (SQLException e) {
                LOGGER.warn("E close(ResultSet)...", e);
            }
        }
    }
    /**
     * @param stmt Statement
     */
    public static void closeQuietly(Statement stmt) {
        if (stmt != null) {
            try {
                stmt.close();
            } catch (SQLException e) {
                LOGGER.warn("E close(Statement)...", e);
            }
        }
    }

    public static String removeBreakingWhitespace(String original) {
        StringTokenizer whitespaceStripper = new StringTokenizer(original);
        StringBuilder builder = new StringBuilder();
        while (whitespaceStripper.hasMoreTokens()) {
            builder.append(whitespaceStripper.nextToken());
            builder.append(" ");
        }
        return builder.toString();
    }

    public static String multiLinesToOneLine(String lines, String replacement) {
        if (replacement == null) {
            replacement = "";
        }
        String str1 = replace(lines, "\r\n", replacement);
        String str2 = replace(str1, "\r", replacement);
        String str3 = replace(str2, "\n", replacement);
        return str3;
    }

    //以下方法copy from org.apache.commons.lang3.StringUtils，为了让Hqcp减少依赖

    public static String replace(String text, String searchString, String replacement) {
        return replace(text, searchString, replacement, -1);
    }

    public static String replace(String text, String searchString, String replacement, int max) {
        if (isEmpty(text) || isEmpty(searchString) || replacement == null || max == 0) {
            return text;
        }
        int start = 0;
        int end = text.indexOf(searchString, start);
        if (end == -1) {
            return text;
        }
        int replLength = searchString.length();
        int increase = replacement.length() - replLength;
        increase = increase < 0 ? 0 : increase;
        increase *= max < 0 ? 16 : max > 64 ? 64 : max;
        StringBuilder buf = new StringBuilder(text.length() + increase);
        while (end != -1) {
            buf.append(text.substring(start, end)).append(replacement);
            start = end + replLength;
            if (--max == 0) {
                break;
            }
            end = text.indexOf(searchString, start);
        }
        buf.append(text.substring(start));
        return buf.toString();
    }

    public static boolean isEmpty(CharSequence cs) {
        return cs == null || cs.length() == 0;
    }

}
