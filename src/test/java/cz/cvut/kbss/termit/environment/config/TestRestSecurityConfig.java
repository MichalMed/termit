package cz.cvut.kbss.termit.environment.config;

import cz.cvut.kbss.termit.environment.Environment;
import cz.cvut.kbss.termit.security.HttpAuthenticationEntryPoint;
import cz.cvut.kbss.termit.security.JwtAuthenticationFilter;
import cz.cvut.kbss.termit.security.JwtAuthorizationFilter;
import cz.cvut.kbss.termit.security.JwtUtils;
import cz.cvut.kbss.termit.service.security.SecurityUtils;
import cz.cvut.kbss.termit.service.security.TermItUserDetailsService;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

/**
 * This configuration class is necessary when testing security of REST controllers (e.g., {@link
 * cz.cvut.kbss.termit.rest.UserController}).
 */
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class TestRestSecurityConfig extends WebSecurityConfigurerAdapter {

    private AuthenticationEntryPoint authenticationEntryPoint = new HttpAuthenticationEntryPoint();

    @Mock
    private AuthenticationFailureHandler authenticationFailureHandler;

    @Mock
    private AuthenticationSuccessHandler authenticationSuccessHandler;

    @Mock
    private AuthenticationProvider authenticationProvider;

    @Mock
    private TermItUserDetailsService userDetailsService;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private SecurityUtils securityUtils;

    protected TestRestSecurityConfig() {
        MockitoAnnotations.initMocks(this);
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.authenticationProvider(authenticationProvider);
    }

    @Bean
    @Override
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.authorizeRequests().anyRequest().permitAll().and()
            .exceptionHandling().authenticationEntryPoint(authenticationEntryPoint)
            .and().cors().and().csrf().disable()
            .addFilter(new JwtAuthenticationFilter(authenticationManager(), jwtUtils))
            .addFilter(new JwtAuthorizationFilter(authenticationManager(), jwtUtils, securityUtils, userDetailsService,
                    Environment.getObjectMapper()))
            .formLogin().successHandler(authenticationSuccessHandler)
            .failureHandler(authenticationFailureHandler).and().sessionManagement()
            .sessionCreationPolicy(SessionCreationPolicy.STATELESS);
    }
}
