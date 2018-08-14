package cz.cvut.kbss.termit.util;

import javax.validation.ConstraintViolation;
import java.io.Serializable;
import java.util.Collection;

/**
 * Represents a result of validation using a JSR 380 {@link javax.validation.Validator}.
 * <p>
 * The main reason for the existence of this class is that Java generics are not able to cope with the set of constraint
 * violations of some type produced by the validator.
 *
 * @param <T> The validated type
 */
public class ValidationResult<T> implements Serializable {

    private final Collection<ConstraintViolation<T>> violations;

    private ValidationResult(Collection<ConstraintViolation<T>> violations) {
        this.violations = violations;
    }

    public Collection<ConstraintViolation<T>> getViolations() {
        return violations;
    }

    public boolean isValid() {
        return violations.isEmpty();
    }

    public static <T> ValidationResult<T> of(Collection<ConstraintViolation<T>> violations) {
        return new ValidationResult<>(violations);
    }
}
