package com.artemhontar.fluxdigitalstore.service.Email;

import com.artemhontar.fluxdigitalstore.exception.EmailFailureException;
import com.artemhontar.fluxdigitalstore.model.LocalUser;
import com.artemhontar.fluxdigitalstore.model.VerificationToken;
import com.artemhontar.fluxdigitalstore.model.ResetPasswordToken;

import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;

import java.io.ByteArrayOutputStream;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    // Inject Mocks into the Services (assuming BasicMailService uses the mailSender)
    @InjectMocks
    private EmailVerificationService emailVerificationService;

    @InjectMocks
    private ResetPasswordEmailService resetPasswordEmailService;

    private MimeMessage mockMimeMessage;

    // --- Mock Data ---
    private LocalUser testUser;
    private VerificationToken verifToken;
    private ResetPasswordToken resetToken;
    private final String TEST_EMAIL = "test@example.com";
    private final String TEST_VERIF_TOKEN_STRING = "verif-token-123";
    private final String TEST_RESET_TOKEN_STRING = "reset-token-456";

    @BeforeEach
    void setUp() {
        // MimeMessageHelper requires a real MimeMessage instance, which needs a Session.
        // We ensure our mock sender returns this real message instance.
        Session session = Session.getDefaultInstance(new Properties());
        mockMimeMessage = new MimeMessage(session);

        // Mock the creation of the MimeMessage
        when(mailSender.createMimeMessage()).thenReturn(mockMimeMessage);

        // Setup LocalUser mock data
        testUser = new LocalUser();
        // FIX: Changed setUsername() to setEmail() as LocalUser uses 'email' field and Lombok generates setEmail().
        testUser.setEmail(TEST_EMAIL);

        // Setup VerificationToken mock data
        verifToken = new VerificationToken();
        verifToken.setLocalUser(testUser);
        verifToken.setToken(TEST_VERIF_TOKEN_STRING);

        // Setup ResetPasswordToken mock data
        resetToken = new ResetPasswordToken();
        resetToken.setLocalUser(testUser);
        resetToken.setToken(TEST_RESET_TOKEN_STRING);
    }

    /**
     * Helper method to extract the raw content of the MimeMessage as a String.
     * This is necessary because MimeMessage does not have a simple getContentAsString() method.
     */
    private String getMimeMessageContent(MimeMessage message) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        // writeTo dumps the full email content (headers, body, etc.) to the stream
        message.writeTo(baos);
        return baos.toString("UTF-8");
    }

    // --------------------------------------------------------------
    // EmailVerificationService Tests
    // --------------------------------------------------------------

    @Test
    void sendEmailConformationMessage_Success_MailSentAndContentIsHTML() throws Exception {
        // Arrange
        // Capture the MimeMessage object passed to the send method
        ArgumentCaptor<MimeMessage> messageCaptor = ArgumentCaptor.forClass(MimeMessage.class);

        // Act
        emailVerificationService.sendEmailConformationMessage(verifToken);

        // Assert
        // 1. Verify send was called exactly once
        verify(mailSender, times(1)).send(messageCaptor.capture());

        MimeMessage sentMessage = messageCaptor.getValue();

        // 2. Verify Recipient and Subject
        assertEquals("✅ Confirm Your Email for FluxDigitalStore", sentMessage.getSubject(), "Subject is incorrect.");
        assertEquals(TEST_EMAIL, ((InternetAddress) sentMessage.getAllRecipients()[0]).getAddress(), "Recipient email is incorrect.");

        // 3. Verify HTML Content
        String content = getMimeMessageContent(sentMessage);

        // Check for key HTML elements and the token/URL
        assertTrue(content.contains("<html>"), "Content should contain <html> tag indicating HTML format.");
        assertTrue(content.contains("Verify My Email"), "Content should contain the call-to-action button text.");
        assertTrue(content.contains(TEST_VERIF_TOKEN_STRING), "Content should contain the verification token.");
        assertTrue(content.contains("localhost:8080/auth/v1/verify"), "Content should contain the correct base URL.");
    }

    @Test
    void sendEmailConformationMessage_MailException_ThrowsEmailFailureException() {
        // Arrange
        // Simulate a failure when mailSender.send() is called
        doThrow(mock(MailException.class)).when(mailSender).send(any(MimeMessage.class));

        // Act & Assert
        assertThrows(EmailFailureException.class, () ->
                emailVerificationService.sendEmailConformationMessage(verifToken)
        );

        // Verify send was still attempted
        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }

    // --------------------------------------------------------------
    // ResetPasswordEmailService Tests
    // --------------------------------------------------------------

    @Test
    void sendResetPasswordEmail_Success_MailSentAndContentIsHTML() throws Exception {
        // Arrange
        ArgumentCaptor<MimeMessage> messageCaptor = ArgumentCaptor.forClass(MimeMessage.class);

        // Act
        resetPasswordEmailService.sendResetPasswordEmail(resetToken);

        // Assert
        // 1. Verify send was called exactly once
        verify(mailSender, times(1)).send(messageCaptor.capture());

        MimeMessage sentMessage = messageCaptor.getValue();

        // 2. Verify Recipient and Subject
        assertEquals("🔑 Password Reset Request", sentMessage.getSubject(), "Subject is incorrect.");
        assertEquals(TEST_EMAIL, ((InternetAddress) sentMessage.getAllRecipients()[0]).getAddress(), "Recipient email is incorrect.");

        // 3. Verify HTML Content
        String content = getMimeMessageContent(sentMessage);

        // Check for key HTML elements and the token/URL
        assertTrue(content.contains("<html>"), "Content should contain <html> tag indicating HTML format.");
        assertTrue(content.contains("Reset Password"), "Content should contain the call-to-action button text.");
        assertTrue(content.contains(TEST_RESET_TOKEN_STRING), "Content should contain the reset token.");
        assertTrue(content.contains("http://localhost:8080/auth/v1/reset_password"), "Content should contain the correct base URL.");
    }

    @Test
    void sendResetPasswordEmail_MailException_ThrowsEmailFailureException() {
        // Arrange
        // Simulate a failure when mailSender.send() is called
        doThrow(mock(MailException.class)).when(mailSender).send(any(MimeMessage.class));

        // Act & Assert
        assertThrows(EmailFailureException.class, () ->
                resetPasswordEmailService.sendResetPasswordEmail(resetToken)
        );

        // Verify send was still attempted
        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }
}