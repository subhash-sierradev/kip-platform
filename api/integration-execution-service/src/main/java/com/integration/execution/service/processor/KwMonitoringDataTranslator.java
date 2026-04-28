package com.integration.execution.service.processor;

import com.integration.execution.client.TranslationApiClient;
import com.integration.execution.config.properties.TranslationApiProperties;
import com.integration.execution.model.KwMonitoringDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Translates the human-readable text fields of {@link KwMonitoringDocument} objects
 * <em>at the data level</em> — before FreeMarker renders the Confluence page.
 *
 * <h3>Translated fields</h3>
 * <ul>
 *   <li>{@code title} — document title.</li>
 *   <li>{@code body} — document body text.</li>
 *   <li>String values inside {@code attributes.dynamicData} — content values
 *       (keys / field-names are left in their original language as they are
 *       schema-defined identifiers).</li>
 *   <li>String entries inside {@code attributes.tags}.</li>
 * </ul>
 *
 * <h3>Non-translated fields</h3>
 * Structural fields ({@code id}, timestamps, {@code tenantId}, form definition
 * identifiers), author display names, and serial-number strings are never sent
 * to the translation API.
 *
 * <h3>Batching</h3>
 * All translatable strings are collected from every document, batched into chunks
 * of at most {@link TranslationApiProperties#getMaxChars()} characters, and sent
 * to the Translation API in a single pass per batch using a segment separator
 * {@value #SEG} to join/split strings within each batch.
 *
 * <h3>Failure handling</h3>
 * Any exception causes a {@code WARN} log and the original document list is
 * returned unchanged so the pipeline never fails due to translation errors.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KwMonitoringDataTranslator {

    /** Segment separator used when batching multiple strings into one API call. */
    static final String SEG = "<<<SEG>>>";

    /** Compiled pattern for splitting on the segment separator. */
    private static final Pattern SEG_PATTERN = Pattern.compile(Pattern.quote("\n" + SEG + "\n"));

    private final TranslationApiClient translationApiClient;
    private final TranslationApiProperties translationApiProperties;

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /**
     * Translates the text content of all documents in {@code documents} from
     * {@code sourceLanguage} to {@code targetLanguage}.
     *
     * <p>The original list and its documents are <em>not</em> mutated; a new list of
     * deep-copied documents with translated field values is returned.</p>
     *
     * @param documents      source documents
     * @param sourceLanguage BCP-47 source language code (e.g. {@code "en"})
     * @param targetLanguage BCP-47 target language code (e.g. {@code "ja"})
     * @return new list with translated content, or {@code documents} when translation
     *         is skipped or fails
     */
    public List<KwMonitoringDocument> translate(final List<KwMonitoringDocument> documents,
                                                final String sourceLanguage,
                                                final String targetLanguage) {
        if (!translationApiProperties.isEnabled()) {
            log.debug("Translation disabled (translation.api.enabled=false); returning original monitoring data.");
            return documents;
        }

        if (documents == null || documents.isEmpty()) {
            return documents;
        }

        if (targetLanguage == null || targetLanguage.isBlank()
                || targetLanguage.equalsIgnoreCase(sourceLanguage)) {
            log.debug("Target '{}' equals source '{}'; skipping data translation.", targetLanguage, sourceLanguage);
            return documents;
        }

        try {
            // Step 1: Extract all translatable strings and record their positions
            List<FieldRef> fieldRefs = new ArrayList<>();
            List<String> originals = new ArrayList<>();
            for (int docIdx = 0; docIdx < documents.size(); docIdx++) {
                collectFields(documents.get(docIdx), docIdx, fieldRefs, originals);
            }

            if (originals.isEmpty()) {
                log.debug("No translatable fields found in monitoring data; returning originals.");
                return documents;
            }

            int totalChars = originals.stream().mapToInt(String::length).sum();
            log.info("Translating monitoring data: {} strings ({} chars) from '{}' to '{}'",
                    originals.size(), totalChars, sourceLanguage, targetLanguage);

            // Step 2: Translate in batches
            List<String> translated = translateInBatches(originals, sourceLanguage, targetLanguage);

            // Step 3: Deep-copy documents and apply translated values
            return applyTranslations(documents, fieldRefs, translated);

        } catch (Exception ex) {
            log.warn("KwMonitoringDataTranslator: data translation failed; returning original documents. Error: {}",
                    ex.getMessage(), ex);
            return documents;
        }
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /**
     * Collects all translatable string fields from {@code doc} and records each
     * field's position as a {@link FieldRef} so it can be re-applied after translation.
     */
    private void collectFields(final KwMonitoringDocument doc,
                                final int docIdx,
                                final List<FieldRef> refs,
                                final List<String> originals) {
        collectSimpleField(doc.getTitle(), FieldKind.TITLE, null, docIdx, refs, originals);
        collectSimpleField(doc.getBody(), FieldKind.BODY, null, docIdx, refs, originals);

        Map<String, Object> attributes = doc.getAttributes();
        if (attributes == null) {
            return;
        }
        collectDynamicDataFields(attributes, docIdx, refs, originals);
        collectTagFields(attributes, docIdx, refs, originals);
    }

    /** Adds a single string field to the collection lists if it is non-blank. */
    private void collectSimpleField(final String value, final FieldKind kind, final String key,
                                     final int docIdx, final List<FieldRef> refs,
                                     final List<String> originals) {
        if (value != null && !value.isBlank()) {
            refs.add(new FieldRef(docIdx, kind, key, -1));
            originals.add(value);
        }
    }

    /** Collects translatable string values from the {@code dynamicData} map. */
    private void collectDynamicDataFields(final Map<String, Object> attributes, final int docIdx,
                                           final List<FieldRef> refs, final List<String> originals) {
        Object rawDynamic = attributes.get("dynamicData");
        if (!(rawDynamic instanceof Map<?, ?> dynMap)) {
            return;
        }
        for (Map.Entry<?, ?> entry : dynMap.entrySet()) {
            if (entry.getKey() instanceof String key
                    && entry.getValue() instanceof String value
                    && !value.isBlank()) {
                refs.add(new FieldRef(docIdx, FieldKind.DYNAMIC_VALUE, key, -1));
                originals.add(value);
            }
        }
    }

    /** Collects translatable string entries from the {@code tags} list. */
    private void collectTagFields(final Map<String, Object> attributes, final int docIdx,
                                   final List<FieldRef> refs, final List<String> originals) {
        Object rawTags = attributes.get("tags");
        if (!(rawTags instanceof List<?> tagList)) {
            return;
        }
        for (int i = 0; i < tagList.size(); i++) {
            if (tagList.get(i) instanceof String tag && !tag.isBlank()) {
                refs.add(new FieldRef(docIdx, FieldKind.TAG, null, i));
                originals.add(tag);
            }
        }
    }

    /**
     * Deep-copies {@code documents} and re-injects the translated values at the
     * positions recorded in {@code refs}.
     */
    @SuppressWarnings("unchecked")
    private List<KwMonitoringDocument> applyTranslations(final List<KwMonitoringDocument> documents,
                                                          final List<FieldRef> refs,
                                                          final List<String> translated) {
        List<KwMonitoringDocument> result = new ArrayList<>(documents.size());
        for (KwMonitoringDocument doc : documents) {
            result.add(deepCopy(doc));
        }

        for (int i = 0; i < refs.size(); i++) {
            FieldRef ref = refs.get(i);
            String value = translated.get(i);
            KwMonitoringDocument doc = result.get(ref.docIdx());

            switch (ref.kind()) {
                case TITLE -> doc.setTitle(value);
                case BODY -> doc.setBody(value);
                case DYNAMIC_VALUE -> {
                    Map<String, Object> attrs = doc.getAttributes();
                    if (attrs != null && attrs.get("dynamicData") instanceof Map<?, ?> rawMap) {
                        ((Map<String, Object>) rawMap).put(ref.key(), value);
                    }
                }
                case TAG -> {
                    Map<String, Object> attrs = doc.getAttributes();
                    if (attrs != null && attrs.get("tags") instanceof List<?> rawList) {
                        List<Object> tagList = (List<Object>) rawList;
                        if (ref.tagIdx() < tagList.size()) {
                            tagList.set(ref.tagIdx(), value);
                        }
                    }
                }
                default -> throw new IllegalStateException("Unhandled FieldKind: " + ref.kind());
            }
        }

        return result;
    }

    /**
     * Creates a mutable deep copy of {@code src} so that translated values can be
     * injected without mutating the original document or its nested maps/lists.
     */
    @SuppressWarnings("unchecked")
    private KwMonitoringDocument deepCopy(final KwMonitoringDocument src) {
        Map<String, Object> attrsCopy = null;
        if (src.getAttributes() != null) {
            attrsCopy = new LinkedHashMap<>();
            for (Map.Entry<String, Object> entry : src.getAttributes().entrySet()) {
                String key = entry.getKey();
                Object val = entry.getValue();
                if ("dynamicData".equals(key) && val instanceof Map<?, ?> m) {
                    attrsCopy.put(key, new LinkedHashMap<>((Map<String, Object>) m));
                } else if ("tags".equals(key) && val instanceof List<?> l) {
                    attrsCopy.put(key, new ArrayList<>(l));
                } else {
                    attrsCopy.put(key, val);
                }
            }
        }

        return KwMonitoringDocument.builder()
                .id(src.getId())
                .title(src.getTitle())
                .body(src.getBody())
                .createdTimestamp(src.getCreatedTimestamp())
                .updatedTimestamp(src.getUpdatedTimestamp())
                .dynamicFormDefinitionId(src.getDynamicFormDefinitionId())
                .dynamicFormDefinitionName(src.getDynamicFormDefinitionName())
                .dynamicFormVersionNumber(src.getDynamicFormVersionNumber())
                .tenantId(src.getTenantId())
                .attributes(attrsCopy)
                .build();
    }

    /**
     * Splits {@code texts} into batches of at most
     * {@link TranslationApiProperties#getMaxChars()} characters, translates each
     * batch via {@link TranslationApiClient}, and returns the full translated list
     * in the same order.  Falls back to the original string for any batch that fails.
     */
    private List<String> translateInBatches(final List<String> texts,
                                             final String src,
                                             final String target) {
        List<String> result = new ArrayList<>(texts);
        int start = 0;
        int batchCharLimit = translationApiProperties.getMaxChars();

        while (start < texts.size()) {
            List<String> batch = new ArrayList<>();
            int charCount = 0;
            int end = start;

            while (end < texts.size()) {
                String text = texts.get(end);
                int separatorCost = batch.isEmpty() ? 0 : (SEG.length() + 2); // "\n" + SEG + "\n"
                int toAdd = text.length() + separatorCost;
                if (!batch.isEmpty() && charCount + toAdd > batchCharLimit) {
                    break;
                }
                batch.add(text);
                charCount += toAdd;
                end++;
            }

            String joined = String.join("\n" + SEG + "\n", batch);
            Optional<String> translatedOpt = translationApiClient.translate(joined, src, target);

            if (translatedOpt.isPresent()) {
                String[] parts = SEG_PATTERN.split(translatedOpt.get(), -1);
                if (parts.length == batch.size()) {
                    for (int i = 0; i < batch.size(); i++) {
                        result.set(start + i, parts[i].trim());
                    }
                    log.debug("Data batch [{}-{}]: {} strings translated successfully.",
                            start, end - 1, batch.size());
                } else {
                    log.warn("Data batch [{}-{}]: received {} segments but expected {}; "
                            + "keeping original text for this batch.",
                            start, end - 1, parts.length, batch.size());
                }
            } else {
                log.warn("Data batch [{}-{}]: Translation API returned no result; "
                        + "keeping original text for this batch.", start, end - 1);
            }

            start = end;
        }

        return result;
    }

    // -----------------------------------------------------------------------
    // Inner types
    // -----------------------------------------------------------------------

    private enum FieldKind { TITLE, BODY, DYNAMIC_VALUE, TAG }

    /**
     * Records the position of a translatable string within the document list so that
     * the translated value can be re-injected after batched translation.
     *
     * @param docIdx  index of the document in the list
     * @param kind    which kind of field this refers to
     * @param key     map key — only used for {@link FieldKind#DYNAMIC_VALUE}
     * @param tagIdx  list index — only used for {@link FieldKind#TAG}
     */
    private record FieldRef(int docIdx, FieldKind kind, String key, int tagIdx) { }
}


