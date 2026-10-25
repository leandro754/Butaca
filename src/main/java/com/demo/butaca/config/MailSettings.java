package com.demo.butaca.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "butaca.mail")
public class MailSettings {
    private boolean enabled;
    private String from = "";
    private String replyTo = "contact@leandro754.moe";
    private String testRecipient = "contact@leandro754.moe";
    private String publicUrl = "http://localhost:5173";
}
