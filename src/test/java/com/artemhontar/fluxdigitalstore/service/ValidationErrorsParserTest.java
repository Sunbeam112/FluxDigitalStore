package com.artemhontar.fluxdigitalstore.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ValidationErrorsParserTest {

    @InjectMocks
    private ValidationErrorsParser parser;

    @Mock
    private BindingResult bindingResult;

    // --- Mocks for FieldError objects ---
    // Note: We use concrete FieldError objects because Mockito setup for simple getters is straightforward
    private final FieldError emailError1 = new FieldError("objectName", "email", "Email cannot be empty");
    private final FieldError emailError2 = new FieldError("objectName", "email", "Must be a valid format");
    private final FieldError passwordError = new FieldError("objectName", "password", "Password must be at least 8 characters");

    // ===================================
    // TEST: parseErrorsFrom
    // ===================================

    @Test
    void parseErrorsFrom_MultipleFieldErrors_ReturnsCorrectlyStructuredMap() {
        // ARRANGE
        List<FieldError> fieldErrors = Arrays.asList(emailError1, emailError2, passwordError);
        when(bindingResult.getFieldErrors()).thenReturn(fieldErrors);

        // ACT
        Map<String, List<String>> errorsMap = parser.parseErrorsFrom(bindingResult);

        // ASSERT
        assertNotNull(errorsMap, "The errors map should not be null.");
        assertEquals(2, errorsMap.size(), "There should be 2 unique fields with errors (email, password).");

        // Verify email field errors
        assertTrue(errorsMap.containsKey("email"));
        List<String> emailErrors = errorsMap.get("email");
        assertEquals(2, emailErrors.size(), "Email field should have 2 errors.");
        assertTrue(emailErrors.contains("Email cannot be empty"));
        assertTrue(emailErrors.contains("Must be a valid format"));

        // Verify password field errors
        assertTrue(errorsMap.containsKey("password"));
        List<String> passwordErrors = errorsMap.get("password");
        assertEquals(1, passwordErrors.size(), "Password field should have 1 error.");
        assertTrue(passwordErrors.contains("Password must be at least 8 characters"));
    }

    @Test
    void parseErrorsFrom_NoErrors_ReturnsEmptyMap() {
        // ARRANGE
        when(bindingResult.getFieldErrors()).thenReturn(Collections.emptyList());

        // ACT
        Map<String, List<String>> errorsMap = parser.parseErrorsFrom(bindingResult);

        // ASSERT
        assertNotNull(errorsMap);
        assertTrue(errorsMap.isEmpty(), "The map should be empty when there are no errors.");
    }

    // ===================================
    // TEST: createErrorResponse
    // ===================================

    @Test
    void createErrorResponse_Basic_ReturnsMapWithCodeAndMessage() {
        // ARRANGE
        String code = "NOT_FOUND";
        String message = "The requested resource was not found.";

        // ACT
        Map<String, Object> response = parser.createErrorResponse(code, message);

        // ASSERT
        assertEquals(2, response.size());
        assertEquals(code, response.get("code"));
        assertEquals(message, response.get("message"));
    }

    @Test
    void createErrorResponse_WithDetails_ReturnsMapWithDetailsKey() {
        // ARRANGE
        String code = "VALIDATION_FAILED";
        String message = "Input validation failed on several fields.";
        Map<String, List<String>> details = Map.of(
                "name", List.of("Name is required"),
                "age", List.of("Age must be positive")
        );

        // ACT
        Map<String, Object> response = parser.createErrorResponse(code, message, details);

        // ASSERT
        assertEquals(3, response.size()); // code, message, details
        assertEquals(code, response.get("code"));
        assertEquals(message, response.get("message"));
        assertTrue(response.containsKey("details"));

        // Verify the details map is correctly embedded
        @SuppressWarnings("unchecked")
        Map<String, List<String>> returnedDetails = (Map<String, List<String>>) response.get("details");
        assertEquals(2, returnedDetails.size());
        assertTrue(returnedDetails.get("name").contains("Name is required"));
    }
}
