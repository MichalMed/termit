package cz.cvut.kbss.termit.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import cz.cvut.kbss.jopa.model.annotations.*;
import cz.cvut.kbss.termit.exception.TermItException;
import cz.cvut.kbss.termit.util.Vocabulary;

import java.lang.reflect.Field;
import java.util.Date;

@MappedSuperclass
public abstract class HasProvenanceData {

    @ParticipationConstraints(nonEmpty = true)
    @OWLObjectProperty(iri = Vocabulary.s_p_ma_autora, fetch = FetchType.EAGER)
    private User author;

    @ParticipationConstraints(nonEmpty = true)
    @OWLDataProperty(iri = Vocabulary.s_p_created)
    private Date created;

    @OWLObjectProperty(iri = Vocabulary.s_p_ma_posledniho_editora, fetch = FetchType.EAGER)
    private User lastEditor;

    @OWLDataProperty(iri = Vocabulary.s_p_ma_posledni_modifikaci)
    private Date lastModified;

    public User getAuthor() {
        return author;
    }

    public void setAuthor(User author) {
        this.author = author;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public User getLastEditor() {
        return lastEditor;
    }

    public void setLastEditor(User lastEditor) {
        this.lastEditor = lastEditor;
    }

    public Date getLastModified() {
        return lastModified;
    }

    public void setLastModified(Date lastModified) {
        this.lastModified = lastModified;
    }

    /**
     * Gets the datetime of this asset's last modification.
     * <p>
     * If {@link #getLastModified()} is set, its value is returned. Otherwise, {@link #getCreated()} is returned.
     *
     * @return Datetime of the last modification, if present, or creation of this asset
     */
    @JsonIgnore
    public Date getLastModifiedOrCreated() {
        return lastModified != null ? lastModified : created;
    }

    public static Field getAuthorField() {
        try {
            return HasProvenanceData.class.getDeclaredField("author");
        } catch (NoSuchFieldException e) {
            throw new TermItException("Fatal error! Unable to retrieve \"author\" field.", e);
        }
    }

    public static Field getLastEditorField() {
        try {
            return HasProvenanceData.class.getDeclaredField("lastEditor");
        } catch (NoSuchFieldException e) {
            throw new TermItException("Fatal error! Unable to retrieve \"lastEditor\" field.", e);
        }
    }
}
