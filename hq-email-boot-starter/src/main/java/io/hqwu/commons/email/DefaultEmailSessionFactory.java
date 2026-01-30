package io.hqwu.commons.email;

import jakarta.mail.Authenticator;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.slf4j.Logger;

import java.io.*;
import java.util.Properties;

/**
 * 默认的邮件会话工厂实现类。
 * <p>
 * 该类实现了{@link EmailService.EmailSessionFactory}接口，用于创建和配置Jakarta Mail的{@link Session}对象。
 * 提供了SMTP配置、身份认证、调试日志输出等功能，支持链式调用配置。
 * </p>
 *
 * <p>主要功能：</p>
 * <ul>
 *   <li>基于SMTP配置属性创建邮件会话</li>
 *   <li>支持密码认证方式连接邮件服务器</li>
 *   <li>支持调试模式，可将邮件发送过程的详细日志输出到指定的SLF4J Logger</li>
 *   <li>通过管道流将Jakarta Mail的调试输出重定向到日志系统</li>
 * </ul>
 *
 * <p>使用示例：</p>
 * <pre>{@code
 * Properties smtpProps = new Properties();
 * smtpProps.put("mail.smtp.host", "smtp.example.com");
 * smtpProps.put("mail.smtp.port", "587");
 *
 * DefaultEmailSessionFactory factory = new DefaultEmailSessionFactory(smtpProps)
 *     .setPasswordAuthentication(new PasswordAuthentication("user@example.com", "password"))
 *     .setDebug(true)
 *     .setDebugLogger(LoggerFactory.getLogger(DefaultEmailSessionFactory.class));
 *
 * Session session = factory.getSession();
 * }</pre>
 *
 * @author taige (Wu, Hongqiang)
 * @see EmailService.EmailSessionFactory
 * @see jakarta.mail.Session
 * @see jakarta.mail.PasswordAuthentication
 * @see org.slf4j.Logger
 * @since 2021-12-11
 */
@RequiredArgsConstructor
@Setter
@Accessors(chain = true)
public class DefaultEmailSessionFactory implements EmailService.EmailSessionFactory {

    private final Properties smtpProperties;

    private PasswordAuthentication passwordAuthentication;

    private boolean debug;

    private Logger debugLogger;

    @Override
    public Session getSession() {
        Session session = passwordAuthentication == null ?
                Session.getInstance(smtpProperties) :
                Session.getInstance(smtpProperties, new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return passwordAuthentication;
                    }
                });
        if (debug && debugLogger != null && debugLogger.isDebugEnabled()) {
            session.setDebugOut(debugOutput(debugLogger));
            session.setDebug(debug);
        }
        return session;
    }

    PrintStream debugOutput(Logger logger) {
        PipedOutputStream pos = new PipedOutputStream();
        PipedInputStream pis = new PipedInputStream();
        try {
            pos.connect(pis);
        } catch (IOException e) {
            try {
                pos.close();
                pis.close();
            } catch (IOException ignored) {}
            return null;
        }
        BufferedReader bis = new BufferedReader(new InputStreamReader(pis));
        Thread debugger = new Thread(() -> {
            logger.debug("start email debug output");
            try {
                while (true) {
                    String emailLog = bis.readLine();
                    if (emailLog == null) {
                        break;
                    }
                    logger.debug(emailLog);
                }
            } catch (IOException ignored) {
            } finally {
                logger.debug("stop email debug output");
            }
        });
        debugger.setName("EmailLogger");
        debugger.setDaemon(true);
        debugger.start();
        return new PrintStream(pos);
    }

}