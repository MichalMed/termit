package cz.cvut.kbss.termit.service.security;

import cz.cvut.kbss.termit.exception.ValidationException;
import cz.cvut.kbss.termit.model.UserAccount;
import cz.cvut.kbss.termit.security.model.AuthenticationToken;
import cz.cvut.kbss.termit.security.model.TermItUserDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Optional;

/**
 * Handle user session-related functions.
 */
@Service
public class SecurityUtils {

    private final UserDetailsService userDetailsService;

    private final PasswordEncoder passwordEncoder;

    @Autowired
    public SecurityUtils(UserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
        this.userDetailsService = userDetailsService;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * This is a statically accessible variant of the {@link #getCurrentUser()} method.
     * <p>
     * It allows to access the currently logged in user without injecting {@code SecurityUtils} as a bean.
     *
     * @return Currently logged in user
     */
    public static UserAccount currentUser() {
        final SecurityContext context = SecurityContextHolder.getContext();
        assert context != null;
        final TermItUserDetails userDetails = (TermItUserDetails) context.getAuthentication().getDetails();
        return userDetails.getUser();
    }

    /**
     * Gets the currently authenticated user.
     *
     * @return Current user
     */
    public UserAccount getCurrentUser() {
        return currentUser();
    }

    /**
     * Gets details of the currently authenticated user.
     * <p>
     * If no user is logged in, an empty {@link Optional} is returned.
     *
     * @return Currently authenticated user details
     */
    public Optional<TermItUserDetails> getCurrentUserDetails() {
        final SecurityContext context = SecurityContextHolder.getContext();
        if (context.getAuthentication() != null && context.getAuthentication().getDetails() instanceof TermItUserDetails) {
            return Optional.of((TermItUserDetails) context.getAuthentication().getDetails());
        } else {
            return Optional.empty();
        }
    }

    /**
     * Checks if a user is currently authenticated, or if the current thread is processing a request from an anonymous
     * user.
     *
     * @return Whether a user is authenticated
     */
    public boolean isAuthenticated() {
        final SecurityContext context = SecurityContextHolder.getContext();
        return context.getAuthentication() != null && context.getAuthentication().getDetails() instanceof TermItUserDetails;
    }

    /**
     * Creates an authentication token based on the specified user details and sets it to the current thread's security
     * context.
     *
     * @param userDetails Details of the user to set as current
     * @return The generated authentication token
     */
    public AuthenticationToken setCurrentUser(TermItUserDetails userDetails) {
        final AuthenticationToken token = new AuthenticationToken(userDetails.getAuthorities(), userDetails);
        token.setAuthenticated(true);

        final SecurityContext context = new SecurityContextImpl();
        context.setAuthentication(token);
        SecurityContextHolder.setContext(context);
        return token;
    }

    /**
     * Reloads the current user's data from the database.
     */
    public void updateCurrentUser() {
        final TermItUserDetails updateDetails =
                (TermItUserDetails) userDetailsService.loadUserByUsername(getCurrentUser().getUsername());
        setCurrentUser(updateDetails);
    }

    /**
     * Checks that the specified password corresponds to the current user's password.
     *
     * @param password The password to verify
     * @throws IllegalArgumentException When the password's do not match
     */
    public void verifyCurrentUserPassword(String password) {
        final UserAccount currentUser = getCurrentUser();
        if (!passwordEncoder.matches(password, currentUser.getPassword())) {
            throw new ValidationException("The specified password does not match the original one.");
        }
    }

    /**
     * Verifies that the specified user is enabled and not locked.
     *
     * @param user User to check
     */
    public static void verifyAccountStatus(UserAccount user) {
        Objects.requireNonNull(user);
        if (user.isLocked()) {
            throw new LockedException("Account of user " + user + " is locked.");
        }
        if (!user.isEnabled()) {
            throw new DisabledException("Account of user " + user + " is disabled.");
        }
    }
}
