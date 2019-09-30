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
package cz.cvut.kbss.termit.model.resource;

import com.fasterxml.jackson.annotation.JsonIgnore;
import cz.cvut.kbss.jopa.model.annotations.*;
import cz.cvut.kbss.jsonld.annotation.JsonLdAttributeOrder;
import cz.cvut.kbss.termit.exception.TermItException;
import cz.cvut.kbss.termit.model.util.SupportsStorage;
import cz.cvut.kbss.termit.service.IdentifierResolver;
import cz.cvut.kbss.termit.service.provenance.ProvenanceManager;
import cz.cvut.kbss.termit.util.Vocabulary;

import java.lang.reflect.Field;
import java.net.URI;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@OWLClass(iri = Vocabulary.s_c_dokument)
@EntityListeners(ProvenanceManager.class)
@JsonLdAttributeOrder({"uri", "label", "description", "author", "lastEditor"})
public class Document extends Resource implements SupportsStorage {

    @OWLObjectProperty(iri = Vocabulary.s_p_ma_soubor, fetch = FetchType.EAGER)
    private Set<File> files;

    @JsonIgnore
    @Inferred
    @OWLObjectProperty(iri = Vocabulary.s_p_ma_dokumentovy_slovnik)
    private URI vocabulary;

    public Set<File> getFiles() {
        return files;
    }

    public void setFiles(Set<File> files) {
        this.files = files;
    }

    public void addFile(File file) {
        Objects.requireNonNull(file);
        if (files == null) {
            this.files = new HashSet<>();
        }
        files.add(file);
    }

    public void removeFile(File file) {
        if (files != null) {
            files.remove(file);
        }
    }

    /**
     * Retrieves file with the specified filename from this document.
     *
     * @param fileName Name of the file to retrieve
     * @return {@code Optional} containing the file with a matching filename or an empty {@code Optional}
     */
    public Optional<File> getFile(String fileName) {
        return files != null ? files.stream().filter(f -> f.getLabel().equals(fileName)).findAny() :
               Optional.empty();
    }

    public URI getVocabulary() {
        return vocabulary;
    }

    public void setVocabulary(URI vocabulary) {
        this.vocabulary = vocabulary;
    }

    @Override
    public String getDirectoryName() {
        if (getLabel() == null || getUri() == null) {
            throw new IllegalStateException("Missing document name or URI required for directory name resolution.");
        }
        return IdentifierResolver.normalize(getLabel()) + "_" + getUri().hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Document)) {
            return false;
        }
        Document document = (Document) o;
        return Objects.equals(getUri(), document.getUri());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getUri());
    }

    @Override
    public String toString() {
        return "Document{" +
                "uri=" + getUri() +
                ", name='" + getLabel() + '\'' +
                ", description='" + getDescription() + '\'' +
                ", files=" + files +
                ", author=" + getAuthor() +
                ", dateCreated=" + getCreated() +
                '}';
    }

    public static Field getFilesField() {
        try {
            return Document.class.getDeclaredField("files");
        } catch (NoSuchFieldException e) {
            throw new TermItException("Fatal error! Unable to retrieve \"files\" field.", e);
        }
    }
}
