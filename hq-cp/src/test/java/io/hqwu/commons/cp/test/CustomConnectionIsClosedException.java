package io.hqwu.commons.cp.test;

/**
 * 测试用异常类，类名以ConnectionIsClosedException结尾
 * 不继承SQLException，用于测试MySQLPooledConnection中cause链的类名检查逻辑
 */
public class CustomConnectionIsClosedException extends Exception {
    public CustomConnectionIsClosedException(String message) {
        super(message);
    }
}
