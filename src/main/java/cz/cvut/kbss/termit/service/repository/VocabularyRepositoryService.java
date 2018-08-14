package cz.cvut.kbss.termit.service.repository;

import cz.cvut.kbss.termit.model.Vocabulary;
import cz.cvut.kbss.termit.persistence.dao.GenericDao;
import cz.cvut.kbss.termit.persistence.dao.VocabularyDao;
import cz.cvut.kbss.termit.service.IdentifierResolver;
import cz.cvut.kbss.termit.service.security.SecurityUtils;
import cz.cvut.kbss.termit.util.ConfigParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.validation.Validator;
import java.util.Date;

@Service
public class VocabularyRepositoryService extends BaseRepositoryService<Vocabulary> {

    private final SecurityUtils securityUtils;

    private final IdentifierResolver idResolver;

    private final VocabularyDao vocabularyDao;

    @Autowired
    public VocabularyRepositoryService(VocabularyDao vocabularyDao, SecurityUtils securityUtils,
                                       IdentifierResolver idResolver,
                                       Validator validator) {
        super(validator);
        this.vocabularyDao = vocabularyDao;
        this.securityUtils = securityUtils;
        this.idResolver = idResolver;
    }

    @Override
    protected GenericDao<Vocabulary> getPrimaryDao() {
        return vocabularyDao;
    }

    @Override
    protected void prePersist(Vocabulary instance) {
        validate(instance);
        instance.setDateCreated(new Date());
        instance.setAuthor(securityUtils.getCurrentUser());
        if (instance.getUri() == null) {
            instance.setUri(idResolver.generateIdentifier(ConfigParam.NAMESPACE_VOCABULARY, instance.getName()));
        }
    }

    @Override
    protected Vocabulary postLoad(Vocabulary instance) {
        instance.getAuthor().erasePassword();
        return instance;
    }
}
