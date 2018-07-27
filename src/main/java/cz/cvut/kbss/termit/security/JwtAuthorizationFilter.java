package cz.cvut.kbss.termit.security;

import cz.cvut.kbss.termit.security.model.UserDetails;
import cz.cvut.kbss.termit.service.security.SecurityUtils;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * This filter retrieves JWT from the incoming request and validates it, ensuring that the user is authorized to access
 * the application.
 */
public class JwtAuthorizationFilter extends BasicAuthenticationFilter {

    private final JwtUtils jwtUtils;

    private final SecurityUtils securityUtils;

    // TODO The filter should eventually also load the current user record from repository and verify that the account is not disabled/locked

    public JwtAuthorizationFilter(AuthenticationManager authenticationManager, JwtUtils jwtUtils,
                                  SecurityUtils securityUtils) {
        super(authenticationManager);
        this.jwtUtils = jwtUtils;
        this.securityUtils = securityUtils;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        final String authHeader = request.getHeader(SecurityConstants.AUTHENTICATION_HEADER);
        if (authHeader == null || !authHeader.startsWith(SecurityConstants.JWT_TOKEN_PREFIX)) {
            chain.doFilter(request, response);
            return;
        }
        final String authToken = authHeader.substring(SecurityConstants.JWT_TOKEN_PREFIX.length());
        final UserDetails userDetails = jwtUtils.extractUserInfo(authToken);
        securityUtils.setCurrentUser(userDetails);
        refreshToken(authToken, response);

        chain.doFilter(request, response);
    }

    private void refreshToken(String authToken, HttpServletResponse response) {
        final String newToken = jwtUtils.refreshToken(authToken);
        response.setHeader(SecurityConstants.AUTHENTICATION_HEADER, SecurityConstants.JWT_TOKEN_PREFIX + newToken);
    }
}
