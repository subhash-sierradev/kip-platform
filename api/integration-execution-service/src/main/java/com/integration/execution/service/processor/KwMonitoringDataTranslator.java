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
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
 *   <li>{@code displayFullName} values inside {@code attributes.authors[]} — author display names.</li>
 * </ul>
 *
 * <h3>Non-translated fields</h3>
 * Structural fields ({@code id}, timestamps, {@code tenantId}, form definition
 * identifiers), and serial-number strings are never sent to the translation API.
 *
 * <h3>Translation strategy</h3>
 * All translatable strings are collected from every document and each string is
 * sent to the Translation API individually. For multi-language output
 * {@link #translateAll} passes every string once carrying all target language
 * codes ({@link TranslationApiClient#translateMulti}), replacing N × L calls
 * with N calls. When a string cannot be translated the original is kept and a
 * WARN is logged; the pipeline never fails due to translation errors.
 *
 * <h3>Failure handling</h3>
 * Any exception causes a {@code WARN} log and the original document list is
 * returned unchanged so the pipeline never fails due to translation errors.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KwMonitoringDataTranslator {
    /** Dynamic-data keys (lowercase) excluded from translation: "date" (pre-formatted),
     * "priority" (colour resolution key; display label comes from uiLabels["HIGH"] etc.),
     * "client" (used for grouping only; never rendered directly in dynamic-field rows). */
    private static final Set<String> NON_TRANSLATABLE_DYNAMIC_KEYS = Set.of("date", "priority", "client");

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
            List<FieldRef> fieldRefs = new ArrayList<>();
            List<String> originals = new ArrayList<>();
            for (int docIdx = 0; docIdx < documents.size(); docIdx++) {
                collectFields(documents.get(docIdx), docIdx, fieldRefs, originals);
            }
            if (originals.isEmpty()) {
                log.debug("No translatable fields found in monitoring data; returning originals.");
                return documents;
            }
            log.info("Translating monitoring data: {} strings from '{}' to '{}'",
                    originals.size(), sourceLanguage, targetLanguage);
            List<String> translated = translateStrings(originals, sourceLanguage, targetLanguage);
            return applyTranslations(documents, fieldRefs, translated);
        } catch (Exception ex) {
            log.warn("KwMonitoringDataTranslator: data translation failed; returning original documents. Error: {}",
                    ex.getMessage(), ex);
            return documents;
        }
    }
    /**
     * Translates all documents to <em>every</em> non-source target language in a
     * single pass — one API call per string instead of one per language per string.
     *
     * <p>The original list and its documents are <em>not</em> mutated; each language
     * receives its own deep-copied, translated document list.</p>
     *
     * @param documents       source documents
     * @param sourceLanguage  BCP-47 source language code (e.g. {@code "en"})
     * @param targetLanguages list of BCP-47 target language codes (may include source; filtered internally)
     * @return map of languageCode → translated document list; source language and blank/null
     *         codes are excluded; original documents returned on disabled/error
     */
    public Map<String, List<KwMonitoringDocument>> translateAll(
            final List<KwMonitoringDocument> documents,
            final String sourceLanguage,
            final List<String> targetLanguages) {
        List<String> targets = filterTargets(targetLanguages, sourceLanguage);
        if (targets.isEmpty()) {
            return Map.of();
        }
        if (!translationApiProperties.isEnabled()) {
            log.warn("Translation disabled (translation.api.enabled=false); "
                    + "returning source-language docs for target languages: {}", targets);
            return fallbackAll(targets, documents);
        }
        if (documents == null || documents.isEmpty()) {
            return fallbackAll(targets, documents);
        }
        try {
            return doTranslateAll(documents, sourceLanguage, targets);
        } catch (Exception ex) {
            log.warn("KwMonitoringDataTranslator: multi-language translation failed; "
                    + "returning original documents for all languages. Error: {}", ex.getMessage(), ex);
            return fallbackAll(targets, documents);
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
        collectAuthorFields(attributes, docIdx, refs, originals);
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
            if (!(entry.getKey() instanceof String key)) {
                continue;
            }
            if (NON_TRANSLATABLE_DYNAMIC_KEYS.contains(key.toLowerCase(Locale.ENGLISH))) {
                continue;
            }
            Object val = entry.getValue();
            if (val instanceof String strVal && !strVal.isBlank()) {
                refs.add(new FieldRef(docIdx, FieldKind.DYNAMIC_VALUE, key, -1, null));
                originals.add(strVal);
            } else if (val instanceof Map<?, ?> innerMap) {
                collectInnerMapFields(docIdx, key, innerMap, refs, originals);
            }
        }
    }

    /**
     * Collects translatable entries from a nested Map value inside {@code dynamicData}.
     * Finds the first entry whose value is either a plain String or a List of Strings
     * (mirrors {@code resolveStringValue} logic in the renderer).
     */
    private void collectInnerMapFields(final int docIdx, final String outerKey,
                                        final Map<?, ?> innerMap,
                                        final List<FieldRef> refs, final List<String> originals) {
        for (Map.Entry<?, ?> inner : innerMap.entrySet()) {
            if (!(inner.getKey() instanceof String innerKey)) {
                continue;
            }
            if (inner.getValue() instanceof String innerStr && !innerStr.isBlank()) {
                refs.add(new FieldRef(docIdx, FieldKind.DYNAMIC_VALUE, outerKey, -1, innerKey));
                originals.add(innerStr);
                break;
            } else if (inner.getValue() instanceof List<?> innerList) {
                for (int li = 0; li < innerList.size(); li++) {
                    if (innerList.get(li) instanceof String listStr && !listStr.isBlank()) {
                        refs.add(new FieldRef(docIdx, FieldKind.DYNAMIC_VALUE, outerKey, -1, innerKey, li));
                        originals.add(listStr);
                    }
                }
                break;
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

    /** Collects {@code displayFullName} values from the {@code authors} list for translation. */
    private void collectAuthorFields(final Map<String, Object> attributes, final int docIdx,
                                      final List<FieldRef> refs, final List<String> originals) {
        Object rawAuthors = attributes.get("authors");
        if (!(rawAuthors instanceof List<?> authorList)) {
            return;
        }
        for (int i = 0; i < authorList.size(); i++) {
            if (authorList.get(i) instanceof Map<?, ?> author) {
                Object name = author.get("displayFullName");
                if (name instanceof String s && !s.isBlank()) {
                    refs.add(new FieldRef(docIdx, FieldKind.AUTHOR_NAME, null, i));
                    originals.add(s);
                }
            }
        }
    }
    /**
     * Deep-copies {@code documents} and re-injects the translated values at the
     * positions recorded in {@code refs}.
     */
    private List<KwMonitoringDocument> applyTranslations(final List<KwMonitoringDocument> documents,
                                                          final List<FieldRef> refs,
                                                          final List<String> translated) {
        List<KwMonitoringDocument> result = new ArrayList<>(documents.size());
        for (KwMonitoringDocument doc : documents) {
            result.add(deepCopy(doc));
        }
        for (int i = 0; i < refs.size(); i++) {
            applyFieldValue(refs.get(i), translated.get(i), result.get(refs.get(i).docIdx()));
        }
        return result;
    }

    /** Writes one translated value back into the correct field of {@code doc} per {@code ref}. */
    @SuppressWarnings("unchecked")
    private void applyFieldValue(final FieldRef ref, final String value,
                                 final KwMonitoringDocument doc) {
        switch (ref.kind()) {
            case TITLE -> doc.setTitle(value);
            case BODY  -> doc.setBody(value);
            case DYNAMIC_VALUE -> {
                Map<String, Object> attrs = doc.getAttributes();
                if (attrs != null && attrs.get("dynamicData") instanceof Map<?, ?> rawMap) {
                    if (ref.innerKey() == null) {
                        // Plain string value — replace directly
                        ((Map<String, Object>) rawMap).put(ref.key(), value);
                    } else {
                        // Nested map value — guaranteed to exist and be a Map by construction.
                        Map<String, Object> innerMap = (Map<String, Object>) rawMap.get(ref.key());
                        if (ref.innerListIdx() < 0) {
                            // Simple string entry in inner map
                            innerMap.put(ref.innerKey(), value);
                        } else {
                            // List entry in inner map — update at specific index
                            @SuppressWarnings("unchecked")
                            List<Object> innerList = (List<Object>) innerMap.get(ref.innerKey());
                            innerList.set(ref.innerListIdx(), value);
                        }
                    }
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
            case AUTHOR_NAME -> applyAuthorName(doc, ref.tagIdx(), value);
            default -> throw new IllegalStateException("Unhandled FieldKind: " + ref.kind());
        }
    }

    /** Writes a translated author display name at {@code authorIdx} in the authors list. */
    @SuppressWarnings("unchecked")
    private void applyAuthorName(final KwMonitoringDocument doc,
                                  final int authorIdx, final String value) {
        Map<String, Object> attrs = doc.getAttributes();
        if (attrs != null && attrs.get("authors") instanceof List<?> rawList) {
            List<Object> authorList = (List<Object>) rawList;
            if (authorIdx < authorList.size()
                    && authorList.get(authorIdx) instanceof Map<?, ?> m) {
                ((Map<String, Object>) m).put("displayFullName", value);
            }
        }
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
                    // Deep copy: also copy any Map values inside dynamicData so that
                    // applyFieldValue mutations on inner maps don't bleed across language copies.
                    Map<String, Object> dynCopy = new LinkedHashMap<>();
                    for (Map.Entry<?, ?> dynEntry : m.entrySet()) {
                        if (dynEntry.getKey() instanceof String dynKey) {
                            Object dynVal = dynEntry.getValue();
                            if (dynVal instanceof Map<?, ?> innerMap) {
                                // Deep copy the inner map AND any List values within it so that
                                // per-language mutations (new list items) don't bleed across copies.
                                Map<String, Object> innerCopy = new LinkedHashMap<>();
                                for (Map.Entry<?, ?> innerEntry : innerMap.entrySet()) {
                                    if (innerEntry.getKey() instanceof String innerKey) {
                                        Object innerVal = innerEntry.getValue();
                                        if (innerVal instanceof List<?> innerList) {
                                            innerCopy.put(innerKey, new ArrayList<>(innerList));
                                        } else {
                                            innerCopy.put(innerKey, innerVal);
                                        }
                                    }
                                }
                                dynCopy.put(dynKey, innerCopy);
                            } else {
                                dynCopy.put(dynKey, dynVal);
                            }
                        }
                    }
                    attrsCopy.put(key, dynCopy);
                } else if ("tags".equals(key) && val instanceof List<?> l) {
                    attrsCopy.put(key, new ArrayList<>(l));
                } else if ("authors".equals(key) && val instanceof List<?> l) {
                    attrsCopy.put(key, deepCopyAuthorList(l));
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
     * Deep-copies a list of author objects; non-Map entries are carried over by reference.
     */
    @SuppressWarnings("unchecked")
    private List<Object> deepCopyAuthorList(final List<?> authorList) {
        List<Object> copy = new ArrayList<>(authorList.size());
        for (Object author : authorList) {
            if (author instanceof Map<?, ?> m) {
                copy.add(new LinkedHashMap<>((Map<String, Object>) m));
            } else {
                copy.add(author);
            }
        }
        return copy;
    }

    /**
     * Translates each string in {@code texts} individually via
     * {@link TranslationApiClient#translate}. When the API returns no result for a
     * string the original is kept and a WARN is logged.
     */
    private List<String> translateStrings(final List<String> texts,
                                           final String src,
                                           final String target) {
        List<String> result = new ArrayList<>(texts);
        for (int i = 0; i < texts.size(); i++) {
            Optional<String> translated = translationApiClient.translate(texts.get(i), src, target);
            if (translated.isPresent()) {
                result.set(i, translated.get());
            } else {
                log.warn("String [{}]: Translation API returned no result; keeping original.", i);
            }
        }
        return result;
    }
    /**
     * Like {@link #translateStrings} but sends each string to <em>all</em>
     * {@code targets} in a single {@link TranslationApiClient#translateMulti} call,
     * returning a per-language result list. When a language is absent from the
     * response the original string is kept for that language and a WARN is logged.
     */
    private Map<String, List<String>> translateStringsMultiLang(final List<String> texts,
                                                                  final String src,
                                                                  final List<String> targets) {
        Map<String, List<String>> resultByLang = new LinkedHashMap<>();
        for (String target : targets) {
            resultByLang.put(target, new ArrayList<>(texts));
        }
        for (int i = 0; i < texts.size(); i++) {
            Map<String, Optional<String>> perLang =
                    translationApiClient.translateMulti(texts.get(i), src, targets);
            for (String target : targets) {
                Optional<String> translated = perLang.getOrDefault(target, Optional.empty());
                if (translated.isPresent()) {
                    resultByLang.get(target).set(i, translated.get());
                } else {
                    log.warn("String [{}] - '{}': Translation API returned no result; keeping original.",
                            i, target);
                }
            }
        }
        return resultByLang;
    }
    /**
     * Filters {@code targetLanguages} to non-blank, non-source, distinct codes.
     */
    private List<String> filterTargets(final List<String> targetLanguages,
                                        final String sourceLanguage) {
        return targetLanguages.stream()
                .filter(l -> l != null && !l.isBlank() && !l.equalsIgnoreCase(sourceLanguage))
                .distinct()
                .toList();
    }
    /**
     * Returns a map keyed by each target language that maps to the original
     * (untranslated) {@code documents} list. Used as the fallback when disabled or failing.
     */
    private Map<String, List<KwMonitoringDocument>> fallbackAll(
            final List<String> targets, final List<KwMonitoringDocument> documents) {
        Map<String, List<KwMonitoringDocument>> fallback = new LinkedHashMap<>();
        for (String lang : targets) {
            fallback.put(lang, documents);
        }
        return fallback;
    }
    /**
     * Core multi-language translation: collects fields, translates each string for
     * all targets, and applies per-language results.
     * Extracted from {@link #translateAll} to keep NPath complexity low.
     */
    private Map<String, List<KwMonitoringDocument>> doTranslateAll(
            final List<KwMonitoringDocument> documents,
            final String sourceLanguage,
            final List<String> targets) {
        List<FieldRef> fieldRefs = new ArrayList<>();
        List<String> originals = new ArrayList<>();
        for (int docIdx = 0; docIdx < documents.size(); docIdx++) {
            collectFields(documents.get(docIdx), docIdx, fieldRefs, originals);
        }
        if (originals.isEmpty()) {
            log.debug("No translatable fields found; returning originals for all target languages.");
            return fallbackAll(targets, documents);
        }
        log.info("Multi-language translation: {} strings from '{}' to {}",
                originals.size(), sourceLanguage, targets);
        Map<String, List<String>> translatedByLang =
                translateStringsMultiLang(originals, sourceLanguage, targets);
        Map<String, List<KwMonitoringDocument>> result = new LinkedHashMap<>();
        for (String lang : targets) {
            List<String> translated = translatedByLang.getOrDefault(lang, new ArrayList<>(originals));
            result.put(lang, applyTranslations(documents, fieldRefs, translated));
        }
        return result;
    }
    /**
     * Translates a map of source-language label strings into every requested target language.
     *
     * <p>One {@link TranslationApiClient#translateMulti} HTTP call is made <em>per label entry</em>,
     * covering all target languages in that single request (N labels → N HTTP requests,
     * each returning translations for all L languages at once).  The approach follows the
     * same "collect once, apply many" pattern used by {@link #translateStringsMultiLang}.</p>
     *
     * <p>Any per-label or per-language failure is handled gracefully: the original
     * English value is kept for that entry so the pipeline never breaks.</p>
     *
     * @param englishLabels  map of {@code labelKey → English display string}
     * @param sourceLanguage BCP-47 source language code (e.g. {@code "en"})
     * @param targetLanguages list of BCP-47 target language codes; the source language
     *                        and blank/null entries are filtered out automatically
     * @return map of {@code languageCode → (labelKey → translated value)};
     *         languages absent from the response fall back to the English value
     */
    public Map<String, Map<String, String>> translateLabelMaps(
            final Map<String, String> englishLabels,
            final String sourceLanguage,
            final List<String> targetLanguages) {

        List<String> targets = filterTargets(targetLanguages, sourceLanguage);

        Map<String, Map<String, String>> result = new LinkedHashMap<>();
        for (String lang : targets) {
            result.put(lang, new LinkedHashMap<>());
        }

        if (targets.isEmpty() || !translationApiProperties.isEnabled()
                || englishLabels == null || englishLabels.isEmpty()) {
            // Fallback: populate each language with the English labels unchanged.
            for (String lang : targets) {
                if (englishLabels != null) {
                    result.get(lang).putAll(englishLabels);
                }
            }
            return result;
        }

        for (Map.Entry<String, String> entry : englishLabels.entrySet()) {
            String labelKey = entry.getKey();
            String englishValue = entry.getValue();

            if (englishValue == null || englishValue.isBlank()) {
                for (String lang : targets) {
                    result.get(lang).put(labelKey, englishValue != null ? englishValue : "");
                }
                continue;
            }

            // One translateMulti call per label, covering all target languages at once.
            Map<String, Optional<String>> perLang =
                    translationApiClient.translateMulti(englishValue, sourceLanguage, targets);

            for (String lang : targets) {
                String translated = perLang.getOrDefault(lang, Optional.empty())
                        .orElse(englishValue);           // fall back to English on failure
                result.get(lang).put(labelKey, translated);
            }
        }

        return result;
    }
    private enum FieldKind { TITLE, BODY, DYNAMIC_VALUE, TAG, AUTHOR_NAME }

    /**
     * Records the position of a translatable string within the document list so that
     * the translated value can be re-injected after translation.
     *
     * @param docIdx       index of the document in the list
     * @param kind         which kind of field this refers to
     * @param key          map key — only used for {@link FieldKind#DYNAMIC_VALUE}
     * @param tagIdx       list index — only used for {@link FieldKind#TAG} and {@link FieldKind#AUTHOR_NAME}
     * @param innerKey     when the dynamic-data value is itself a Map, this is the key inside
     *                     that inner map whose String value was extracted for translation.
     *                     {@code null} when the dynamic-data value is a plain String.
     * @param innerListIdx when the inner-map value at {@code innerKey} is a {@code List},
     *                     this is the index of the String element to translate.
     *                     {@code -1} when the inner-map value is a plain String (not a List).
     */
    private record FieldRef(int docIdx, FieldKind kind, String key, int tagIdx, String innerKey, int innerListIdx) {
        /** Convenience constructor for non-nested fields (innerKey = null, innerListIdx = -1). */
        FieldRef(int docIdx, FieldKind kind, String key, int tagIdx) {
            this(docIdx, kind, key, tagIdx, null, -1);
        }
        /** Convenience constructor for nested-map String fields (innerListIdx = -1). */
        FieldRef(int docIdx, FieldKind kind, String key, int tagIdx, String innerKey) {
            this(docIdx, kind, key, tagIdx, innerKey, -1);
        }
    }
}