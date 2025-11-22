package com.artemhontar.fluxdigitalstore.service.Email;

import com.artemhontar.fluxdigitalstore.exception.EmailFailureException;
import com.artemhontar.fluxdigitalstore.model.ResetPasswordToken;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class ResetPasswordEmailService extends BasicMailService {
    private static final String url = "http://localhost:8080/reset_password?token=";
    private static final String greeting = "Attempt to reset your password";
    private static final String text = "Hello! \n Someone asked for password reset. If it was you, please follow the link: ";

    public ResetPasswordEmailService(JavaMailSender mailSender) {
        super(mailSender);
    }

    public void sendResetPasswordEmail(ResetPasswordToken token) throws EmailFailureException {
        SimpleMailMessage message = createSimpleMailMessage();
        message.setTo(token.getLocalUser().getEmail());
        message.setSubject(greeting);
        message.setText(String.format("%s\n%s?%s", text, url, token.getToken()));
        try {
            mailSender.send(message);
        } catch (MailException ex) {
            throw new EmailFailureException();
        }
    }
}
