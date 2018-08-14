package cz.cvut.kbss.termit.exception;

/**
 * General exception for issues with JSON Web Tokens.
 */
public class JwtException extends TermItException {

    public JwtException(String message) {
        super(message);
    }

    public JwtException(String message, Throwable cause) {
        super(message, cause);
    }
}