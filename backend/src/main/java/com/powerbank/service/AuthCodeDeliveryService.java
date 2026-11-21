package com.powerbank.service;

import com.powerbank.entity.AuthTokenPurpose;
import com.powerbank.entity.UserAccount;
import com.powerbank.exception.ApiException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class AuthCodeDeliveryService {

    private final ObjectProvider<JavaMailSender> mailSenders;
    private final boolean mailEnabled;
    private final String fromAddress;
    private final String configuredUsername;

    public AuthCodeDeliveryService(
            ObjectProvider<JavaMailSender> mailSenders,
            @Value("${app.mail.enabled:false}") boolean mailEnabled,
            @Value("${app.mail.from:}") String fromAddress,
            @Value("${spring.mail.username:}") String configuredUsername) {
        this.mailSenders = mailSenders;
        this.mailEnabled = mailEnabled;
        this.fromAddress = fromAddress;
        this.configuredUsername = configuredUsername;
    }

    public void deliver(UserAccount user, AuthTokenPurpose purpose, String code) {
        if (!mailEnabled || user.getEmail() == null || user.getEmail().isBlank()) {
            System.out.printf("Simulated %s code issued for user id %d: %s%n", purpose, user.getId(), code);
            return;
        }

        JavaMailSender sender = mailSenders.getIfAvailable();
        if (sender == null) {
            throw deliveryUnavailable();
        }

        String action = purpose == AuthTokenPurpose.RESET_PASSWORD ? "password recovery" : "account verification";
        SimpleMailMessage message = new SimpleMailMessage();
        String from = fromAddress.isBlank() ? configuredUsername : fromAddress;
        if (!from.isBlank()) message.setFrom(from);
        message.setTo(user.getEmail());
        message.setSubject("PowerBank Stations " + action + " code");
        message.setText("Your " + action + " code is: " + code
                + "\n\nThe code expires in 10 minutes. If you did not request it, you can ignore this email.");

        try {
            sender.send(message);
        } catch (MailException error) {
            throw deliveryUnavailable();
        }
    }

    private ApiException deliveryUnavailable() {
        return new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "CODE_DELIVERY_FAILED",
                "The email service could not send the code. Check the SMTP settings and try again.");
    }
}
