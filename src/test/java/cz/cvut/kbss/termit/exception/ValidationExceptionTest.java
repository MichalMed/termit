package cz.cvut.kbss.termit.exception;

import cz.cvut.kbss.termit.model.User;
import cz.cvut.kbss.termit.util.ValidationResult;
import org.junit.jupiter.api.Test;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ValidationExceptionTest {

    @Test
    void getMessageReturnsMessageWhenItWasSpecified() {
        final String msg = "I have a bad feeling about this.";
        final ValidationException ex = new ValidationException(msg);
        assertEquals(msg, ex.getMessage());
    }

    @Test
    void getMessageReturnsConcatenatedConstraintViolationMessages() {
        final User u = new User();
        u.setFirstName("test");
        u.setLastName("testowitch");
        final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        final ValidationResult<User> violations = ValidationResult.of(validator.validate(u));
        final ValidationException ex = new ValidationException(violations);
        final String result = ex.getMessage();
        assertAll(() -> assertThat(result, containsString("username")),
                () -> assertThat(result, containsString("password")));
    }
}