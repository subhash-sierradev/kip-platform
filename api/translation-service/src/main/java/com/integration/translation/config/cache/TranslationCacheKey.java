package com.integration.translation.config.cache;

/**
 * Immutable, fixed-size cache key for a single translation request.
 *
 * <h3>Why not a plain String?</h3>
 * <p>Concatenating the full source text into the key string (e.g.
 * {@code text + "::" + sourceLang + "::" + targetLang}) has three drawbacks:</p>
 * <ul>
 *   <li><strong>Memory</strong> — texts up to 50 000 characters produce equally
 *       large key strings that must be kept alive for the lifetime of each cache
 *       entry.</li>
 *   <li><strong>Performance</strong> — {@link String#hashCode()} iterates every
 *       character; for 50 k chars on every cache lookup this is measurably slow
 *       under concurrent load.</li>
 *   <li><strong>Collision risk</strong> — if the delimiter ({@code "::"}) appears
 *       in the text, two different logical keys produce identical strings.</li>
 * </ul>
 *
 * <h3>This approach</h3>
 * <p>The source text is reduced to a 64-character hexadecimal SHA-256 digest by
 * {@link TranslationCacheKeyGenerator} before the key is constructed.  This
 * gives:</p>
 * <ul>
 *   <li><strong>Constant size</strong> — every key is
 *       {@code 64 + 2 + len(sourceLang) + len(targetLang)} characters, regardless
 *       of input text length.</li>
 *   <li><strong>Fast hashing</strong> — Java records derive
 *       {@link #hashCode()} and {@link #equals(Object)} from the three fields,
 *       all of which are short strings.</li>
 *   <li><strong>Collision resistance</strong> — SHA-256 provides 128-bit
 *       collision resistance; accidental same-key for different texts is
 *       negligible.</li>
 * </ul>
 *
 * @param textSha256 lowercase hexadecimal SHA-256 digest of the source text
 *                   (always 64 characters)
 * @param sourceLang BCP-47 source language code (e.g. {@code "en"})
 * @param targetLang BCP-47 target language code (e.g. {@code "ja"})
 */
public record TranslationCacheKey(
        String textSha256,
        String sourceLang,
        String targetLang) {
    // Java record auto-generates equals(), hashCode(), and toString() from the
    // three components — no manual implementation needed.
}

