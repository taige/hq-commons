package io.hqwu.commons.email.autoconfigure;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.Properties;

@Validated
@Data
@ConfigurationProperties(prefix = "hq-commons.email")
public class EmailProperties {

    /**
     * sender account
     */
    @Email
    @NotBlank
    private String fromAddress;

    /**
     * nickname for sender account
     */
    private String fromNickname;

    /**
     * authentication account & password
     */
    private Auth auth;

    /**
     * SMTP 服务器配置属性
     * <p>配置示例（简化格式）：</p>
     * <pre>{@code
     * hq-commons.email:
     *   smtp:
     *     host: smtp.gmail.com
     *     port: 587
     *     auth: true
     *     starttls.enable: true
     * }</pre>
     *
     * <p>完整配置项参考：</p>
     * @see <a href="https://www.tutorialspoint.com/javamail_api/javamail_api_smtp_servers.htm">JavaMail API - SMTP Servers</a>
     */
    private Properties smtp;

    /**
     * debug
     */
    private Debug debug = new Debug();

    @Data
    public static class Auth {
        /**
         * username for authentication
         */
        private String username;
        /**
         * password for authentication
         */
        private String password;
    }

    @Data
    public static class Debug {
        /**
         * enable debug output
         */
        private boolean enabled = false;
        /**
         * debug output (slf4j) logger name
         */
        private String logger;
    }

}
