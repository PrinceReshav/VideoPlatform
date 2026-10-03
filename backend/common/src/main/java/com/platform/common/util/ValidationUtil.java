package com.platform.common.util;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ValidationUtil {

    private static final Validator VALIDATOR;

    static {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        VALIDATOR = factory.getValidator();
    }

    public static <T> Map<String, String> validate(T object) {
        Set<ConstraintViolation<T>> violations = VALIDATOR.validate(object);
        Map<String, String> errors = new HashMap<>();
        for (ConstraintViolation<T> violation : violations) {
            String fieldName = violation.getPropertyPath().toString();
            errors.put(fieldName, violation.getMessage());
        }
        return errors;
    }

    public static <T> void validateAndThrow(T object) {
        Map<String, String> errors = validate(object);
        if (!errors.isEmpty()) {
            throw new com.platform.common.exception.ValidationException(
                    "Validation failed", errors);
        }
    }

    public static boolean isValidEmail(String email) {
        if (email == null) return false;
        return email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }

    public static boolean isValidUsername(String username) {
        if (username == null) return false;
        // 3-30 chars, alphanumeric, underscore, hyphen
        return username.matches("^[a-zA-Z0-9_-]{3,30}$");
    }

    public static boolean isStrongPassword(String password) {
        if (password == null || password.length() < 8) return false;
        boolean hasUpper = password.matches(".*[A-Z].*");
        boolean hasLower = password.matches(".*[a-z].*");
        boolean hasDigit = password.matches(".*\\d.*");
        boolean hasSpecial = password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*");
        return hasUpper && hasLower && hasDigit && hasSpecial;
    }

    public static boolean isValidUrl(String url) {
        if (url == null) return false;
        try {
            new java.net.URL(url).toURI();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}