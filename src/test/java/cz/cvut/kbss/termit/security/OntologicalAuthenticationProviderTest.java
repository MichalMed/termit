package cz.cvut.kbss.termit.security;

import cz.cvut.kbss.termit.environment.Generator;
import cz.cvut.kbss.termit.environment.config.TestSecurityConfig;
import cz.cvut.kbss.termit.event.LoginFailureEvent;
import cz.cvut.kbss.termit.event.LoginSuccessEvent;
import cz.cvut.kbss.termit.model.User;
import cz.cvut.kbss.termit.persistence.dao.UserDao;
import cz.cvut.kbss.termit.security.model.UserDetails;
import cz.cvut.kbss.termit.service.BaseServiceTestRunner;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ContextConfiguration;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

@Configuration
@Tag("security")
@ContextConfiguration(classes = {TestSecurityConfig.class, OntologicalAuthenticationProviderTest.class})
class OntologicalAuthenticationProviderTest extends BaseServiceTestRunner {

    @Bean
    public Listener listener() {
        return spy(new Listener());
    }

    @Autowired
    private AuthenticationProvider provider;

    @Autowired
    private UserDao userDao;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private Listener listener;

    private User user;
    private String plainPassword;

    @BeforeEach
    void setUp() {
        this.user = Generator.generateUserWithId();
        this.plainPassword = user.getPassword();
        user.setPassword(passwordEncoder.encode(plainPassword));
        transactional(() -> userDao.persist(user));
        SecurityContextHolder.setContext(new SecurityContextImpl());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.setContext(new SecurityContextImpl());
    }

    @Test
    void successfulAuthenticationSetsSecurityContext() {
        final Authentication auth = authentication(user.getUsername(), plainPassword);
        final SecurityContext context = SecurityContextHolder.getContext();
        assertNull(context.getAuthentication());
        final Authentication result = provider.authenticate(auth);
        assertNotNull(SecurityContextHolder.getContext());
        final UserDetails details = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getDetails();
        assertEquals(user.getUsername(), details.getUsername());
        assertTrue(result.isAuthenticated());
    }

    private static Authentication authentication(String username, String password) {
        return new UsernamePasswordAuthenticationToken(username, password);
    }

    @Test
    void authenticateThrowsUserNotFoundExceptionForUnknownUsername() {
        final Authentication auth = authentication("unknownUsername", user.getPassword());
        assertThrows(UsernameNotFoundException.class, () -> provider.authenticate(auth));
        final SecurityContext context = SecurityContextHolder.getContext();
        assertNull(context.getAuthentication());
    }

    @Test
    void authenticateThrowsBadCredentialsForInvalidPassword() {
        final Authentication auth = authentication(user.getUsername(), "unknownPassword");
        assertThrows(BadCredentialsException.class, () -> provider.authenticate(auth));
        final SecurityContext context = SecurityContextHolder.getContext();
        assertNull(context.getAuthentication());
    }

    @Test
    void supportsUsernameAndPasswordAuthentication() {
        assertTrue(provider.supports(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    void authenticateThrowsAuthenticationExceptionForEmptyUsername() {
        final Authentication auth = authentication("", "");
        final UsernameNotFoundException ex = assertThrows(UsernameNotFoundException.class,
                () -> provider.authenticate(auth));
        assertThat(ex.getMessage(), containsString("Username cannot be empty."));
    }

    @Test
    void successfulLoginEmitsLoginSuccessEvent() {
        final Authentication auth = authentication(user.getUsername(), plainPassword);
        provider.authenticate(auth);
        verify(listener).onSuccess(any());
        assertEquals(user, listener.user);
    }

    @Test
    void failedLoginEmitsLoginFailureEvent() {
        final Authentication auth = authentication(user.getUsername(), "unknownPassword");
        assertThrows(BadCredentialsException.class, () -> provider.authenticate(auth));
        verify(listener).onFailure(any());
        assertEquals(user, listener.user);
    }

    @Test
    void authenticateThrowsLockedExceptionForLockedUser() {
        user.lock();
        transactional(() -> userDao.update(user));
        final Authentication auth = authentication(user.getUsername(), plainPassword);
        final LockedException ex = assertThrows(LockedException.class, () -> provider.authenticate(auth));
        assertEquals("Account of user " + user + " is locked.", ex.getMessage());
    }

    @Test
    void authenticationThrowsDisabledExceptionForDisabledUser() {
        user.disable();
        transactional(() -> userDao.update(user));
        final Authentication auth = authentication(user.getUsername(), plainPassword);
        final DisabledException ex = assertThrows(DisabledException.class, () -> provider.authenticate(auth));
        assertEquals("Account of user " + user + " is disabled.", ex.getMessage());
    }

    public static class Listener {

        private User user;

        @EventListener
        public void onSuccess(LoginSuccessEvent event) {
            this.user = event.getUser();
        }

        @EventListener
        public void onFailure(LoginFailureEvent event) {
            this.user = event.getUser();
        }
    }
}