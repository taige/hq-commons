package io.hqwu.commons.email;

import jakarta.mail.Address;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
public class EmailMessage implements Serializable {
    /**
     * 发件人
     */
    private Address from;
    /**
     * 收件人邮箱
     */
    private String to;
    /**
     * 抄送人邮箱
     */
    private String copyTo;
    /**
     * 邮件标题
     */
    private String subject;
    /**
     * 邮件正文内容
     */
    private String content;
    /**
     * 邮件正文内容类型 Mime type of the content
     */
    private String contentType;
    /**
     * 邮件附件全路径
     */
    private String attachmentFilePath;
}