package cz.cvut.kbss.termit.service.repository;

import cz.cvut.kbss.termit.exception.NotFoundException;
import cz.cvut.kbss.termit.exception.ResourceExistsException;
import cz.cvut.kbss.termit.model.Term;
import cz.cvut.kbss.termit.model.Vocabulary;
import cz.cvut.kbss.termit.persistence.dao.GenericDao;
import cz.cvut.kbss.termit.persistence.dao.TermDao;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.Validator;
import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
public class TermRepositoryService extends BaseRepositoryService<Term> {

    private final TermDao termDao;

    private final VocabularyRepositoryService vocabularyService;

    public TermRepositoryService(Validator validator, TermDao termDao, VocabularyRepositoryService vocabularyService) {
        super(validator);
        this.termDao = termDao;
        this.vocabularyService = vocabularyService;
    }

    @Override
    protected GenericDao<Term> getPrimaryDao() {
        return this.termDao;
    }

    @Transactional
    public void addTermToVocabulary(Term instance, Vocabulary vocabulary) {
        Objects.requireNonNull(instance);
        Objects.requireNonNull(vocabulary);
        addTopLevelTerm(instance, vocabulary);
        vocabularyService.update(vocabulary);
    }

    private void addTopLevelTerm(Term instance, Vocabulary vocabulary) {
        validate(instance);
        if (!vocabulary.getGlossary().getTerms().add(instance)) {
            throw ResourceExistsException.create("Term", instance.getUri());
        }
    }

    @Transactional
    public void addTermToVocabulary(Term instance, Vocabulary vocabulary, URI parentTermUri) {
        Objects.requireNonNull(instance);
        Objects.requireNonNull(vocabulary);
        Objects.requireNonNull(parentTermUri);
        addTopLevelTerm(instance, vocabulary);

        Term parenTerm = find(parentTermUri).orElseThrow(() -> NotFoundException.create("Term", parentTermUri));

        Set<URI> newTerms = parenTerm.getSubTerms();
        newTerms.add(instance.getUri());
        parenTerm.setSubTerms(newTerms);

        vocabularyService.update(vocabulary);
    }

    public List<Term> findAll(URI vocabularyUri, int limit, int offset) {
        Vocabulary vocabulary = vocabularyService.find(vocabularyUri)
                                                 .orElseThrow(() -> NotFoundException
                                                         .create(Vocabulary.class.getSimpleName(), vocabularyUri));
        Pageable pageable = PageRequest.of(offset, limit);

        return termDao.findAll(pageable, vocabulary);
    }

    public List<Term> findAll(String searchString, URI vocabularyUri) {
        Vocabulary vocabulary = vocabularyService.find(vocabularyUri)
                                                 .orElseThrow(() -> NotFoundException
                                                         .create(Vocabulary.class.getSimpleName(), vocabularyUri));

        List<Term> rootTerms = termDao.findAll(searchString, vocabulary);
        // List<Term> allTerms = new ArrayList<>(rootTerms.size()*4);
        //TODO filter
        return rootTerms;
    }
}
