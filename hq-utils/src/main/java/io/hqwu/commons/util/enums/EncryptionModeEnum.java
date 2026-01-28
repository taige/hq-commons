package io.hqwu.commons.util.enums;

/**
 * 加密模式枚举类。
 * <p>
 * 本枚举定义了系统支持的加密算法类型，涵盖了常见的非对称加密（RSA）与对称加密（AES）模式。
 * 主要用于在安全工具类（如 {@code EncryptionUtil}）或加密服务中作为参数，以指定具体的加密策略和算法强度。
 * </p>
 */
public enum EncryptionModeEnum {
    /** RSA1024加密算法 */
    RSA1024("rsa1024", "RSA1024加密算法"),

    /** RSA2048加密算法 */
    RSA2048("rsa2048", "RSA2048加密算法"),

    /** AES对称加密算法 */
    AES("aes", "AES对称加密算法");

    /**code*/
    private String code;

    /**描述*/
    private String desc;

    /**
     * 构造函数
     * @param code
     * @param desc
     */
    private EncryptionModeEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /**
     * Getter method for property <tt>code</tt>.
     * 
     * @return property value of code
     */
    public String getCode() {
        return code;
    }

    /**
     * Setter method for property <tt>code</tt>.
     * 
     * @param code value to be assigned to property code
     */
    public void setCode(String code) {
        this.code = code;
    }

    /**
     * Getter method for property <tt>desc</tt>.
     * 
     * @return property value of desc
     */
    public String getDesc() {
        return desc;
    }

    /**
     * Setter method for property <tt>desc</tt>.
     * 
     * @param desc value to be assigned to property desc
     */
    public void setDesc(String desc) {
        this.desc = desc;
    }
}
