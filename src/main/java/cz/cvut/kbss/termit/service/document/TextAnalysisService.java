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
package cz.cvut.kbss.termit.service.document;

import cz.cvut.kbss.termit.dto.TextAnalysisInput;
import cz.cvut.kbss.termit.exception.UnsupportedAssetOperationException;
import cz.cvut.kbss.termit.exception.WebServiceIntegrationException;
import cz.cvut.kbss.termit.model.TextAnalysisRecord;
import cz.cvut.kbss.termit.model.resource.File;
import cz.cvut.kbss.termit.persistence.dao.TextAnalysisRecordDao;
import cz.cvut.kbss.termit.util.ConfigParam;
import cz.cvut.kbss.termit.util.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.*;

@Service
public class TextAnalysisService {

    private static final Logger LOG = LoggerFactory.getLogger(TextAnalysisService.class);

    private final RestTemplate restClient;

    private final Configuration config;

    private final DocumentManager documentManager;

    private final AnnotationGenerator annotationGenerator;

    private final TextAnalysisRecordDao recordDao;

    @Autowired
    public TextAnalysisService(RestTemplate restClient, Configuration config, DocumentManager documentManager,
                               AnnotationGenerator annotationGenerator, TextAnalysisRecordDao recordDao) {
        this.restClient = restClient;
        this.config = config;
        this.documentManager = documentManager;
        this.annotationGenerator = annotationGenerator;
        this.recordDao = recordDao;
    }

    /**
     * Passes the content of the specified file to the remote text analysis service, letting it find occurrences of
     * terms from the vocabulary associated with parent document of the specified file in the text.
     * <p>
     * The analysis result is passed to the term occurrence generator.
     *
     * @param file File whose content shall be analyzed
     * @see #analyzeFile(File, Set)
     */
    @Transactional
    public void analyzeFile(File file) {
        Objects.requireNonNull(file);
        final TextAnalysisInput input = createAnalysisInput(file);
        if (file.getDocument() == null || file.getDocument().getVocabulary() == null) {
            throw new UnsupportedAssetOperationException("Cannot analyze file without specifying vocabulary context.");
        }
        input.addVocabularyContext(file.getDocument().getVocabulary());
        invokeTextAnalysisService(file, input);
    }

    private TextAnalysisInput createAnalysisInput(File file) {
        final TextAnalysisInput input = new TextAnalysisInput();
        input.setContent(documentManager.loadFileContent(file));
        input.setVocabularyRepository(URI.create(config.get(ConfigParam.REPOSITORY_URL)));
        input.setLanguage(config.get(ConfigParam.LANGUAGE));
        return input;
    }

    private void invokeTextAnalysisService(File file, TextAnalysisInput input) {
        final HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.ACCEPT, MediaType.APPLICATION_XML_VALUE);
        try {
            LOG.debug("Invoking text analysis on input: {}", input);
            final ResponseEntity<Resource> resp = restClient
                    .exchange(config.get(ConfigParam.TEXT_ANALYSIS_SERVICE_URL), HttpMethod.POST,
                            new HttpEntity<>(input, headers), Resource.class);
            if (!resp.hasBody()) {
                throw new WebServiceIntegrationException("Text analysis service returned empty response.");
            }
            assert resp.getBody() != null;
            documentManager.createBackup(file);
            final Resource resource = resp.getBody();
            try (final InputStream is = resource.getInputStream()) {
                annotationGenerator.generateAnnotations(is, file);
            }
            storeTextAnalysisRecord(file, input);
        } catch (WebServiceIntegrationException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new WebServiceIntegrationException("Text analysis invocation failed.", e);
        } catch (IOException e) {
            throw new WebServiceIntegrationException("Unable to read text analysis result from response.", e);
        }
    }

    private void storeTextAnalysisRecord(File file, TextAnalysisInput config) {
        LOG.trace("Creating record of text analysis event for file {}.", file);
        assert config.getVocabularyContexts() != null;

        final TextAnalysisRecord record = new TextAnalysisRecord(new Date(), file);
        record.setVocabularies(new HashSet<>(config.getVocabularyContexts()));
        recordDao.persist(record);
    }

    /**
     * Passes the content of the specified file to the remote text analysis service, letting it find occurrences of
     * terms from the vocabularies specified by their repository contexts.
     * <p>
     * The analysis result is passed to the term occurrence generator.
     *
     * @param file               File whose content shall be analyzed
     * @param vocabularyContexts Identifiers of repository contexts containing vocabularies intended for text analysis
     * @see #analyzeFile(File)
     */
    @Transactional
    public void analyzeFile(File file, Set<URI> vocabularyContexts) {
        Objects.requireNonNull(file);
        final TextAnalysisInput input = createAnalysisInput(file);
        input.setVocabularyContexts(vocabularyContexts);
        invokeTextAnalysisService(file, input);
    }

    /**
     * Gets the latest {@link TextAnalysisRecord} for the specified Resource.
     *
     * @param resource Analyzed Resource
     * @return Latest analysis record, if it exists
     */
    public Optional<TextAnalysisRecord> findLatestAnalysisRecord(cz.cvut.kbss.termit.model.resource.Resource resource) {
        return recordDao.findLatest(resource);
    }
}
