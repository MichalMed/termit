package cz.cvut.kbss.termit.service.repository;

import cz.cvut.kbss.termit.dto.assignment.TermAssignments;
import cz.cvut.kbss.termit.model.Term;
import cz.cvut.kbss.termit.model.Vocabulary;
import cz.cvut.kbss.termit.persistence.dao.AssetDao;
import cz.cvut.kbss.termit.persistence.dao.TermAssignmentDao;
import cz.cvut.kbss.termit.persistence.dao.TermDao;
import cz.cvut.kbss.termit.service.IdentifierResolver;
import cz.cvut.kbss.termit.util.Constants;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.Validator;
import java.net.URI;
import java.util.List;
import java.util.Objects;

@Service
public class TermRepositoryService extends BaseAssetRepositoryService<Term> {

    private final IdentifierResolver idResolver;

    private final TermDao termDao;

    private final TermAssignmentDao termAssignmentDao;

    private final VocabularyRepositoryService vocabularyService;

    public TermRepositoryService(Validator validator, IdentifierResolver idResolver,
                                 TermDao termDao, TermAssignmentDao termAssignmentDao,
                                 VocabularyRepositoryService vocabularyService) {
        super(validator);
        this.idResolver = idResolver;
        this.termDao = termDao;
        this.termAssignmentDao = termAssignmentDao;
        this.vocabularyService = vocabularyService;
    }

    @Override
    protected AssetDao<Term> getPrimaryDao() {
        return this.termDao;
    }

    @Override
    public void persist(Term instance) {
        throw new UnsupportedOperationException(
                "Persisting term by itself is not supported. It has to be connected to a vocabulary or a parent term.");
    }

    @Transactional
    public void addRootTermToVocabulary(Term instance, Vocabulary vocabulary) {
        validate(instance);
        Objects.requireNonNull(instance);
        Objects.requireNonNull(vocabulary);

        if (instance.getUri() == null) {
            instance.setUri(generateIdentifier(vocabulary.getUri(), instance.getLabel()));
        }
        // Load vocabulary so that it is managed and changes to it (resp. the glossary) are persisted on commit
        final Vocabulary toUpdate = vocabularyService.getRequiredReference(vocabulary.getUri());
        verifyIdentifierUnique(instance);
        toUpdate.getGlossary().addRootTerm(instance);
        instance.setVocabulary(toUpdate.getUri());
        termDao.persist(instance, toUpdate);
    }

    /**
     * Generates term identifier based on the specified parent vocabulary identifier and a term label.
     *
     * @param vocabularyUri Vocabulary identifier
     * @param termLabel     Term label
     * @return Generated term identifier
     */
    public URI generateIdentifier(URI vocabularyUri, String termLabel) {
        Objects.requireNonNull(vocabularyUri);
        Objects.requireNonNull(termLabel);
        return idResolver.generateIdentifier(
                idResolver.buildNamespace(vocabularyUri.toString(), Constants.TERM_NAMESPACE_SEPARATOR),
                termLabel);
    }

    @Transactional
    public void addChildTerm(Term instance, Term parentTerm) {
        validate(instance);
        Objects.requireNonNull(instance);
        Objects.requireNonNull(parentTerm);
        if (instance.getUri() == null) {
            instance.setUri(generateIdentifier(parentTerm.getVocabulary(), instance.getLabel()));
        }
        verifyIdentifierUnique(instance);

        if (instance.getVocabulary() == null) {
            instance.setVocabulary(parentTerm.getVocabulary());
        }
        instance.setParent(parentTerm.getUri());

        termDao.persist(instance, vocabularyService.getRequiredReference(parentTerm.getVocabulary()));
    }

    /**
     * Gets all terms from a vocabulary, regardless of their position in the term hierarchy.
     * <p>
     * This returns all terms contained in a vocabulary's glossary.
     *
     * @param vocabulary Vocabulary whose terms should be returned
     * @return List of terms ordered by label
     */
    public List<Term> findAll(Vocabulary vocabulary) {
        Objects.requireNonNull(vocabulary);
        return termDao.findAll(vocabulary);
    }

    /**
     * Finds all root terms (terms without parent term) in the specified vocabulary.
     *
     * @param vocabulary Vocabulary whose terms should be returned
     * @param pageSpec   Page specifying result number and position
     * @return Matching root terms
     */
    public List<Term> findAllRoots(Vocabulary vocabulary, Pageable pageSpec) {
        Objects.requireNonNull(vocabulary);
        Objects.requireNonNull(pageSpec);
        return termDao.findAllRoots(vocabulary, pageSpec);
    }

    public List<Term> findAllRoots(Vocabulary vocabulary, String searchString) {
        return termDao.findAllRoots(searchString, vocabulary);
    }

    /**
     * Checks whether a term with the specified label exists in a vocabulary with the specified URI.
     *
     * @param label      Label to check
     * @param vocabulary Vocabulary in which terms will be searched
     * @return Whether term with {@code label} already exists in vocabulary
     */
    public boolean existsInVocabulary(String label, Vocabulary vocabulary) {
        return termDao.existsInVocabulary(label, vocabulary);
    }

    /**
     * Retrieves aggregated information about the specified Term's assignments to and occurrences in {@link
     * cz.cvut.kbss.termit.model.resource.Resource}s.
     *
     * @param instance Term whose assignment/occurrence data should be retrieved
     * @return Aggregated Term assignment/occurrence data
     */
    public List<TermAssignments> getAssignmentsInfo(Term instance) {
        return termAssignmentDao.getAssignmentInfo(instance);
    }
}
