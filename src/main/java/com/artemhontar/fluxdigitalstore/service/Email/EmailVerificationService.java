package com.artemhontar.fluxdigitalstore.service.Email;

import com.artemhontar.fluxdigitalstore.exception.EmailFailureException;
import com.artemhontar.fluxdigitalstore.model.VerificationToken;
import jakarta.mail.internet.MimeMessage; // Use jakarta for Spring Boot 3+
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper; // New helper class
import org.springframework.stereotype.Service;

@Service
public class EmailVerificationService extends BasicMailService {
    // Moved URL construction to be dynamic or a template variable, as it is complex HTML now
    private static final String URL_BASE = "http://localhost:8080/auth/v1/verify?token=%s";


    public EmailVerificationService(JavaMailSender mailSender) {
        super(mailSender);
    }

    public void sendEmailConformationMessage(VerificationToken token) throws EmailFailureException {
        try {
            // 1. Create a MimeMessage
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "UTF-8");

            // 2. Set the recipient and subject
            helper.setTo(token.getLocalUser().getUsername());
            helper.setFrom(super.fromAddress);
            helper.setSubject("Confirm Your Email for FluxDigitalStore");
            // Assuming BasicMailService sets the 'from' address, otherwise use helper.setFrom()

            // 3. Construct the Fancier HTML Content
            String verificationLink = String.format(URL_BASE, token.getToken());
            String htmlContent = String.format("""
                    <html>
                        <body style="font-family: sans-serif; background-color: #f4f4f4; padding: 20px;">
                            <div style="background-color: #ffffff; max-width: 600px; margin: auto; padding: 20px; border-radius: 8px; box-shadow: 0 4px 8px rgba(0,0,0,0.1);">
                                <h2 style="color: #333333;">Hello! Welcome to FluxDigitalStore!</h2>
                                <p style="color: #666666; line-height: 1.5;">Thank you for registering. To start enjoying our digital library, please confirm your email address by clicking the button below:</p>
                                <div style="text-align: center; margin: 30px 0;">
                                    <a href="%s" style="display: inline-block; padding: 10px 20px; background-color: #007bff; color: #ffffff; text-decoration: none; border-radius: 5px; font-weight: bold;">
                                        Verify My Email
                                    </a>
                                </div>
                                <p style="color: #666666; line-height: 1.5; font-size: 0.9em;">If the button above does not work, please copy and paste this link into your browser:</p>
                                <p style="word-break: break-all; font-size: 0.8em;"><a href="%s">%s</a></p>
                                <hr style="border: 0; border-top: 1px solid #eeeeee; margin: 20px 0;">
                                <p style="color: #999999; font-size: 0.75em;">FluxDigitalStore - Digital Library Access</p>
                            </div>
                        </body>
                    </html>
                    """, verificationLink, verificationLink, verificationLink);

            // 4. Set the content and mark as HTML
            helper.setText(htmlContent, true);

            // 5. Send the MimeMessage
            mailSender.send(mimeMessage);
        } catch (MailException | jakarta.mail.MessagingException ex) {
            throw new EmailFailureException("Failed to send email verification message.");
        }
    }
}