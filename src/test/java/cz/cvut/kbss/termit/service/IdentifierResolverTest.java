package cz.cvut.kbss.termit.service;

import cz.cvut.kbss.termit.environment.Generator;
import cz.cvut.kbss.termit.environment.PropertyMockingApplicationContextInitializer;
import cz.cvut.kbss.termit.util.ConfigParam;
import cz.cvut.kbss.termit.util.Vocabulary;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.context.ContextConfiguration;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ContextConfiguration(initializers = PropertyMockingApplicationContextInitializer.class)
class IdentifierResolverTest extends BaseServiceTestRunner {

    @Autowired
    private MockEnvironment environment;

    @Autowired
    private IdentifierResolver sut;

    @Test
    void normalizeTransformsValueToLowerCase() {
        final String value = "CapitalizedSTring";
        assertEquals(value.toLowerCase(), IdentifierResolver.normalize(value));
    }

    @Test
    void normalizeTrimsValue() {
        final String value = "   DDD   ";
        assertEquals(value.trim().toLowerCase(), IdentifierResolver.normalize(value));
    }

    @Test
    void normalizeReplacesSpacesWithDashes() {
        final String value = "Catherine Halsey";
        assertEquals("catherine-halsey", IdentifierResolver.normalize(value));
    }

    @Test
    void normalizeChangesCzechAccutesToAsciiCharacters() {
        final String value = "Strukturální Plán";
        assertEquals("strukturalni-plan", IdentifierResolver.normalize(value));
    }

    @Test
    void normalizeChangesCzechAdornmentsToAsciiCharacters() {
        final String value = "předzahrádka";
        assertEquals("predzahradka", IdentifierResolver.normalize(value));
    }

    @Test
    void generateIdentifierAppendsNormalizedComponentsToSpecifiedNamespace() {
        final String namespace = "http://onto.fel.cvut.cz/ontologies/termit/";
        final String compOne = "Catherine";
        final String compTwo = "Halsey";
        final String result = sut.generateIdentifier(namespace, compOne, compTwo).toString();
        assertEquals(namespace + "catherine-halsey", result);
    }

    @Test
    void generateIdentifierAppendsSlashWhenNamespaceDoesNotEndWithIt() {
        final String namespace = "http://onto.fel.cvut.cz/ontologies/termit";
        final String compOne = "Catherine";
        final String compTwo = "Halsey";
        final String result = sut.generateIdentifier(namespace, compOne, compTwo).toString();
        assertEquals(namespace + "/catherine-halsey", result);
    }

    @Test
    void generateIdentifierDoesNotAppendSlashWhenNamespaceEndsWithHashTag() {
        final String namespace = "http://onto.fel.cvut.cz/ontologies/termit#";
        final String compOne = "Catherine";
        final String compTwo = "Halsey";
        final String result = sut.generateIdentifier(namespace, compOne, compTwo).toString();
        assertEquals(namespace + "catherine-halsey", result);
    }

    @Test
    void generateIdentifierThrowsIllegalArgumentWhenNoComponentsAreProvided() {
        final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> sut.generateIdentifier(Vocabulary.ONTOLOGY_IRI_termit));
        assertEquals("Must provide at least one component for identifier generation.", ex.getMessage());
    }

    @Test
    void generateIdentifierAppendsNormalizedComponentsToNamespaceLoadedFromConfig() {
        final String namespace = "http://onto.fel.cvut.cz/ontologies/termit/vocabulary/";
        final String comp = "Metropolitan Plan";
        environment.setProperty(ConfigParam.NAMESPACE_VOCABULARY.toString(), namespace);
        final String result = sut.generateIdentifier(ConfigParam.NAMESPACE_VOCABULARY, comp).toString();
        assertEquals(namespace + "metropolitan-plan", result);
    }

    @Test
    void resolveIdentifierAppendsFragmentToSpecifiedNamespace() {
        final String namespace = "http://onto.fel.cvut.cz/ontologies/termit/vocabulary/";
        final String fragment = "metropolitan-plan";
        assertEquals(namespace + fragment, sut.resolveIdentifier(namespace, fragment).toString());
    }

    @Test
    void resolveIdentifierAppendsSlashAndFragmentIfNamespaceDoesNotEndWithOne() {
        final String namespace = "http://onto.fel.cvut.cz/ontologies/termit/vocabulary";
        final String fragment = "metropolitan-plan";
        assertEquals(namespace + "/" + fragment, sut.resolveIdentifier(namespace, fragment).toString());
    }

    @Test
    void resolveIdentifierDoesNotAppendSlashIfNamespaceEndsWithHashTag() {
        final String namespace = "http://onto.fel.cvut.cz/ontologies/termit/vocabulary#";
        final String fragment = "metropolitan-plan";
        assertEquals(namespace + fragment, sut.resolveIdentifier(namespace, fragment).toString());
    }

    @Test
    void resolveIdentifierAppendsFragmentToNamespaceLoadedFromConfiguration() {
        final String namespace = "http://onto.fel.cvut.cz/ontologies/termit/vocabulary/";
        final String fragment = "metropolitan-plan";
        environment.setProperty(ConfigParam.NAMESPACE_VOCABULARY.toString(), namespace);
        assertEquals(namespace + fragment,
                sut.resolveIdentifier(ConfigParam.NAMESPACE_VOCABULARY, fragment).toString());
    }

    @Test
    void extractIdentifierFragmentExtractsLastPartOfUri() {
        final URI uri = Generator.generateUri();
        final String result = sut.extractIdentifierFragment(uri);
        assertEquals(uri.toString().substring(uri.toString().lastIndexOf('/') + 1), result);
    }

    @Test
    void extractIdentifierFragmentExtractsFragmentFromUriWithUrlFragment() {
        final URI uri = URI.create("http://onto.fel.cvut.cz/ontologies/termit/vocabulary#test");
        assertEquals("test", sut.extractIdentifierFragment(uri));
    }
}