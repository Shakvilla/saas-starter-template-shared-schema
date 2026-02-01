package cloud.norgha.multi_tenant_saas_starter_template.infrastructure.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validates password strength according to enterprise security standards.
 */
public class PasswordValidator implements ConstraintValidator<StrongPassword, String> {

    private static final int MIN_LENGTH = 8;
    private static final String UPPERCASE_PATTERN = ".*[A-Z].*";
    private static final String LOWERCASE_PATTERN = ".*[a-z].*";
    private static final String DIGIT_PATTERN = ".*[0-9].*";
    private static final String SPECIAL_CHAR_PATTERN = ".*[!@#$%^&*(),.?\":{}|<>].*";

    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        if (password == null || password.isBlank()) {
            return false;
        }

        if (password.length() < MIN_LENGTH) {
            setCustomMessage(context, "Password must be at least " + MIN_LENGTH + " characters");
            return false;
        }

        if (!password.matches(UPPERCASE_PATTERN)) {
            setCustomMessage(context, "Password must contain at least one uppercase letter");
            return false;
        }

        if (!password.matches(LOWERCASE_PATTERN)) {
            setCustomMessage(context, "Password must contain at least one lowercase letter");
            return false;
        }

        if (!password.matches(DIGIT_PATTERN)) {
            setCustomMessage(context, "Password must contain at least one digit");
            return false;
        }

        if (!password.matches(SPECIAL_CHAR_PATTERN)) {
            setCustomMessage(context, "Password must contain at least one special character (!@#$%^&*(),.?\":{}|<>)");
            return false;
        }

        return true;
    }

    private void setCustomMessage(ConstraintValidatorContext context, String message) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
    }
}
