package io.hqwu.commons.email;

import com.icegreen.greenmail.configuration.GreenMailConfiguration;
import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.ServerSetupTest;
import io.hqwu.commons.email.EmailService.EmailSessionFactory;
import jakarta.mail.Address;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DefaultEmailServiceImplTest {

    @RegisterExtension
    static GreenMailExtension greenMail = new GreenMailExtension(ServerSetupTest.SMTP)
            .withConfiguration(GreenMailConfiguration.aConfig().withUser("test", "password"))
            .withPerMethodLifecycle(true);

    private DefaultEmailServiceImpl emailService;
    private Address fromAddress;
    private EmailSessionFactory sessionFactory;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() throws MessagingException {
        fromAddress = new InternetAddress("default-sender@example.com");
        
        // This factory implementation covers the EmailSessionFactory interface
        sessionFactory = () -> greenMail.getSmtp().createSession();

        emailService = new DefaultEmailServiceImpl(fromAddress, sessionFactory);
    }

    @Test
    void send_shouldSendSimpleTextEmail() throws MessagingException, IOException {
        EmailMessage emailMessage = new EmailMessage()
                .setTo("receiver@example.com")
                .setSubject("Test Subject")
                .setContent("This is the email content.");

        emailService.send(emailMessage);

        MimeMessage[] receivedMessages = greenMail.getReceivedMessages();
        assertThat(receivedMessages).hasSize(1);

        MimeMessage receivedMessage = receivedMessages[0];
        assertThat(receivedMessage.getFrom()).contains(fromAddress);
        assertThat(receivedMessage.getRecipients(Message.RecipientType.TO))
                .extracting(Address::toString)
                .containsExactly("receiver@example.com");
        assertThat(receivedMessage.getSubject()).isEqualTo("Test Subject");

        // Content is wrapped in MimeMultipart
        MimeMultipart multipart = (MimeMultipart) receivedMessage.getContent();
        assertThat(multipart.getBodyPart(0).getContent().toString()).contains("This is the email content.");
    }

    @Test
    void send_shouldSendHtmlEmail() throws MessagingException, IOException {
        EmailMessage emailMessage = new EmailMessage()
                .setTo("receiver@example.com")
                .setSubject("HTML Test")
                .setContent("<h1>Hello</h1>")
                .setContentType("text/html;charset=UTF-8");

        emailService.send(emailMessage);

        MimeMessage receivedMessage = greenMail.getReceivedMessages()[0];
        assertThat(receivedMessage.getContentType()).startsWith("multipart/mixed");
        MimeMultipart multipart = (MimeMultipart) receivedMessage.getContent();
        assertThat(multipart.getBodyPart(0).getContentType()).startsWith("text/html");
        assertThat(multipart.getBodyPart(0).getContent().toString()).isEqualTo("<h1>Hello</h1>");
    }

    @Test
    void send_shouldSendWithCC() throws MessagingException {
        EmailMessage emailMessage = new EmailMessage()
                .setTo("to@example.com")
                .setCopyTo("cc1@example.com,cc2@example.com")
                .setSubject("CC Test")
                .setContent("Content");

        emailService.send(emailMessage);

        MimeMessage[] receivedMessages = greenMail.getReceivedMessages();
        // GreenMail creates separate messages for each recipient (TO + CC recipients)
        assertThat(receivedMessages).hasSizeGreaterThanOrEqualTo(1);
        MimeMessage receivedMessage = receivedMessages[0];

        assertThat(receivedMessage.getRecipients(Message.RecipientType.TO))
                .extracting(Address::toString)
                .containsExactly("to@example.com");
        assertThat(receivedMessage.getRecipients(Message.RecipientType.CC))
                .extracting(Address::toString)
                .containsExactly("cc1@example.com", "cc2@example.com");
    }

    @Test
    void send_shouldUseCustomFromAddress() throws MessagingException {
        Address customFrom = new InternetAddress("custom-sender@example.com");
        EmailMessage emailMessage = new EmailMessage()
                .setFrom(customFrom)
                .setTo("receiver@example.com")
                .setSubject("Custom From Test")
                .setContent("Content");

        emailService.send(emailMessage);

        MimeMessage receivedMessage = greenMail.getReceivedMessages()[0];
        assertThat(receivedMessage.getFrom()).contains(customFrom);
    }

    @Test
    void send_shouldSendWithAttachments() throws IOException, MessagingException {
        File attachment1 = Files.createFile(tempDir.resolve("att1.txt")).toFile();
        Files.writeString(attachment1.toPath(), "attachment content 1");

        File attachment2 = Files.createFile(tempDir.resolve("att2.log")).toFile();
        Files.writeString(attachment2.toPath(), "attachment content 2");

        String nonExistentFile = tempDir.resolve("nonexistent.txt").toString();

        String attachmentPaths = attachment1.getAbsolutePath() + "," + nonExistentFile + "," + attachment2.getAbsolutePath();

        EmailMessage emailMessage = new EmailMessage()
                .setTo("receiver@example.com")
                .setSubject("Attachment Test")
                .setContent("Email with attachments")
                .setAttachmentFilePath(attachmentPaths);

        emailService.send(emailMessage);

        MimeMessage receivedMessage = greenMail.getReceivedMessages()[0];
        assertThat(receivedMessage.getContent()).isInstanceOf(MimeMultipart.class);

        MimeMultipart multipart = (MimeMultipart) receivedMessage.getContent();
        // Expect 1 part for content + 2 parts for attachments
        assertThat(multipart.getCount()).isEqualTo(3);

        assertThat(multipart.getBodyPart(1).getFileName()).isEqualTo("att1.txt");
        assertThat(multipart.getBodyPart(2).getFileName()).isEqualTo("att2.log");
    }
    
    @Test
    void send_shouldHandleNoSubject() throws MessagingException {
        EmailMessage emailMessage = new EmailMessage()
                .setTo("receiver@example.com")
                .setContent("No subject");

        emailService.send(emailMessage);
        
        MimeMessage receivedMessage = greenMail.getReceivedMessages()[0];
        assertThat(receivedMessage.getSubject()).isNullOrEmpty();
    }

    @Test
    void send_shouldThrowEmailExceptionWhenTransportFails() throws MessagingException {
        // Create a failing email service with invalid SMTP configuration
        EmailSessionFactory failingSessionFactory = () -> {
            // Create a session with invalid host to cause transport failure
            java.util.Properties props = new java.util.Properties();
            props.put("mail.smtp.host", "invalid.host.that.does.not.exist");
            props.put("mail.smtp.port", "25");
            props.put("mail.smtp.connectiontimeout", "1000");
            props.put("mail.smtp.timeout", "1000");
            return jakarta.mail.Session.getInstance(props);
        };

        DefaultEmailServiceImpl failingEmailService = new DefaultEmailServiceImpl(fromAddress, failingSessionFactory);

        EmailMessage emailMessage = new EmailMessage()
                .setTo("receiver@example.com")
                .setSubject("Failure Test")
                .setContent("This will fail");

        assertThatThrownBy(() -> failingEmailService.send(emailMessage))
                .isInstanceOf(MessagingException.class)
                .hasMessage("Couldn't connect to host, port: invalid.host.that.does.not.exist, 25; timeout 1000");
    }

    @Test
    @Disabled("与 EmailAutoConfigurationTest#shouldCreateFromAddressWithChineseNickname 用例不相容")
    void send_shouldWrapIOExceptionInMessagingException() {
        // Set invalid charset to force IOException in MimeUtility.encodeText
        // verification: see experimental test which confirmed setting checking mail.mime.charset causes UnsupportedEncodingException
        String originalCharset = System.getProperty("mail.mime.charset");
        System.setProperty("mail.mime.charset", "INVALID_CHARSET_FOR_TEST");

        try {
            EmailMessage emailMessage = new EmailMessage()
                    .setTo("receiver@example.com")
                    // Must contain non-ASCII characters to trigger encoding logic
                    .setSubject("Subject with unicode \u4e2d\u6587")
                    .setContent("Test content");

            assertThatThrownBy(() -> emailService.send(emailMessage))
                    .isInstanceOf(MessagingException.class)
                    .hasMessage("email sending error")
                    .hasCauseInstanceOf(IOException.class);
        } finally {
            // Restore system property
            if (originalCharset != null) {
                System.setProperty("mail.mime.charset", originalCharset);
            } else {
                System.clearProperty("mail.mime.charset");
            }
        }
    }
}
