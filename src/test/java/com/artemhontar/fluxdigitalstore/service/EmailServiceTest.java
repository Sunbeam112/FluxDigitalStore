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

/**
 * Unit tests for the email service classes: {@link EmailVerificationService} and {@link ResetPasswordEmailService}.
 * These tests utilize Mockito to mock the {@link JavaMailSender} dependency, allowing for the verification
 * of email content and method invocation without actually sending emails.
 * <p>
 * The test setup includes a crucial step using reflection to manually set the 'fromAddress'
 * field inherited from {@link BasicMailService}, as the Spring @Value annotation is not processed
 * in a pure Mockito unit testing environment.
 */
@ExtendWith(MockitoExtension.class)
public class EmailServiceTest {

    /**
     * Mock object for the Spring mail sender, used to verify send attempts.
     */
    @Mock
    private JavaMailSender mailSender;

    /**
     * Instance of the verification service under test, with mocks injected.
     */
    @InjectMocks
    private EmailVerificationService emailVerificationService;

    /**
     * Instance of the password reset service under test, with mocks injected.
     */
    @InjectMocks
    private ResetPasswordEmailService resetPasswordEmailService;

    /**
     * A real MimeMessage instance, returned by the mocked mailSender, needed by MimeMessageHelper.
     */
    private MimeMessage mockMimeMessage;

    // --- Mock Data ---
    private LocalUser testUser;
    private VerificationToken verifToken;
    private ResetPasswordToken resetToken;
    private final String TEST_EMAIL = "test@example.com";
    private final String TEST_VERIF_TOKEN_STRING = "verif-token-123";
    private final String TEST_RESET_TOKEN_STRING = "reset-token-456";
    private final String TEST_FROM_EMAIL = "noreply@fluxdigitalstore.com";

    /**
     * Sets up the necessary mock objects and test data before each test method.
     * This includes creating a real MimeMessage, mocking its creation, initializing
     * the user/token data, and critically, using reflection to inject the 'fromAddress'.
     *
     * @throws Exception if reflection fails to access or set the field.
     */
    @BeforeEach
    void setUp() throws Exception {
        // MimeMessageHelper requires a real MimeMessage instance, which needs a Session.
        Session session = Session.getDefaultInstance(new Properties());
        mockMimeMessage = new MimeMessage(session);

        // Mock the creation of the MimeMessage
        when(mailSender.createMimeMessage()).thenReturn(mockMimeMessage);

        // Setup LocalUser mock data
        testUser = new LocalUser();
        testUser.setEmail(TEST_EMAIL);

        // Setup VerificationToken mock data
        verifToken = new VerificationToken();
        verifToken.setLocalUser(testUser);
        verifToken.setToken(TEST_VERIF_TOKEN_STRING);

        // Setup ResetPasswordToken mock data
        resetToken = new ResetPasswordToken();
        resetToken.setLocalUser(testUser);
        resetToken.setToken(TEST_RESET_TOKEN_STRING);

        // Inject the 'fromAddress' into the services using reflection to simulate @Value loading.
        setFromAddressViaReflection(emailVerificationService, TEST_FROM_EMAIL);
        setFromAddressViaReflection(resetPasswordEmailService, TEST_FROM_EMAIL);
    }

    /**
     * Uses reflection to inject the 'fromAddress' value into the BasicMailService's protected field.
     * This is necessary because the @Value annotation is not processed in Mockito unit tests,
     * which caused the "From address must not be null" exception.
     *
     * @param serviceInstance The service instance (EmailVerificationService or ResetPasswordEmailService).
     * @param fromAddress     The email address string to inject.
     * @throws Exception if the field cannot be found or accessed.
     */
    private void setFromAddressViaReflection(BasicMailService serviceInstance, String fromAddress) throws Exception {
        java.lang.reflect.Field field = BasicMailService.class.getDeclaredField("fromAddress");
        field.setAccessible(true);
        field.set(serviceInstance, fromAddress);
    }

