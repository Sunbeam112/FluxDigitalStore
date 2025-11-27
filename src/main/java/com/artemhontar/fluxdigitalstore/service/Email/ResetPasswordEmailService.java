package com.artemhontar.fluxdigitalstore.service.Email;

import com.artemhontar.fluxdigitalstore.exception.EmailFailureException;
import com.artemhontar.fluxdigitalstore.model.ResetPasswordToken;
import jakarta.mail.internet.MimeMessage; // Use jakarta for Spring Boot 3+
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper; // New helper class
import org.springframework.stereotype.Service;

@Service
public class ResetPasswordEmailService extends BasicMailService {
    // The link should only contain the base URL
    private static final String URL_BASE = "http://localhost:8080/auth/v1/reset_password?token=%s";
    private static final String GREETING = "Password Reset Request";

    public ResetPasswordEmailService(JavaMailSender mailSender) {
        super(mailSender);
    }

    public void sendResetPasswordEmail(ResetPasswordToken token) throws EmailFailureException {
        try {
            // 1. Create a MimeMessage
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "UTF-8");

            // 2. Set the recipient and subject
            helper.setTo(token.getLocalUser().getEmail());
            helper.setFrom(super.fromAddress);
            helper.setSubject("🔑 " + GREETING);

            // 3. Construct the Fancier HTML Content
            String resetLink = String.format(URL_BASE, token.getToken());
            String htmlContent = String.format("""
                    <html>
                        <body style="font-family: sans-serif; background-color: #f4f4f4; padding: 20px;">
                            <div style="background-color: #ffffff; max-width: 600px; margin: auto; padding: 20px; border-radius: 8px; border-left: 5px solid #ffc107; box-shadow: 0 4px 8px rgba(0,0,0,0.1);">
                                <h2 style="color: #dc3545;">%s</h2>
                                <p style="color: #666666; line-height: 1.5;">Hello!</p>
                                <p style="color: #666666; line-height: 1.5;">We received a request to reset the password for your account. If this was you, please click the button below to complete the reset process:</p>
                                <div style="text-align: center; margin: 30px 0;">
                                    <a href="%s" style="display: inline-block; padding: 12px 25px; background-color: #dc3545; color: #ffffff; text-decoration: none; border-radius: 5px; font-weight: bold; font-size: 16px;">
                                        Reset Password
                                    </a>
                                </div>
                                <p style="color: #666666; line-height: 1.5;">If you did not request a password reset, you can safely ignore this email. The link will expire soon.</p>
                                <hr style="border: 0; border-top: 1px solid #eeeeee; margin: 20px 0;">
                                <p style="color: #999999; font-size: 0.75em;">For security reasons, do not share this link with anyone.</p>
                            </div>
                        </body>
                    </html>
                    """, GREETING, resetLink);

            // 4. Set the content and mark as HTML
            helper.setText(htmlContent, true);

            // 5. Send the MimeMessage
            mailSender.send(mimeMessage);
        } catch (MailException | jakarta.mail.MessagingException ex) {
            throw new EmailFailureException("Failed to send password reset email.");
        }
    }
}