package com.artemhontar.fluxdigitalstore.service;

import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class ValidationErrorsParser {

    /**
     * Parses BindingResult and returns a map of field names to a list of error messages for each field.
     * Example: { "fieldName": ["Error message 1", "Error message 2"] }
     */
    public Map<String, List<String>> parseErrorsFrom(BindingResult bindingResult) {
        Map<String, List<String>> errorsMap = new HashMap<>();

        for (FieldError fieldError : bindingResult.getFieldErrors()) {
            // Use computeIfAbsent for cleaner code
            errorsMap.computeIfAbsent(fieldError.getField(), k -> new ArrayList<>())
                    .add(fieldError.getDefaultMessage());
        }
        return errorsMap;
    }

    /**
     * Creates a consistent error response structure for API clients.
     * Example: { "code": "ERROR_CODE", "message": "User-friendly message" }
     *
     * @param code    A unique error code for programmatic identification.
     * @param message A user-friendly message explaining the error.
     * @return A map representing the error response.
     */
    public Map<String, Object> createErrorResponse(String code, String message) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("code", code);
        errorResponse.put("message", message);
        return errorResponse;
    }

    /**
     * Overloaded method to create an error response that includes detailed validation errors.
     * Example: { "code": "VALIDATION_FAILED", "message": "Input validation failed.", "details": { "field1": ["error1"] } }
     *
     * @param code    A unique error code.
     * @param message A user-friendly message.
     * @param details A map of detailed validation errors (field name to list of messages).
     * @return A map representing the error response with validation details.
     */
    public Map<String, Object> createErrorResponse(String code, String message, Map<String, List<String>> details) {
        Map<String, Object> errorResponse = createErrorResponse(code, message); // Reuses the basic error creation
        errorResponse.put("details", details); // Add the parsed validation errors here
        return errorResponse;
    }
}