    /**
     * Helper method to extract the raw content of the MimeMessage as a String for assertion.
     *
     * @param message The MimeMessage object sent during the test.
     * @return The raw content of the message as a UTF-8 String.
     * @throws Exception if reading the content fails.
     */
    private String getMimeMessageContent(MimeMessage message) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        message.writeTo(baos);
        return baos.toString("UTF-8");
    }

    // --------------------------------------------------------------
    // EmailVerificationService Tests
    // --------------------------------------------------------------

    /**
     * Tests successful sending of the email verification message.
     * Verifies that the mailSender.send() method is called once, and asserts the correctness
     * of the subject, recipient, sender, and the presence of key HTML content.
     */
    @Test
    void sendEmailConformationMessage_Success_MailSentAndContentIsHTML() throws Exception {
        ArgumentCaptor<MimeMessage> messageCaptor = ArgumentCaptor.forClass(MimeMessage.class);

        emailVerificationService.sendEmailConformationMessage(verifToken);

        verify(mailSender, times(1)).send(messageCaptor.capture());

        MimeMessage sentMessage = messageCaptor.getValue();

        assertEquals("Confirm Your Email for FluxDigitalStore", sentMessage.getSubject(), "Subject is incorrect.");
        assertEquals(TEST_EMAIL, ((InternetAddress) sentMessage.getAllRecipients()[0]).getAddress(), "Recipient email is incorrect.");
        assertEquals(TEST_FROM_EMAIL, ((InternetAddress) sentMessage.getFrom()[0]).getAddress(), "From email is incorrect.");

        String content = getMimeMessageContent(sentMessage);

        assertTrue(content.contains("<html>"), "Content should contain <html> tag indicating HTML format.");
        assertTrue(content.contains("Verify My Email"), "Content should contain the call-to-action button text.");
        assertTrue(content.contains(TEST_VERIF_TOKEN_STRING), "Content should contain the verification token.");
        assertTrue(content.contains("localhost:8080/auth/v1/verify"), "Content should contain the correct base URL.");
    }

    /**
     * Tests that a {@link MailException} thrown by the underlying {@link JavaMailSender}
     * is correctly wrapped and re-thrown as a {@link EmailFailureException}.
     */
    @Test
    void sendEmailConformationMessage_MailException_ThrowsEmailFailureException() {
        doThrow(mock(MailException.class)).when(mailSender).send(any(MimeMessage.class));

        assertThrows(EmailFailureException.class, () ->
                emailVerificationService.sendEmailConformationMessage(verifToken)
        );

        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }

    // --------------------------------------------------------------
    // ResetPasswordEmailService Tests
    // --------------------------------------------------------------

    /**
     * Tests successful sending of the reset password email message.
     * Verifies that the mailSender.send() method is called once, and asserts the correctness
     * of the subject, recipient, sender, and the presence of key HTML content.
     */
    @Test
    void sendResetPasswordEmail_Success_MailSentAndContentIsHTML() throws Exception {
        ArgumentCaptor<MimeMessage> messageCaptor = ArgumentCaptor.forClass(MimeMessage.class);

        resetPasswordEmailService.sendResetPasswordEmail(resetToken);

        verify(mailSender, times(1)).send(messageCaptor.capture());

        MimeMessage sentMessage = messageCaptor.getValue();

        assertEquals("🔑 Password Reset Request", sentMessage.getSubject(), "Subject is incorrect.");
        assertEquals(TEST_EMAIL, ((InternetAddress) sentMessage.getAllRecipients()[0]).getAddress(), "Recipient email is incorrect.");
        assertEquals(TEST_FROM_EMAIL, ((InternetAddress) sentMessage.getFrom()[0]).getAddress(), "From email is incorrect.");

        String content = getMimeMessageContent(sentMessage);

        assertTrue(content.contains("<html>"), "Content should contain <html> tag indicating HTML format.");
        assertTrue(content.contains("Reset Password"), "Content should contain the call-to-action button text.");
        assertTrue(content.contains(TEST_RESET_TOKEN_STRING), "Content should contain the reset token.");
        assertTrue(content.contains("http://localhost:8080/auth/v1/reset_password"), "Content should contain the correct base URL.");
    }

    /**
     * Tests that a {@link MailException} thrown by the underlying {@link JavaMailSender}
     * during a password reset attempt is correctly wrapped and re-thrown as a {@link EmailFailureException}.
     */
    @Test
    void sendResetPasswordEmail_MailException_ThrowsEmailFailureException() {
        doThrow(mock(MailException.class)).when(mailSender).send(any(MimeMessage.class));

        assertThrows(EmailFailureException.class, () ->
                resetPasswordEmailService.sendResetPasswordEmail(resetToken)
        );

        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }
}