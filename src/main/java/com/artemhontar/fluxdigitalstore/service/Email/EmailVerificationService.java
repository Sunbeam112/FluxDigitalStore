package com.artemhontar.fluxdigitalstore.service.Email;

import com.artemhontar.fluxdigitalstore.exception.EmailFailureException;
import com.artemhontar.fluxdigitalstore.model.VerificationToken;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailVerificationService extends BasicMailService {
    private static final String url = "localhost:8080/verify";
    private static final String greeting = "Hello! \nTo confirm the email, please follow the link:";

    public EmailVerificationService(JavaMailSender mailSender) {
        super(mailSender);
    }


    public void sendEmailConformationMessage(VerificationToken token) throws EmailFailureException {
        SimpleMailMessage message = createSimpleMailMessage();
        message.setTo(token.getLocalUser().getUsername());
        message.setSubject("Please confirm your email");
        message.setText(String.format("<h1>Test</h1><p>smaller text</p>\n%s\n%s?token=%s", greeting, url, token.getToken()));
        try {
            mailSender.send(message);
        } catch (MailException ex) {
            throw new EmailFailureException();
        }
    }

}
