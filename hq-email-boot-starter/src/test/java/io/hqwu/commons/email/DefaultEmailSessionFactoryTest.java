package io.hqwu.commons.email;

import com.icegreen.greenmail.configuration.GreenMailConfiguration;
import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.ServerSetupTest;
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.Logger;

import java.io.PrintStream;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class DefaultEmailSessionFactoryTest {

    @RegisterExtension
    static GreenMailExtension greenMail = new GreenMailExtension(ServerSetupTest.SMTP)
            .withConfiguration(GreenMailConfiguration.aConfig().withUser("testuser", "testpass"))
            .withPerMethodLifecycle(true);

    @Test
    void getSession_withAuthentication_shouldSucceed() throws MessagingException {
        // Arrange
        Properties props = new Properties();
        props.put("mail.smtp.host", ServerSetupTest.SMTP.getBindAddress());
        props.put("mail.smtp.port", String.valueOf(ServerSetupTest.SMTP.getPort()));
        DefaultEmailSessionFactory factory = new DefaultEmailSessionFactory(props)
                .setPasswordAuthentication(new PasswordAuthentication("testuser", "testpass"));

        // Act
        Session session = factory.getSession();
        
        // Assert
        assertThat(session).isNotNull();
        assertThat(session.getProperties()).containsAllEntriesOf(props);

        // Verify authentication by sending an email
        MimeMessage message = new MimeMessage(session);
        message.setFrom(new InternetAddress("sender@example.com"));
        message.addRecipient(Message.RecipientType.TO, new InternetAddress("receiver@example.com"));
        message.setSubject("Auth Test");
        message.setText("This should work.");
        
        Transport.send(message);

        assertThat(greenMail.getReceivedMessages()).hasSize(1);
    }

    @Test
    void getSession_withoutAuthentication_shouldSucceedWhenServerDoesNotRequireAuth() throws MessagingException {
        // Arrange
        // Stop the auth-required server and start one that doesn't need it
        greenMail.stop();
        greenMail.start();

        Properties props = new Properties();
        props.put("mail.smtp.host", ServerSetupTest.SMTP.getBindAddress());
        props.put("mail.smtp.port", String.valueOf(ServerSetupTest.SMTP.getPort()));
        // Instantiate factory without credentials
        DefaultEmailSessionFactory factory = new DefaultEmailSessionFactory(props);

        // Act
        Session session = factory.getSession();

        // Assert
        assertThat(session).isNotNull();

        // Verify by sending an email
        MimeMessage message = new MimeMessage(session);
        message.setFrom(new InternetAddress("sender@example.com"));
        message.addRecipient(Message.RecipientType.TO, new InternetAddress("receiver@example.com"));
        message.setSubject("No-Auth Test");
        message.setText("This should also work.");

        Transport.send(message);

        assertThat(greenMail.getReceivedMessages()).hasSize(1);
    }

    @Test
    void getSession_withoutAuthentication_shouldFailWhenServerRequiresAuth() throws MessagingException {
        // Arrange
        // Configure properties to require authentication
        Properties props = new Properties();
        props.put("mail.smtp.host", ServerSetupTest.SMTP.getBindAddress());
        props.put("mail.smtp.port", String.valueOf(ServerSetupTest.SMTP.getPort()));
        props.put("mail.smtp.auth", "true"); // Enable authentication requirement

        DefaultEmailSessionFactory factory = new DefaultEmailSessionFactory(props);

        // Act
        Session session = factory.getSession();
        
        // Assert
        assertThat(session).isNotNull();

        MimeMessage message = new MimeMessage(session);
        message.setFrom(new InternetAddress("sender@example.com"));
        message.addRecipient(Message.RecipientType.TO, new InternetAddress("receiver@example.com"));
        message.setSubject("Should Fail");
        message.setText("Test");

        // Attempting to send should fail because we haven't provided authentication
        // but the properties specify that auth is required
        assertThatThrownBy(() -> Transport.send(message))
                .isInstanceOf(MessagingException.class);
    }

    @Test
    void getSession_withDebugEnabled_shouldLogDebugInfo() throws MessagingException, InterruptedException {
        // Arrange
        Logger mockLogger = mock(Logger.class);
        when(mockLogger.isDebugEnabled()).thenReturn(true); // Ensure debug is enabled for the logger

        Properties props = new Properties();
        props.put("mail.smtp.host", ServerSetupTest.SMTP.getBindAddress());
        props.put("mail.smtp.port", String.valueOf(ServerSetupTest.SMTP.getPort()));
        props.put("mail.smtp.auth", "true"); // Enable authentication requirement for more verbose debug output
        props.put("mail.smtp.starttls.enable", "true"); // Add more properties to generate more debug logs

        DefaultEmailSessionFactory factory = new DefaultEmailSessionFactory(props)
                .setPasswordAuthentication(new PasswordAuthentication("testuser", "testpass"))
                .setDebug(true)
                .setDebugLogger(mockLogger);

        // Act
        Session session = factory.getSession();
        
        // Assert
        assertThat(session).isNotNull();
        assertThat(session.getDebug()).isTrue(); // Verify session debug is enabled

        // Send an email to trigger debug output
        MimeMessage message = new MimeMessage(session);
        message.setFrom(new InternetAddress("sender@example.com"));
        message.addRecipient(Message.RecipientType.TO, new InternetAddress("receiver@example.com"));
        message.setSubject("Debug Test");
        message.setText("This should generate debug logs.");

        // Get transport and send message, then close it
        Transport transport = session.getTransport("smtp");
        try {
            transport.connect();
            message.saveChanges();
            transport.sendMessage(message, message.getAllRecipients());
        } finally {
            transport.close();
        }

        // Use reflection to get and close the debug PrintStream from Session
        // This triggers the "stop email debug output" log without modifying source code
        try {
            java.lang.reflect.Field debugOutField = Session.class.getDeclaredField("out");
            debugOutField.setAccessible(true);
            PrintStream debugOut = (PrintStream) debugOutField.get(session);
            if (debugOut != null) {
                debugOut.close(); // Close the debug output stream
            }
        } catch (Exception e) {
            // If reflection fails, skip this step
        }

        // Give enough time for the async logger thread to process all output and finish
        Thread.sleep(100);

        // Verify that debug messages were logged
        verify(mockLogger, atLeastOnce()).debug(anyString());
        verify(mockLogger).debug("start email debug output");
        verify(mockLogger).debug("stop email debug output");
    }
}
