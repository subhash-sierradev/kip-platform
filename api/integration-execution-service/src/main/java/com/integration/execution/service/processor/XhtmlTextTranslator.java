package com.integration.execution.service.processor;

import com.integration.execution.client.TranslationApiClient;
import com.integration.execution.config.properties.TranslationApiProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.parser.Parser;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Translates only the human-readable text nodes of a Confluence Storage Format XHTML document,
 * leaving all markup, macro tags ({@code <ac:*>}), and structural attributes intact.
 *
 * <h3>Algorithm</h3>
 * <ol>
 *   <li>Parse the XHTML with jsoup's XML parser (preserves namespace tags exactly).</li>
 *   <li>Walk every text node; skip nodes that are inside
 *       {@code <ac:parameter ac:name="colour">} (Confluence API constants),
 *       purely numeric, hex-colour, or punctuation-only text.</li>
 *   <li>Batch the remaining text nodes into chunks of at most
 *       {@link TranslationApiProperties#getMaxChars()} characters joined by {@link #SEG}.
 *       This stays well below the Translation API's character limit.</li>
 *   <li>Translate each batch via {@link TranslationApiClient}. If the returned segment count
 *       does not match the input, the original text is kept for that batch (graceful fallback).</li>
 *   <li>Re-inject the translated strings into the jsoup DOM and serialise back to XHTML.</li>
 * </ol>
 *
 * <p>Any exception during parsing or re-injection causes an immediate fall-back to the
 * original XHTML string — the pipeline never fails due to translation errors.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class XhtmlTextTranslator {

    /** Segment separator used when batching multiple text nodes into one API call. */
    static final String SEG = "<<<SEG>>>";

    /** Compiled pattern for splitting on the segment separator. */
    private static final Pattern SEG_PATTERN = Pattern.compile(Pattern.quote("\n" + SEG + "\n"));

    /** Text that is purely a CSS hex colour value — must not be translated. */
    private static final Pattern HEX_COLOR = Pattern.compile("^#[0-9A-Fa-f]{3,8}$");

    /** Text containing only digits, spaces, and common numeric punctuation. */
    private static final Pattern PURE_NUMBER = Pattern.compile("^[\\d\\s.,+%()\\-/]+$");

    /** Text containing only whitespace or punctuation characters. */
    private static final Pattern PURE_PUNCTUATION =
            Pattern.compile("^[\\s\\-\u2013\u2014.,:;|/\\\\]+$");

    private final TranslationApiClient translationApiClient;
    private final TranslationApiProperties translationApiProperties;

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /**
     * Translates the human-readable text nodes of {@code xhtml} from {@code sourceLanguage}
     * to {@code targetLanguage}.
     *
     * @param xhtml           Confluence Storage Format XHTML produced by FreeMarker
     * @param sourceLanguage  BCP-47 source language code (e.g. {@code "en"})
     * @param targetLanguage  BCP-47 target language code (e.g. {@code "ja"})
     * @return the XHTML with translated text nodes, or {@code xhtml} unchanged on any failure
     */
    public String translateXhtml(final String xhtml,
                                 final String sourceLanguage,
                                 final String targetLanguage) {
        if (xhtml == null || xhtml.isBlank()) {
            return xhtml;
        }
        try {
            Document doc = Jsoup.parse(xhtml, "", Parser.xmlParser());
            doc.outputSettings().prettyPrint(false);

            List<TextNode> nodes = new ArrayList<>();
            collectTranslatableNodes(doc, nodes);

            if (nodes.isEmpty()) {
                log.debug("No translatable text nodes found; returning original XHTML.");
                return xhtml;
            }

            List<String> originals = nodes.stream().map(TextNode::text).toList();
            int totalChars = originals.stream().mapToInt(String::length).sum();
            log.info("Translating {} text nodes ({} chars) from '{}' to '{}'",
                    nodes.size(), totalChars, sourceLanguage, targetLanguage);

            List<String> translated = translateInBatches(originals, sourceLanguage, targetLanguage);

            for (int i = 0; i < nodes.size(); i++) {
                nodes.get(i).text(translated.get(i));
            }

            return doc.outerHtml();

        } catch (Exception ex) {
            log.warn("XhtmlTextTranslator: DOM translation failed; returning original XHTML. "
                    + "Error: {}", ex.getMessage(), ex);
            return xhtml;
        }
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /**
     * Recursively walks the DOM and collects all translatable {@link TextNode}s
     * into {@code result}. Skips {@code <ac:parameter ac:name="colour">} subtrees
     * (Confluence colour API constants that must remain in English).
     */
    private void collectTranslatableNodes(final Element element, final List<TextNode> result) {
        for (Node child : element.childNodes()) {
            if (child instanceof TextNode textNode) {
                String text = textNode.text().trim();
                if (isTranslatable(text)) {
                    result.add(textNode);
                }
            } else if (child instanceof Element childEl) {
                // Skip colour parameter nodes — these are Confluence-specific API constants.
                if ("ac:parameter".equalsIgnoreCase(childEl.tagName())
                        && "colour".equalsIgnoreCase(childEl.attr("ac:name"))) {
                    continue;
                }
                collectTranslatableNodes(childEl, result);
            }
        }
    }

    /** Returns {@code true} when {@code text} should be sent to the translation API. */
    private boolean isTranslatable(final String text) {
        if (text.isBlank()) {
            return false;
        }
        if (HEX_COLOR.matcher(text).matches()) {
            return false;
        }
        if (PURE_NUMBER.matcher(text).matches()) {
            return false;
        }
        if (PURE_PUNCTUATION.matcher(text).matches()) {
            return false;
        }
        return true;
    }

    /**
     * Splits {@code texts} into batches of at most
     * {@link TranslationApiProperties#getMaxChars()} characters, translates each
     * batch, and returns the full list of translated strings in the same order.
     * Falls back to the original string for any batch that fails.
     */
    private List<String> translateInBatches(final List<String> texts,
                                            final String src,
                                            final String target) {
        List<String> result = new ArrayList<>(texts);
        int start = 0;
        int batchCharLimit = translationApiProperties.getMaxChars();

        while (start < texts.size()) {
            // Build the next batch
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

            // Translate the batch as a single API call
            String joined = String.join("\n" + SEG + "\n", batch);
            Optional<String> translatedOpt = translationApiClient.translate(joined, src, target);

            if (translatedOpt.isPresent()) {
                String[] parts = SEG_PATTERN.split(translatedOpt.get(), -1);
                if (parts.length == batch.size()) {
                    for (int i = 0; i < batch.size(); i++) {
                        result.set(start + i, parts[i].trim());
                    }
                    log.debug("Batch [{}-{}]: {} nodes translated successfully.",
                            start, end - 1, batch.size());
                } else {
                    log.warn("Batch [{}-{}]: received {} segments but expected {}; "
                            + "keeping original text for this batch.",
                            start, end - 1, parts.length, batch.size());
                }
            } else {
                log.warn("Batch [{}-{}]: Translation API returned no result; "
                        + "keeping original text for this batch.", start, end - 1);
            }

            start = end;
        }

        return result;
    }
}

