/**
 * TermIt
 * Copyright (C) 2019 Czech Technical University in Prague
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cz.cvut.kbss.termit.persistence.dao;

import cz.cvut.kbss.jopa.model.EntityManager;
import cz.cvut.kbss.termit.environment.Environment;
import cz.cvut.kbss.termit.environment.Generator;
import cz.cvut.kbss.termit.model.OccurrenceTarget;
import cz.cvut.kbss.termit.model.Term;
import cz.cvut.kbss.termit.model.TermOccurrence;
import cz.cvut.kbss.termit.model.User;
import cz.cvut.kbss.termit.model.resource.File;
import cz.cvut.kbss.termit.model.selector.TextQuoteSelector;
import cz.cvut.kbss.termit.util.Vocabulary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class TermOccurrenceDaoTest extends BaseDaoTestRunner {

    @Autowired
    private EntityManager em;

    @Autowired
    private TermOccurrenceDao sut;

    private User user;

    @BeforeEach
    void setUp() {
        this.user = Generator.generateUserWithId();
        Environment.setCurrentUser(user);
        transactional(() -> em.persist(user));
    }

    @Test
    void findAllFindsOccurrencesOfTerm() {
        final Map<Term, List<TermOccurrence>> map = generateOccurrences(false);
        final Term term = map.keySet().iterator().next();
        final List<TermOccurrence> occurrences = map.get(term);

        final List<TermOccurrence> result = sut.findAll(term);
        assertEquals(occurrences.size(), result.size());
        for (TermOccurrence to : result) {
            assertTrue(occurrences.stream().anyMatch(o -> o.getUri().equals(to.getUri())));
        }
    }

    private Map<Term, List<TermOccurrence>> generateOccurrences(boolean suggested, File... files) {
        final File[] filesToProcess;
        if (files.length == 0) {
            final File file = new File();
            file.setLabel("test.html");
            filesToProcess = new File[]{file};
        } else {
            filesToProcess = files;
        }
        for (File f : filesToProcess) {
            f.setUri(Generator.generateUri());
        }
        final Term tOne = new Term();
        tOne.setUri(Generator.generateUri());
        tOne.setLabel("Term one");
        final Term tTwo = new Term();
        tTwo.setUri(Generator.generateUri());
        tTwo.setLabel("Term two");
        final Map<Term, List<TermOccurrence>> map = new HashMap<>();
        map.put(tOne, new ArrayList<>());
        map.put(tTwo, new ArrayList<>());
        for (int i = 0; i < Generator.randomInt(5, 10); i++) {
            final TermOccurrence to = new TermOccurrence();
            if (suggested) {
                to.addType(Vocabulary.s_c_navrzeny_vyskyt_termu);
            }
            final OccurrenceTarget target = new OccurrenceTarget(filesToProcess.length > 1 ?
                                                                 filesToProcess[Generator
                                                                         .randomInt(0, filesToProcess.length)] :
                                                                 filesToProcess[0]);
            final TextQuoteSelector selector = new TextQuoteSelector("test");
            selector.setPrefix("this is a ");
            selector.setSuffix(".");
            target.setSelectors(Collections.singleton(selector));
            to.setTarget(target);
            if (Generator.randomBoolean()) {
                to.setTerm(tOne.getUri());
                map.get(tOne).add(to);
            } else {
                to.setTerm(tTwo.getUri());
                map.get(tTwo).add(to);
            }
        }
        transactional(() -> {
            for (File f : filesToProcess) {
                em.persist(f);
            }
            map.forEach((t, list) -> {
                em.persist(t);
                list.forEach(ta -> {
                    em.persist(ta.getTarget());
                    em.persist(ta);
                });
            });
        });
        return map;
    }

    @Test
    void findAllInFileReturnsTermOccurrencesWithTargetFile() {
        final File fOne = new File();
        fOne.setLabel("fOne.html");
        final File fTwo = new File();
        fTwo.setLabel("fTwo.html");
        final Map<Term, List<TermOccurrence>> allOccurrences = generateOccurrences(false, fOne, fTwo);
        final List<TermOccurrence> matching = allOccurrences.values().stream().flatMap(
                l -> l.stream().filter(to -> to.getTarget().getSource().equals(fOne.getUri())))
                                                            .collect(Collectors.toList());

        em.getEntityManagerFactory().getCache().evictAll();
        final List<TermOccurrence> result = sut.findAll(fOne);
        assertEquals(matching.size(), result.size());
        for (TermOccurrence to : result) {
            assertTrue(matching.stream().anyMatch(p -> to.getUri().equals(p.getUri())));
        }
    }

    @Test
    void removeSuggestedRemovesOccurrencesForFile() {
        final File file = new File();
        file.setLabel("test.html");
        generateOccurrences(true, file);
        assertFalse(sut.findAll(file).isEmpty());
        transactional(() -> sut.removeSuggested(file));
        assertTrue(sut.findAll(file).isEmpty());
        assertFalse(em.createNativeQuery("ASK { ?x a ?termOccurrence . }", Boolean.class).setParameter("termOccurrence",
                URI.create(Vocabulary.s_c_vyskyt_termu)).getSingleResult());
    }

    @Test
    void removeSuggestedRetainsConfirmedOccurrences() {
        final File file = new File();
        file.setLabel("test.html");
        final Map<Term, List<TermOccurrence>> allOccurrences = generateOccurrences(true, file);
        assertFalse(sut.findAll(file).isEmpty());
        final List<TermOccurrence> retained = new ArrayList<>();
        transactional(() -> allOccurrences
                .forEach((t, list) -> list.stream().filter(to -> Generator.randomBoolean()).forEach(to -> {
                    to.removeType(Vocabulary.s_c_navrzeny_vyskyt_termu);
                    retained.add(to);
                    em.merge(to);
                })));
        transactional(() -> sut.removeSuggested(file));
        final List<TermOccurrence> result = sut.findAll(file);
        assertEquals(retained.size(), result.size());
        result.forEach(to -> assertTrue(retained.stream().anyMatch(toExp -> toExp.getUri().equals(to.getUri()))));
    }

    @Test
    void removeAllRemovesSuggestedAndConfirmedOccurrences() {
        final File file = new File();
        file.setLabel("test.html");
        final Map<Term, List<TermOccurrence>> allOccurrences = generateOccurrences(true, file);
        assertFalse(sut.findAll(file).isEmpty());
        transactional(() -> allOccurrences
                .forEach((t, list) -> list.stream().filter(to -> Generator.randomBoolean()).forEach(to -> {
                    to.removeType(Vocabulary.s_c_navrzeny_vyskyt_termu);
                    em.merge(to);
                })));
        transactional(() -> sut.removeAll(file));
        assertTrue(sut.findAll(file).isEmpty());
        assertFalse(em.createNativeQuery("ASK { ?x a ?termOccurrence . }", Boolean.class).setParameter("termOccurrence",
                URI.create(Vocabulary.s_c_vyskyt_termu)).getSingleResult());
    }
}