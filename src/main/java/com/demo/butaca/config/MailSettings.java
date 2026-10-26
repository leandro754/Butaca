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
    private String replyTo = "";
    private String testRecipient = "";
    private String publicUrl = "http://localhost:5173";
}
