package cz.cvut.kbss.termit.persistence.dao;

import cz.cvut.kbss.jopa.model.EntityManager;
import cz.cvut.kbss.jopa.vocabulary.RDFS;
import cz.cvut.kbss.termit.model.Term;
import cz.cvut.kbss.termit.model.Vocabulary;
import cz.cvut.kbss.termit.util.ConfigParam;
import cz.cvut.kbss.termit.util.Configuration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.net.URI;
import java.util.List;
import java.util.Objects;

@Repository
public class TermDao extends BaseDao<Term> {

    private final Configuration config;

    @Autowired
    public TermDao(EntityManager em, Configuration config) {
        super(Term.class, em);
        this.config = config;
    }

    /**
     * Gets all terms on the specified vocabulary.
     * <p>
     * No differences are made between root terms and terms with parents.
     *
     * @param vocabulary Vocabulary whose terms should be returned
     * @return Matching terms, ordered by label
     */
    public List<Term> findAll(Vocabulary vocabulary) {
        Objects.requireNonNull(vocabulary);
        return em.createNativeQuery("SELECT DISTINCT ?term WHERE {" +
                "?term a ?type ;" +
                "rdfs:label ?label ;" +
                "?inVocabulary ?vocabulary ." +
                "} ORDER BY ?label", Term.class)
                 .setParameter("type", typeUri)
                 .setParameter("vocabulary", vocabulary.getUri())
                 .setParameter("inVocabulary",
                         URI.create(cz.cvut.kbss.termit.util.Vocabulary.s_p_je_pojmem_ze_slovniku))
                 .getResultList();
    }

    /**
     * Loads a page of terms contained in the specified vocabulary.
     *
     * @param vocabulary Vocabulary whose terms should be returned
     * @param pageSpec   Page specification
     * @return Matching terms, ordered by their label
     */
    public List<Term> findAllRoots(Vocabulary vocabulary, Pageable pageSpec) {
        Objects.requireNonNull(vocabulary);
        Objects.requireNonNull(pageSpec);
        return em.createNativeQuery("SELECT DISTINCT ?term WHERE {" +
                "?term a ?type ;" +
                "rdfs:label ?label ." +
                "?vocabulary ?hasGlossary/?hasTerm ?term ." +
                "} ORDER BY ?label OFFSET ?offset LIMIT ?limit", Term.class)
                 .setParameter("type", typeUri)
                 .setParameter("hasGlossary", URI.create(cz.cvut.kbss.termit.util.Vocabulary.s_p_ma_glosar))
                 .setParameter("hasTerm", URI.create(cz.cvut.kbss.termit.util.Vocabulary.s_p_obsahuje_pojem))
                 .setParameter("vocabulary", vocabulary.getUri())
                 .setUntypedParameter("offset", pageSpec.getOffset())
                 .setUntypedParameter("limit", pageSpec.getPageSize())
                 .getResultList();
    }

    /**
     * Finds root terms whose term subtree contains a term with label matching the specified search string.
     * <p>
     * Currently, the match uses SPARQL {@code contains} function on lowercase label and search string. A more
     * sophisticated matching can be added.
     *
     * @param searchString String to search term labels by
     * @param vocabulary   Vocabulary whose terms should be returned
     * @return List of root terms contain a matching term in subtree
     */
    public List<Term> findAllRoots(String searchString, Vocabulary vocabulary) {
        return em.createNativeQuery("SELECT DISTINCT ?root WHERE {" +
                "?root a ?type ." +
                "?vocabulary ?hasGlossary/?hasTerm ?root ." +
                "?root ?hasChild* ?term ." +
                "{\n ?term a ?type ;" +
                "rdfs:label ?label ." +
                "FILTER CONTAINS(LCASE(?label), LCASE(?searchString)) ." +
                "}\n} ORDER BY ?label", Term.class)
                 .setParameter("type", typeUri)
                 .setParameter("hasGlossary", URI.create(cz.cvut.kbss.termit.util.Vocabulary.s_p_ma_glosar))
                 .setParameter("hasTerm", URI.create(cz.cvut.kbss.termit.util.Vocabulary.s_p_obsahuje_pojem))
                 .setParameter("vocabulary", vocabulary.getUri())
                 .setParameter("hasChild", URI.create(cz.cvut.kbss.termit.util.Vocabulary.s_p_narrower))
                 .setParameter("searchString", searchString, config.get(ConfigParam.LANGUAGE))
                 .getResultList();
    }

    /**
     * Checks whether a term with the specified label exists in a vocabulary with the specified URI.
     * <p>
     * Note that this method uses comparison ignoring case, so that two labels differing just in character case are
     * considered same here.
     *
     * @param label         Label to check
     * @param vocabularyUri Vocabulary in which terms will be searched
     * @return Whether term with {@code label} already exists in vocabulary
     */
    public boolean existsInVocabulary(String label, URI vocabularyUri) {
        Objects.requireNonNull(label);
        Objects.requireNonNull(vocabularyUri);
        return em.createNativeQuery("ASK { ?term a ?type ; " +
                "?hasLabel ?label ;" +
                "?inVocabulary ?vocabulary ." +
                "FILTER (LCASE(?label) = LCASE(?searchString)) . }", Boolean.class)
                 .setParameter("type", typeUri)
                 .setParameter("hasLabel", URI.create(RDFS.LABEL))
                 .setParameter("inVocabulary",
                     URI.create(cz.cvut.kbss.termit.util.Vocabulary.s_p_je_pojmem_ze_slovniku))
                 .setParameter("vocabulary", vocabularyUri)
                 .setParameter("searchString", label, config.get(ConfigParam.LANGUAGE)).getSingleResult();
    }
}