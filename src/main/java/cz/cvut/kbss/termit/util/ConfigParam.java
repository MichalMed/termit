package cz.cvut.kbss.termit.util;

/**
 * Application configuration parameters, loaded from {@code config.properties} provided on classpath.
 */
public enum ConfigParam {

    /**
     * URL of the main application repository.
     */
    REPOSITORY_URL("repository.url"),

    /**
     * OntoDriver class for the repository.
     */
    DRIVER("persistence.driver"),

    /**
     * Language used to store strings in the repository (persistence unit language).
     */
    LANGUAGE("persistence.language"),

    /**
     * Username for connecting to the application repository.
     */
    REPO_USERNAME("repository.username"),

    /**
     * Password for connecting to the application repository.
     */
    REPO_PASSWORD("repository.password"),

    /**
     * Secret key used when hashing a JWT.
     */
    JWT_SECRET_KEY("jwt.secretKey"),

    /**
     * Base for possible generation of vocabulary identifier.
     *
     * This allows each deployment to specify base IRI from which vocabulary identifiers will be derived.
     */
    VOCABULARY_BASE_IRI("vocabulary.baseIri");

    private final String parameter;

    ConfigParam(String parameter) {
        this.parameter = parameter;
    }

    @Override
    public String toString() {
        return parameter;
    }
}