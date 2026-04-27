package com.integration.translation.config.cache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TranslationCacheKeyGeneratorTest {

    private TranslationCacheKeyGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new TranslationCacheKeyGenerator();
    }

    // ── sha256Hex ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("sha256Hex() produces a 64-character lowercase hex string")
    void sha256Hex_producesFixed64CharHex() {
        String digest = TranslationCacheKeyGenerator.sha256Hex("Hello world");
        assertThat(digest).hasSize(64).matches("[0-9a-f]+");
    }

    @Test
    @DisplayName("sha256Hex() is deterministic — same input always gives same digest")
    void sha256Hex_isDeterministic() {
        String d1 = TranslationCacheKeyGenerator.sha256Hex("Hello world");
        String d2 = TranslationCacheKeyGenerator.sha256Hex("Hello world");
        assertThat(d1).isEqualTo(d2);
    }

    @Test
    @DisplayName("sha256Hex() produces different digests for different texts")
    void sha256Hex_differentTexts_differentDigests() {
        String d1 = TranslationCacheKeyGenerator.sha256Hex("Hello world");
        String d2 = TranslationCacheKeyGenerator.sha256Hex("Hello world.");
        assertThat(d1).isNotEqualTo(d2);
    }

    @Test
    @DisplayName("sha256Hex() handles 50 000-character text and still produces 64 chars")
    void sha256Hex_largeInput_stillFixed64Chars() {
        String large = "a".repeat(50_000);
        assertThat(TranslationCacheKeyGenerator.sha256Hex(large)).hasSize(64);
    }

    @Test
    @DisplayName("sha256Hex() handles empty string")
    void sha256Hex_emptyString_produces64CharDigest() {
        assertThat(TranslationCacheKeyGenerator.sha256Hex("")).hasSize(64);
    }

    @Test
    @DisplayName("sha256Hex() handles multi-byte UTF-8 characters (Japanese)")
    void sha256Hex_multiByteCharacters_produces64CharDigest() {
        assertThat(TranslationCacheKeyGenerator.sha256Hex("こんにちは、世界。")).hasSize(64);
    }

    // ── generate() / key structure ────────────────────────────────────────────

    @Test
    @DisplayName("generate() returns a TranslationCacheKey with correct language codes")
    void generate_returnsTranslationCacheKey() {
        Object key = generator.generate(null, null, "Hello", "en", "ja");

        assertThat(key).isInstanceOf(TranslationCacheKey.class);
        TranslationCacheKey cacheKey = (TranslationCacheKey) key;
        assertThat(cacheKey.sourceLang()).isEqualTo("en");
        assertThat(cacheKey.targetLang()).isEqualTo("ja");
    }

    @Test
    @DisplayName("generate() textSha256 component is always 64 chars regardless of text length")
    void generate_textSha256IsAlways64Chars() {
        Object key = generator.generate(null, null, "x".repeat(50_000), "en", "ru");
        String digest = ((TranslationCacheKey) key).textSha256();
        assertThat(digest).hasSize(64);
    }

    @Test
    @DisplayName("generate() same inputs → equal keys (cache hit)")
    void generate_sameInputs_equalKeys() {
        Object k1 = generator.generate(null, null, "Hello world", "en", "ja");
        Object k2 = generator.generate(null, null, "Hello world", "en", "ja");
        assertThat(k1).isEqualTo(k2);
        assertThat(k1.hashCode()).isEqualTo(k2.hashCode());
    }

    @Test
    @DisplayName("generate() different text → different keys (cache miss)")
    void generate_differentText_differentKeys() {
        Object k1 = generator.generate(null, null, "Hello", "en", "ja");
        Object k2 = generator.generate(null, null, "Hello world", "en", "ja");
        assertThat(k1).isNotEqualTo(k2);
    }

    @Test
    @DisplayName("generate() different targetLang → different keys")
    void generate_differentTargetLang_differentKeys() {
        Object k1 = generator.generate(null, null, "Hello", "en", "ja");
        Object k2 = generator.generate(null, null, "Hello", "en", "ru");
        assertThat(k1).isNotEqualTo(k2);
    }

    @Test
    @DisplayName("generate() key size is constant regardless of text length")
    void generate_keySizeIsConstant() {
        // Both keys should have the same SHA-256 field length (64) despite very
        // different text lengths — proving the large-text memory saving.
        TranslationCacheKey small = (TranslationCacheKey)
                generator.generate(null, null, "Hi", "en", "ja");
        TranslationCacheKey large = (TranslationCacheKey)
                generator.generate(null, null, "x".repeat(50_000), "en", "ja");

        assertThat(small.textSha256()).hasSize(64);
        assertThat(large.textSha256()).hasSize(64);
    }

    @Test
    @DisplayName("delimiter collision: texts containing '::' produce different keys")
    void generate_textContainingDelimiter_noCollision() {
        // With a plain-string key, "a::b" + "::" + "c" == "a" + "::" + "b::c"
        // — a true collision. With SHA-256 this cannot happen.
        Object k1 = generator.generate(null, null, "a::b", "en", "c");
        Object k2 = generator.generate(null, null, "a",    "en", "b::c");
        assertThat(k1).isNotEqualTo(k2);
    }
}

