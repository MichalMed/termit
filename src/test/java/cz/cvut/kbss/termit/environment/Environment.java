package cz.cvut.kbss.termit.environment;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.cvut.kbss.jsonld.ConfigParam;
import cz.cvut.kbss.jsonld.JsonLd;
import cz.cvut.kbss.jsonld.jackson.JsonLdModule;
import cz.cvut.kbss.termit.model.User;
import cz.cvut.kbss.termit.security.model.AuthenticationToken;
import cz.cvut.kbss.termit.security.model.UserDetails;
import cz.cvut.kbss.termit.util.Constants;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.ResourceHttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;

import java.nio.charset.Charset;
import java.security.Principal;
import java.util.Collections;
import java.util.HashSet;

public class Environment {

    private static User currentUser;

    private static ObjectMapper objectMapper;

    /**
     * Initializes security context with the specified user.
     *
     * @param user User to set as currently authenticated
     */
    public static void setCurrentUser(User user) {
        currentUser = user;
        final UserDetails userDetails = new UserDetails(user, new HashSet<>());
        SecurityContext context = new SecurityContextImpl();
        context.setAuthentication(new AuthenticationToken(userDetails.getAuthorities(), userDetails));
        SecurityContextHolder.setContext(context);
    }

    /**
     * Gets current user as security principal.
     *
     * @return Current user authentication as principal or {@code null} if there is no current user
     */
    public static Principal getCurrentUserPrincipal() {
        return SecurityContextHolder.getContext() != null ? SecurityContextHolder.getContext().getAuthentication() :
               null;
    }

    public static User getCurrentUser() {
        return currentUser;
    }

    /**
     * Gets a Jackson object mapper for mapping JSON to Java and vice versa.
     *
     * @return Object mapper
     */
    public static ObjectMapper getObjectMapper() {
        if (objectMapper == null) {
            objectMapper = new ObjectMapper();
            objectMapper.configure(JsonParser.Feature.AUTO_CLOSE_SOURCE, true);
            objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
            objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        }
        return objectMapper;
    }

    /**
     * Creates a Jackson JSON-LD message converter.
     *
     * @return JSON-LD message converter
     */
    public static HttpMessageConverter<?> createJsonLdMessageConverter() {
        final ObjectMapper mapper = new ObjectMapper();
        mapper.configure(JsonParser.Feature.AUTO_CLOSE_SOURCE, true);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        final JsonLdModule module = new JsonLdModule();
        module.configure(ConfigParam.SCAN_PACKAGE, "cz.cvut.kbss.termit");
        mapper.registerModule(module);
        final MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter(mapper);
        converter.setSupportedMediaTypes(Collections.singletonList(MediaType.valueOf(JsonLd.MEDIA_TYPE)));
        return converter;
    }

    public static HttpMessageConverter<?> createDefaultMessageConverter() {
        return new MappingJackson2HttpMessageConverter(getObjectMapper());
    }

    public static HttpMessageConverter<?> createStringEncodingMessageConverter() {
        return new StringHttpMessageConverter(Charset.forName(Constants.UTF_8_ENCODING));
    }

    public static HttpMessageConverter<?> createResourceMessageConverter() {
        return new ResourceHttpMessageConverter();
    }
}
