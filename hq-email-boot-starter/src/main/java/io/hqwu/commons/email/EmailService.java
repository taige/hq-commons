package io.hqwu.commons.email;

import jakarta.mail.MessagingException;
import jakarta.mail.Session;

/**
 * 邮件服务接口
 * <p>
 * 提供邮件发送功能的核心接口，支持通过{@link EmailMessage}对象发送邮件。
 * 实现类需要提供具体的邮件发送逻辑，并通过{@link EmailSessionFactory}获取邮件会话。
 * </p>
 *
 * @author AlphaPay
 * @see EmailMessage 邮件消息实体类
 * @see MessagingException 邮件发送异常
 * @see EmailSessionFactory 邮件会话工厂
 * @see Session Jakarta Mail会话对象
 */
public interface EmailService {
    /**
     * 通用发送邮件方法
     *
     * @param emailMessage 邮件消息对象，包含发件人、收件人、邮件主题、内容等信息
     * @throws MessagingException 当邮件发送失败时抛出此异常
     */
    void send(EmailMessage emailMessage) throws MessagingException;

    /**
     * 邮件会话工厂接口
     * <p>
     * 提供获取Jakarta Mail会话对象的工厂方法。
     * 实现类需要根据具体的邮件服务器配置创建并返回{@link Session}实例。
     * </p>
     *
     * @see Session Jakarta Mail会话对象
     */
    interface EmailSessionFactory {
        /**
         * 获取邮件会话对象
         *
         * @return Jakarta Mail会话实例
         */
        Session getSession();
    }
}