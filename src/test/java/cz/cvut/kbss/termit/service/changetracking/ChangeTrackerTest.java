package cz.cvut.kbss.termit.service.changetracking;

import cz.cvut.kbss.jopa.model.EntityManager;
import cz.cvut.kbss.jopa.vocabulary.SKOS;
import cz.cvut.kbss.termit.environment.Environment;
import cz.cvut.kbss.termit.environment.Generator;
import cz.cvut.kbss.termit.model.Term;
import cz.cvut.kbss.termit.model.User;
import cz.cvut.kbss.termit.model.Vocabulary;
import cz.cvut.kbss.termit.model.changetracking.AbstractChangeRecord;
import cz.cvut.kbss.termit.model.changetracking.PersistChangeRecord;
import cz.cvut.kbss.termit.model.changetracking.UpdateChangeRecord;
import cz.cvut.kbss.termit.model.util.DescriptorFactory;
import cz.cvut.kbss.termit.service.BaseServiceTestRunner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static cz.cvut.kbss.termit.service.changetracking.MetamodelBasedChangeCalculatorTest.cloneOf;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class ChangeTrackerTest extends BaseServiceTestRunner {

    @Autowired
    private EntityManager em;

    @Autowired
    private ChangeTracker sut;

    private User author;

    private Vocabulary vocabulary;

    @BeforeEach
    void setUp() {
        this.author = Generator.generateUserWithId();
        Environment.setCurrentUser(author);
        this.vocabulary = Generator.generateVocabularyWithId();
        transactional(() -> {
            em.persist(author);
            em.persist(vocabulary, DescriptorFactory.vocabularyDescriptor(vocabulary));
        });
    }

    @Test
    void recordAddEventStoresCreationChangeRecordInRepository() {
        enableRdfsInference(em);
        final Term newTerm = Generator.generateTermWithId();
        newTerm.setVocabulary(vocabulary.getUri());
        transactional(() -> {
            em.persist(newTerm, DescriptorFactory.termDescriptor(newTerm));
            sut.recordAddEvent(newTerm);
        });

        final List<AbstractChangeRecord> result = findRecords();
        assertEquals(1, result.size());
        final AbstractChangeRecord record = result.get(0);
        assertThat(record, instanceOf(PersistChangeRecord.class));
        assertEquals(newTerm.getUri(), record.getChangedAsset());
        assertEquals(author, record.getAuthor());
        assertNotNull(record.getTimestamp());
    }

    private List<AbstractChangeRecord> findRecords() {
        return em.createNativeQuery("SELECT ?x WHERE { ?x a ?changeRecord . }", AbstractChangeRecord.class)
                 .setParameter("changeRecord", em.getMetamodel().entity(AbstractChangeRecord.class).getIRI().toURI())
                 .getResultList();
    }

    @Test
    void recordUpdateEventDoesNothingWhenAssetDidNotChange() {
        enableRdfsInference(em);
        final Term original = Generator.generateTermWithId();
        original.setVocabulary(vocabulary.getUri());
        transactional(() -> em.persist(original, DescriptorFactory.termDescriptor(original)));

        final Term update = cloneOf(original);
        transactional(() -> sut.recordUpdateEvent(update, original));

        assertTrue(findRecords().isEmpty());
    }

    @Test
    void recordUpdateRecordsSingleChangeToLiteralAttribute() {
        enableRdfsInference(em);
        final Term original = Generator.generateTermWithId();
        original.setVocabulary(vocabulary.getUri());
        transactional(() -> em.persist(original, DescriptorFactory.termDescriptor(original)));

        final Term update = cloneOf(original);
        update.setDefinition("Updated definition of this term.");
        transactional(() -> sut.recordUpdateEvent(update, original));

        final List<AbstractChangeRecord> result = findRecords();
        assertEquals(1, result.size());
        final AbstractChangeRecord record = result.get(0);
        assertEquals(original.getUri(), record.getChangedAsset());
        assertThat(record, instanceOf(UpdateChangeRecord.class));
        assertEquals(SKOS.DEFINITION, ((UpdateChangeRecord) record).getChangedAttribute().toString());
    }

    @Test
    void recordUpdateRecordsMultipleChangesToAttributes() {
        enableRdfsInference(em);
        final Term original = Generator.generateTermWithId();
        original.setVocabulary(vocabulary.getUri());
        transactional(() -> em.persist(original, DescriptorFactory.termDescriptor(original)));

        final Term update = cloneOf(original);
        update.setDefinition("Updated definition of this term.");
        update.setVocabulary(Generator.generateUri());
        transactional(() -> sut.recordUpdateEvent(update, original));

        final List<AbstractChangeRecord> result = findRecords();
        assertEquals(2, result.size());
        result.forEach(record -> {
            assertEquals(original.getUri(), record.getChangedAsset());
            assertThat(record, instanceOf(UpdateChangeRecord.class));
            assertThat(((UpdateChangeRecord) record).getChangedAttribute().toString(), anyOf(equalTo(SKOS.DEFINITION),
                    equalTo(cz.cvut.kbss.termit.util.Vocabulary.s_p_je_pojmem_ze_slovniku)));
        });
    }
}
