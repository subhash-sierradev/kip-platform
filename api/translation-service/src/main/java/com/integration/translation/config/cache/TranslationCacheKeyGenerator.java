package com.integration.translation.config.cache;

import com.integration.translation.config.TranslationCacheConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Spring {@link KeyGenerator} that produces compact, collision-resistant cache keys
 * for the {@code translations} Caffeine cache.
 *
 * <h3>Key structure</h3>
 * <p>Each key is a {@link TranslationCacheKey} record whose {@code textSha256}
 * component is the lowercase hexadecimal SHA-256 digest of the source text.
 * The digest is always 64 characters long, so key size is bounded regardless of
 * how long the source text is (up to 50 000 characters).</p>
 *
 * <h3>Registration</h3>
 * <p>This bean is referenced by name in the {@code @Cacheable} annotation on
 * {@link com.integration.translation.service.OllamaCachedTranslator}:</p>
 * <pre>{@code
 * @Cacheable(
 *     cacheNames = TranslationCacheConfig.CACHE_TRANSLATIONS,
 *     keyGenerator = TranslationCacheConfig.KEY_GENERATOR
 * )
 * }</pre>
 *
 * <h3>Parameter contract</h3>
 * <p>This generator is tightly coupled to the signature of
 * {@code OllamaCachedTranslator.translateSingleLanguage(String text,
 * String sourceLang, String targetLang)}.  The expected parameter order is:</p>
 * <ol>
 *   <li>{@code params[0]} — source text ({@link String})</li>
 *   <li>{@code params[1]} — source language BCP-47 code ({@link String})</li>
 *   <li>{@code params[2]} — target language BCP-47 code ({@link String})</li>
 * </ol>
 *
 * <h3>Thread safety</h3>
 * <p>{@link MessageDigest} is <em>not</em> thread-safe, so a new instance is
 * obtained for every call via {@link MessageDigest#getInstance(String)}.
 * The JDK caches the algorithm lookup internally, making this negligibly cheap
 * compared to the digest computation itself.</p>
 */
@Slf4j
@Component(TranslationCacheConfig.KEY_GENERATOR)
public class TranslationCacheKeyGenerator implements KeyGenerator {

    /** SHA-256 algorithm name, always available in any Java SE runtime. */
    private static final String SHA_256 = "SHA-256";

    /**
     * Produces a {@link TranslationCacheKey} from the method parameters.
     *
     * @param target the bean instance (unused)
     * @param method the cached method (unused)
     * @param params {@code [text, sourceLang, targetLang]} — must not be null
     * @return a compact, immutable cache key
     */
    @Override
    public Object generate(
            final Object target,
            final Method method,
            final Object... params) {

        String text       = (String) params[0];
        String sourceLang = (String) params[1];
        String targetLang = (String) params[2];

        String digest = sha256Hex(text);

        log.trace("Cache key generated: sourceLang={}, targetLang={}, digest={}",
                sourceLang, targetLang, digest);

        return new TranslationCacheKey(digest, sourceLang, targetLang);
    }

    /**
     * Computes the SHA-256 digest of {@code text} and returns it as a lowercase
     * hexadecimal string of exactly 64 characters.
     *
     * @param text the source text; encoded as UTF-8 before hashing
     * @return 64-character hexadecimal digest
     * @throws IllegalStateException if the JVM does not provide SHA-256
     *         (cannot happen in any standard Java SE runtime)
     */
    static String sha256Hex(final String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance(SHA_256);
            byte[] hash = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            // SHA-256 is mandated by the Java SE specification; this path is unreachable.
            throw new IllegalStateException("SHA-256 algorithm not available in this JVM", ex);
        }
    }
}


