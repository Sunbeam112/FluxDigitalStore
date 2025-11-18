package com.artemhontar.fluxdigitalstore.service.Email;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

public abstract class BasicMailService {


    @Value("${email.from}")
    protected String fromAddress;


    public final JavaMailSender mailSender;

    public BasicMailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    protected SimpleMailMessage createSimpleMailMessage() {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        return message;
    }

}
