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
package cz.cvut.kbss.termit.model.util;

import cz.cvut.kbss.jopa.model.descriptors.Descriptor;
import cz.cvut.kbss.jopa.model.metamodel.FieldSpecification;
import cz.cvut.kbss.termit.environment.Generator;
import cz.cvut.kbss.termit.model.Asset;
import cz.cvut.kbss.termit.model.HasProvenanceData;
import cz.cvut.kbss.termit.model.Term;
import cz.cvut.kbss.termit.model.Vocabulary;
import cz.cvut.kbss.termit.model.resource.Document;
import cz.cvut.kbss.termit.model.resource.File;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DescriptorFactoryTest {

    private Vocabulary vocabulary = Generator.generateVocabularyWithId();

    private Term term;

    private FieldSpecification parentFieldSpec;

    @BeforeEach
    void setUp() {
        this.term = Generator.generateTermWithId();
        term.setVocabulary(vocabulary.getUri());
        this.parentFieldSpec = mock(FieldSpecification.class);
        when(parentFieldSpec.getJavaField()).thenReturn(Term.getParentTermsField());
    }

    @Test
    void termDescriptorCreatesSimpleTermDescriptorWhenNoParentsAreProvided() {
        final Descriptor result = DescriptorFactory.termDescriptor(term);
        assertEquals(vocabulary.getUri(), result.getContext());
        assertEquals(vocabulary.getUri(), result.getAttributeContext(parentFieldSpec));
    }

    @Test
    void termDescriptorCreatesSimpleTermDescriptorWhenParentsAreInSameVocabulary() {
        final Term parent = Generator.generateTermWithId();
        parent.setVocabulary(vocabulary.getUri());
        term.addParentTerm(parent);
        final Descriptor result = DescriptorFactory.termDescriptor(term);
        assertEquals(vocabulary.getUri(), result.getContext());
        assertEquals(vocabulary.getUri(), result.getAttributeContext(parentFieldSpec));
    }

    @Test
    void termDescriptorCreatesDescriptorWithParentTermContextCorrespondingToParentTermVocabulary() {
        final Term parent = Generator.generateTermWithId();
        final URI parentVocabulary = Generator.generateUri();
        parent.setVocabulary(parentVocabulary);
        term.addParentTerm(parent);
        final Descriptor result = DescriptorFactory.termDescriptor(term);
        assertEquals(vocabulary.getUri(), result.getContext());
        assertEquals(parentVocabulary, result.getAttributeDescriptor(parentFieldSpec).getContext());
    }

    @Test
    void parentTermDescriptorContainedInTermDescriptorHasCorrectAuthorAndLastEditorContext() {
        final Term parent = Generator.generateTermWithId();
        final URI parentVocabulary = Generator.generateUri();
        parent.setVocabulary(parentVocabulary);
        term.addParentTerm(parent);
        final Descriptor result = DescriptorFactory.termDescriptor(term);
        final Descriptor parentDescriptor = result.getAttributeDescriptor(parentFieldSpec);
        assertEquals(parentVocabulary, parentDescriptor.getContext());
        final FieldSpecification authorFieldSpec = mock(FieldSpecification.class);
        when(authorFieldSpec.getJavaField()).thenReturn(HasProvenanceData.getAuthorField());
        assertNull(parentDescriptor.getAttributeDescriptor(authorFieldSpec).getContext());
        final FieldSpecification lastEditorFieldSpec = mock(FieldSpecification.class);
        when(lastEditorFieldSpec.getJavaField()).thenReturn(HasProvenanceData.getLastEditorField());
        assertNull(parentDescriptor.getAttributeDescriptor(lastEditorFieldSpec).getContext());
    }

    @Test
    void fileDescriptorContainsAlsoDescriptorForDocument() {
        final File file = Generator.generateFileWithId("test.html");
        final Document doc = Generator.generateDocumentWithId();
        doc.addFile(file);
        file.setDocument(doc);
        doc.setVocabulary(Generator.generateUri());
        final Descriptor result = DescriptorFactory.fileDescriptor(doc.getVocabulary());
        final FieldSpecification docFieldSpec = mock(FieldSpecification.class);
        when(docFieldSpec.getJavaField()).thenReturn(File.getDocumentField());
        final Descriptor docDescriptor = result.getAttributeDescriptor(docFieldSpec);
        assertNotNull(docDescriptor);
        final FieldSpecification authorFieldSpec = mock(FieldSpecification.class);
        when(authorFieldSpec.getJavaField()).thenReturn(Asset.getAuthorField());
        final Descriptor authorDescriptor = docDescriptor.getAttributeDescriptor(authorFieldSpec);
        assertNull(authorDescriptor.getContext());
    }
}
