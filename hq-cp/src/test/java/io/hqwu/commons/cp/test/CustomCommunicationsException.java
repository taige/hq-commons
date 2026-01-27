package io.hqwu.commons.cp.test;

import java.sql.SQLException;

/**
 * 测试用异常类，类名以CommunicationsException结尾
 * 但不继承SQLRecoverableException，用于测试MySQLPooledConnection中的类名检查逻辑
 *
 * sqlState使用"23000"（完整性约束违反），避免被父类isFetalException拦截：
 * - 不是null
 * - 不以"08"开头（连接异常）
 * - 不以'5'-'9'开头
 */
public class CustomCommunicationsException extends SQLException {
    public CustomCommunicationsException(String message) {
        super(message, "23000", 0); // 使用完整性约束违反的sqlState
    }

    public CustomCommunicationsException(String message, Throwable cause) {
        super(message, "23000", 0, cause);
    }
}
