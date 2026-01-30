package io.hqwu.commons.email.autoconfigure;

import io.hqwu.commons.email.DefaultEmailServiceImpl;
import io.hqwu.commons.email.DefaultEmailSessionFactory;
import io.hqwu.commons.email.EmailService;
import io.hqwu.commons.email.EmailService.EmailSessionFactory;
import io.hqwu.commons.util.LoggerFactory;
import io.hqwu.commons.util.StringUtil;
import jakarta.mail.Address;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeUtility;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.io.UnsupportedEncodingException;
import java.util.Properties;

@AutoConfiguration
@ConditionalOnClass(EmailService.class)
@EnableConfigurationProperties(EmailProperties.class)
@ConditionalOnProperty(prefix = "hq-commons.email", name = {"from-address", "smtp.host"})
public class EmailAutoConfiguration {

    @Bean
    public PasswordAuthentication passwordAuthentication(EmailProperties emailProperties) {
        if (emailProperties.getAuth() == null) {
            return null;
        }
        return new PasswordAuthentication(
                emailProperties.getAuth().getUsername(),
                emailProperties.getAuth().getPassword()
        );
    }

    @Bean
    public Properties smtpProperties(EmailProperties emailProperties) {
        Properties props = new Properties();

        if (emailProperties.getAuth() != null && ! emailProperties.getSmtp().containsKey("auth")) {
            props.put("mail.smtp.auth", true);
        }
        emailProperties.getSmtp().forEach((key, value) -> {
            props.put("mail.smtp." + key, value);
        });

        return props;
    }

    @Bean
    public Address fromAddress(EmailProperties emailProperties) throws AddressException, UnsupportedEncodingException {
        if (StringUtil.isBlank(emailProperties.getFromNickname())) {
            return new InternetAddress(emailProperties.getFromAddress());
        }
        return new InternetAddress(MimeUtility.encodeText(
                emailProperties.getFromNickname()) + "<" + emailProperties.getFromAddress() + ">");
    }

    @Bean
    @ConditionalOnMissingBean
    public EmailSessionFactory mailSessionFactory(
            EmailProperties emailProperties,
            Properties smtpProperties,
            @Autowired(required = false) PasswordAuthentication passwordAuthentication) {
        return new DefaultEmailSessionFactory(smtpProperties)
                .setDebug(emailProperties.getDebug().isEnabled())
                .setDebugLogger(emailProperties.getDebug().isEnabled()
                        ? LoggerFactory.getLogger(emailProperties.getDebug().getLogger())
                        : null)
                .setPasswordAuthentication(passwordAuthentication);
    }

    @Bean
    @ConditionalOnMissingBean
    public EmailService emailService(Address fromAddress,
                                     EmailSessionFactory emailSessionFactory) {
        return new DefaultEmailServiceImpl(fromAddress, emailSessionFactory);
    }

}