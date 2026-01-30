package io.hqwu.commons.email;

import io.hqwu.commons.util.Logger;
import io.hqwu.commons.util.StringUtil;
import jakarta.activation.DataHandler;
import jakarta.activation.FileDataSource;
import jakarta.mail.*;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.internet.MimeUtility;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.io.IOException;
import java.util.Date;

/**
 * 默认邮件服务实现类
 * <p>
 * 该类提供了基于Jakarta Mail API的邮件发送功能实现，支持发送带附件的HTML或纯文本邮件。
 * 使用{@link EmailSessionFactory}获取邮件会话，通过构造函数注入默认发件人地址和会话工厂。
 * </p>
 *
 * <p>主要功能：</p>
 * <ul>
 *   <li>支持发送纯文本和HTML格式的邮件</li>
 *   <li>支持添加多个附件（多个附件路径用逗号分隔）</li>
 *   <li>支持抄送功能</li>
 *   <li>自动处理邮件主题的中文编码</li>
 *   <li>提供灵活的发件人配置（可使用默认或自定义发件人）</li>
 * </ul>
 *
 * <p>使用示例：</p>
 * <pre>{@code
 * EmailSessionFactory factory = () -> Session.getInstance(props, authenticator);
 * Address from = new InternetAddress("sender@example.com");
 * DefaultEmailServiceImpl emailService = new DefaultEmailServiceImpl(from, factory);
 *
 * EmailMessage message = new EmailMessage()
 *     .setTo("receiver@example.com")
 *     .setSubject("测试邮件")
 *     .setContent("<h1>Hello</h1>")
 *     .setContentType("text/html;charset=UTF-8");
 * emailService.send(message);
 * }</pre>
 *
 * @author zhangjiankui
 * @version V1.0
 * @see EmailService
 * @see EmailMessage
 * @see EmailSessionFactory
 * @see MessagingException
 * @since 2020/6/16 18:38
 */
@RequiredArgsConstructor
public class DefaultEmailServiceImpl implements EmailService {
    private static final Logger LOGGER = new Logger();

    private final Address fromAddress;

    private final EmailSessionFactory sessionFactory;

    /**
     * 发送邮件
     *
     * @param emailMessage
     */
    @Override
    public void send(EmailMessage emailMessage) throws MessagingException {
        Session session = sessionFactory.getSession();

        try {
            // 组装邮件消息内容
            MimeMessage message = makeMimeMessage(emailMessage, session);

            // 发送邮件
            Transport.send(message);

            LOGGER.info("email successfully sent to={}, subject={}", emailMessage.getTo(), emailMessage.getSubject());
        } catch (IOException e) {
            LOGGER.warn("email sending error", e);
            throw new MessagingException("email sending error", e);
        }
    }

    /**
     * 组装邮件消息体
     * @param emailMessage
     * @param session
     * @return
     * @throws MessagingException
     * @throws IOException
     */
    private MimeMessage makeMimeMessage(EmailMessage emailMessage, Session session) throws MessagingException, IOException {
        MimeMessage message = new MimeMessage(session);
        // 发件人
        message.setFrom(emailMessage.getFrom() == null ? fromAddress : emailMessage.getFrom());

        // 收件人邮箱组
        message.setRecipients(Message.RecipientType.TO, emailMessage.getTo());
        // 抄送人邮箱组
        if (StringUtil.isNotBlank(emailMessage.getCopyTo())) {
            message.setRecipients(Message.RecipientType.CC, emailMessage.getCopyTo());
        }
        // 邮件主题
        if (StringUtil.isNotBlank(emailMessage.getSubject())) {
            // 邮件主题编码
            message.setSubject(MimeUtility.encodeText(emailMessage.getSubject()));
        }
        Multipart mp = new MimeMultipart();
        // 邮件正文内容
        MimeBodyPart mbpContent = new MimeBodyPart();
        if (StringUtil.isNotBlank(emailMessage.getContentType())) {
            mbpContent.setContent(emailMessage.getContent(), emailMessage.getContentType());
        } else {
            mbpContent.setText(emailMessage.getContent());
        }
        mp.addBodyPart(mbpContent);
        // 邮件附件组装
        if (StringUtil.isNotBlank(emailMessage.getAttachmentFilePath())) {
            makeEmailAttachment(emailMessage.getAttachmentFilePath(), mp);
        }
        message.setContent(mp);
        message.setSentDate(new Date());
        message.saveChanges();
        return message;
    }

    /**
     * 组装邮件附件内容
     * @param fileNames
     * @param mp
     * @throws MessagingException
     */
    private void makeEmailAttachment(String fileNames, Multipart mp) throws MessagingException {
        for (String fileName : fileNames.split(",")) {
            if (! new File(fileName).exists()) {
                continue;
            }
            MimeBodyPart mbpFile = new MimeBodyPart();
            FileDataSource fds = new FileDataSource(fileName);
            mbpFile.setDataHandler(new DataHandler(fds));
            mbpFile.setFileName(fds.getName());
            mp.addBodyPart(mbpFile);
        }
    }
}