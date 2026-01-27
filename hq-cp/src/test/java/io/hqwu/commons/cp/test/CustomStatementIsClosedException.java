package io.hqwu.commons.cp.test;

/**
 * 测试用异常类，类名以StatementIsClosedException结尾
 * 不继承SQLException，用于测试MySQLPooledConnection中cause链的类名检查逻辑
 */
public class CustomStatementIsClosedException extends Exception {
    public CustomStatementIsClosedException(String message) {
        super(message);
    }
}
