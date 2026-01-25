package io.hqwu.commons.util.enums;

/**
 * 签名类型枚举类。
 *
 * <p>该类定义了系统中支持的数字签名模式，主要用于安全校验、接口签名验证等场景。
 * 映射了签名编码（如 RSA, RSA2）与具体的签名算法（如 SHA1withRSA, SHA256withRSA）。
 *
 * <p>通常在签名工具类或安全配置中使用，用于指定加签和验签时所采用的算法标准。
 */
public enum SignTypeEnum {

    SHA1WITHRSA("RSA", "SHA1withRSA"),

    SHA256WITHRSA("RSA2", "SHA256withRSA");

    /** 签名编码. */
    private String code;

    /** 签名算法. */
    private String desc;

    /**
     * 根据签名编码构造签名模式.
     * @param code 签名编码.
     * @param desc 签名算法.
     */
    private SignTypeEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /**
     * 判断是否是SHA1WITHRSA的签名类型.
     * @return true/false.
     */
    public boolean isSHA1WITHRSA() {
        return this.equals(SHA1WITHRSA);
    }

    /**
     * 判断是否是RSA2的签名类型.
     * @return true/false.
     */
    public boolean isSHA256WITHRSA() {
        return this.equals(SHA256WITHRSA);
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
