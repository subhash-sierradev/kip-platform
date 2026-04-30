package com.integration.execution.client;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.integration.execution.config.properties.TranslationApiProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit/integration tests for {@link TranslationApiClient}.
 *
 * <p>A WireMock HTTP server is started on a random port so the real
 * {@link java.net.http.HttpClient} is exercised end-to-end without
 * requiring a live Translation Service.</p>
 */
class TranslationApiClientTest {

    private static final String TRANSLATE_PATH = "/api/translate";
    private static final String EN_CONTENT = "<p>Hello World.</p>";

    private WireMockServer wireMock;
    private TranslationApiClient client;

    @BeforeEach
    void setUp() {
        wireMock = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        wireMock.start();

        TranslationApiProperties props = new TranslationApiProperties();
        props.setBaseUrl("http://localhost:" + wireMock.port());
        props.setEnabled(true);
        props.setTimeoutSeconds(10);

        client = new TranslationApiClient(props, new ObjectMapper());
    }

    @AfterEach
    void tearDown() {
        wireMock.stop();
    }

    @Test
    void translate_successfulResponse_returnsTranslatedValue() {
        wireMock.stubFor(post(urlEqualTo(TRANSLATE_PATH))
                .withHeader("Content-Type", equalTo("application/json; charset=UTF-8"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "translationResults": [
                                    {
                                      "translatedTimestamp": 1776694594,
                                      "languageCode": "ja",
                                      "value": "<p>こんにちは世界。</p>"
                                    }
                                  ]
                                }
                                """)));

        Optional<String> result = client.translate(EN_CONTENT, "en", "ja");

        assertThat(result).isPresent()
                .hasValue("<p>こんにちは世界。</p>");
    }

    @Test
    void translate_http500Response_returnsEmpty() {
        wireMock.stubFor(post(urlEqualTo(TRANSLATE_PATH))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withBody("Internal Server Error")));

        Optional<String> result = client.translate(EN_CONTENT, "en", "de");

        assertThat(result).isEmpty();
    }

    @Test
    void translate_http400Response_returnsEmpty() {
        wireMock.stubFor(post(urlEqualTo(TRANSLATE_PATH))
                .willReturn(aResponse()
                        .withStatus(400)
                        .withBody("{\"error\": \"bad request\"}")));

        Optional<String> result = client.translate(EN_CONTENT, "en", "xx");

        assertThat(result).isEmpty();
    }

    @Test
    void translate_emptyTranslationResults_returnsEmpty() {
        wireMock.stubFor(post(urlEqualTo(TRANSLATE_PATH))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"translationResults\": []}")));

        Optional<String> result = client.translate(EN_CONTENT, "en", "ja");

        assertThat(result).isEmpty();
    }

    @Test
    void translate_nullTranslationResults_returnsEmpty() {
        wireMock.stubFor(post(urlEqualTo(TRANSLATE_PATH))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"translationResults\": null}")));

        Optional<String> result = client.translate(EN_CONTENT, "en", "ja");

        assertThat(result).isEmpty();
    }

    @Test
    void translate_blankValueInResult_returnsEmpty() {
        wireMock.stubFor(post(urlEqualTo(TRANSLATE_PATH))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "translationResults": [
                                    { "translatedTimestamp": 0, "languageCode": "ja", "value": "   " }
                                  ]
                                }
                                """)));

        Optional<String> result = client.translate(EN_CONTENT, "en", "ja");

        assertThat(result).isEmpty();
    }

    @Test
    void translate_connectionRefused_returnsEmpty() {
        // Point to a port nothing is listening on
        wireMock.stop();
        TranslationApiProperties props = new TranslationApiProperties();
        props.setBaseUrl("http://localhost:1"); // port 1 — always refused
        props.setEnabled(true);
        props.setTimeoutSeconds(2);

        TranslationApiClient unavailableClient = new TranslationApiClient(props, new ObjectMapper());

        Optional<String> result = unavailableClient.translate(EN_CONTENT, "en", "ja");

        assertThat(result).isEmpty();
    }

    @Test
    void translate_malformedJsonResponse_returnsEmpty() {
        wireMock.stubFor(post(urlEqualTo(TRANSLATE_PATH))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("not valid json {{{")));

        Optional<String> result = client.translate(EN_CONTENT, "en", "ja");

        assertThat(result).isEmpty();
    }

    // ── Fallback-explanation detection ───────────────────────────────────────

    @Test
    void translate_llmEchosOriginalText_treatedAsFallbackAndReturnsEmpty() {
        // LLM echoes the input then appends an English explanation (heuristic 1)
        String echoed = EN_CONTENT + "\nThis is the phonetic rendering of your input.";
        wireMock.stubFor(post(urlEqualTo(TRANSLATE_PATH))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "translationResults": [
                                    { "translatedTimestamp": 0, "languageCode": "ja", "value": "%s" }
                                  ]
                                }
                                """.formatted(echoed.replace("\"", "\\\"")))));

        Optional<String> result = client.translate(EN_CONTENT, "en", "ja");

        assertThat(result).as("echoed input must be rejected as a fallback explanation").isEmpty();
    }


    @Test
    void translate_llmReturnsAsciiOnlyForNonLatinTarget_treatedAsFallbackAndReturnsEmpty() {
        // LLM responded in English (ASCII-only) for a Japanese target — heuristic 2
        wireMock.stubFor(post(urlEqualTo(TRANSLATE_PATH))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "translationResults": [
                                    {
                                      "translatedTimestamp": 0,
                                      "languageCode": "ja",
                                      "value": "This is the phonetic translation of your text."
                                    }
                                  ]
                                }
                                """)));

        Optional<String> result = client.translate("zvdxfbg", "en", "ja");

        assertThat(result).as("ASCII-only response for non-Latin target must be rejected").isEmpty();
    }

    @Test
    void translate_latinTargetLanguageWithAsciiResponse_accepted() {
        // German is a Latin-script language — ASCII response is perfectly valid
        wireMock.stubFor(post(urlEqualTo(TRANSLATE_PATH))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "translationResults": [
                                    { "translatedTimestamp": 0, "languageCode": "de", "value": "Hallo Welt" }
                                  ]
                                }
                                """)));

        Optional<String> result = client.translate("Hello World", "en", "de");

        assertThat(result).isPresent().hasValue("Hallo Welt");
    }

    @Test
    void translate_nonLatinTargetWithNonAsciiResponse_accepted() {
        // Russian response contains Cyrillic — should be accepted for "ru" target
        wireMock.stubFor(post(urlEqualTo(TRANSLATE_PATH))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "translationResults": [
                                    { "translatedTimestamp": 0, "languageCode": "ru", "value": "Привет мир" }
                                  ]
                                }
                                """)));

        Optional<String> result = client.translate("Hello World", "en", "ru");

        assertThat(result).isPresent().hasValue("Привет мир");
    }

    // ── English parenthetical note stripping ─────────────────────────────────

    @Test
    void translate_japanesePlusEnglishParenthetical_stripsEnglishNote() {
        // API returns "ずぶんだすふぐ\n(Japanese transcription of \"zvdxfbg\")"
        // Only the Japanese part should be returned.
        wireMock.stubFor(post(urlEqualTo(TRANSLATE_PATH))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "translationResults": [
                                    {
                                      "translatedTimestamp": 0,
                                      "languageCode": "ja",
                                      "value": "ずぶんだすふぐ\\n(Japanese transcription of \\"zvdxfbg\\")"
                                    }
                                  ]
                                }
                                """)));

        Optional<String> result = client.translate("zvdxfbg", "en", "ja");

        assertThat(result).isPresent().hasValue("ずぶんだすふぐ");
    }

    @Test
    void translate_noParenthetical_returnedAsIs() {
        // Clean translation with no English suffix — must not be modified
        wireMock.stubFor(post(urlEqualTo(TRANSLATE_PATH))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "translationResults": [
                                    { "translatedTimestamp": 0, "languageCode": "ja", "value": "こんにちは" }
                                  ]
                                }
                                """)));

        Optional<String> result = client.translate("Hello", "en", "ja");

        assertThat(result).isPresent().hasValue("こんにちは");
    }

    @Test
    void translate_nonAsciiParenthetical_strippedAtNewline() {
        // Parenthetical after \n is always an LLM explanation — unconditionally truncated
        // regardless of whether it contains non-ASCII characters.
        wireMock.stubFor(post(urlEqualTo(TRANSLATE_PATH))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "translationResults": [
                                    {
                                      "translatedTimestamp": 0,
                                      "languageCode": "ja",
                                      "value": "こんにちは\\n(世界への挨拶)"
                                    }
                                  ]
                                }
                                """)));

        Optional<String> result = client.translate("Hello", "en", "ja");

        assertThat(result).isPresent().hasValue("こんにちは");
    }

    @Test
    void translate_nullLanguageCodeInResult_isSkippedAndReturnsEmpty() {
        // API returns a result with null languageCode — must be ignored, not throw NPE
        wireMock.stubFor(post(urlEqualTo(TRANSLATE_PATH))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "translationResults": [
                                    { "translatedTimestamp": 0, "languageCode": null, "value": "ignored" }
                                  ]
                                }
                                """)));

        Optional<String> result = client.translate(EN_CONTENT, "en", "ja");

        assertThat(result).isEmpty();
    }

    // ── cleanTranslationResponse — LLM artifact stripping ────────────────────

    @Test
    void translate_reportDetailsWithNewlineArtifact_stripsEnglishSuffix() {
        // Real-world Ollama pattern: "報告詳細\n\nHere is your translation: ...\n\nPlease note..."
        String dirty = "報告詳細\\n\\nHere is your translation: 報告詳細\\n\\nPlease note that I am an AI.";
        wireMock.stubFor(post(urlEqualTo(TRANSLATE_PATH))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "translationResults": [
                                    { "translatedTimestamp": 0, "languageCode": "ja",
                                      "value": "%s" }
                                  ]
                                }
                                """.formatted(dirty))));

        Optional<String> result = client.translate("Report Details", "en", "ja");

        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo("報告詳細");
    }

    @Test
    void translate_authorLabelWithInlineEnglishSuffix_stripsEnglishSuffix() {
        // "著者 Here is the translation of your text 'Authors' into Japanese."
        String dirty = "著者 Here is the translation of your text 'Authors' into Japanese.";
        wireMock.stubFor(post(urlEqualTo(TRANSLATE_PATH))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "translationResults": [
                                    { "translatedTimestamp": 0, "languageCode": "ja",
                                      "value": "%s" }
                                  ]
                                }
                                """.formatted(dirty))));

        Optional<String> result = client.translate("Authors", "en", "ja");

        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo("著者");
    }

    @Test
    void translate_leadingHereIsYourTranslationPreamble_stripsToTranslationOnly() {
        // "Here is your translation: 報告詳細"
        wireMock.stubFor(post(urlEqualTo(TRANSLATE_PATH))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "translationResults": [
                                    { "translatedTimestamp": 0, "languageCode": "ja",
                                      "value": "Here is your translation: 報告詳細" }
                                  ]
                                }
                                """)));

        Optional<String> result = client.translate("Report Details", "en", "ja");

        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo("報告詳細");
    }

    @Test
    void translate_machineTranslationDisclaimerAfterDoubleNewline_stripped() {
        // Real-world pattern: "報告詳細\n\nThis translation is a machine translation..."
        String dirty = "報告詳細\\n\\nThis translation is a machine translation of an English text. "
                + "Human review and editing are highly recommended for accurate translations.";
        wireMock.stubFor(post(urlEqualTo(TRANSLATE_PATH))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "translationResults": [
                                    { "translatedTimestamp": 0, "languageCode": "ja",
                                      "value": "%s" }
                                  ]
                                }
                                """.formatted(dirty))));

        Optional<String> result = client.translate("Report Details", "en", "ja");

        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo("報告詳細");
    }

    @Test
    void translate_cleanJapaneseResponse_returnedUnchanged() {
        wireMock.stubFor(post(urlEqualTo(TRANSLATE_PATH))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "translationResults": [
                                    { "translatedTimestamp": 0, "languageCode": "ja",
                                      "value": "報告詳細" }
                                  ]
                                }
                                """)));

        Optional<String> result = client.translate("Report Details", "en", "ja");

        assertThat(result).isPresent().hasValue("報告詳細");
    }

    // ── cleanTranslationResponse — direct unit tests ──────────────────────────

    @Test
    void cleanTranslationResponse_nullInput_returnsNull() {
        assertThat(client.cleanTranslationResponse(null)).isNull();
    }

    @Test
    void cleanTranslationResponse_blankInput_returnsBlank() {
        assertThat(client.cleanTranslationResponse("   ")).isBlank();
    }

    @Test
    void cleanTranslationResponse_cleanText_returnedUnchanged() {
        assertThat(client.cleanTranslationResponse("報告詳細")).isEqualTo("報告詳細");
    }

    @Test
    void cleanTranslationResponse_newlineArtifact_stripsToFirstLine() {
        String raw = "報告詳細\nHere is your translation: 報告詳細\nPlease note that I am an AI.";
        assertThat(client.cleanTranslationResponse(raw)).isEqualTo("報告詳細");
    }

    @Test
    void cleanTranslationResponse_doubleNewlineArtifact_stripsToFirstLine() {
        String raw = "報告詳細\n\nHere is your translation: 報告詳細\n\nPlease note that I am an AI.";
        assertThat(client.cleanTranslationResponse(raw)).isEqualTo("報告詳細");
    }

    @Test
    void cleanTranslationResponse_leadingPreamble_stripsToTranslationOnly() {
        assertThat(client.cleanTranslationResponse("Here is your translation: 報告詳細"))
                .isEqualTo("報告詳細");
    }

    @Test
    void cleanTranslationResponse_leadingPreambleNoColon_returnedAsIs() {
        // "please note something" has no colon — safety guard: return text unchanged
        String text = "please note something in Japanese: 著者";
        // Has colon, so remainder "著者" is returned
        assertThat(client.cleanTranslationResponse(text)).isNotBlank();
    }

    @Test
    void cleanTranslationResponse_inlineEnglishSuffix_stripped() {
        String raw = "著者 Here is the translation of your text 'Authors' into Japanese.";
        assertThat(client.cleanTranslationResponse(raw)).isEqualTo("著者");
    }

    @Test
    void cleanTranslationResponse_inlineSuffixWithNonAscii_notStripped() {
        // Suffix is non-ASCII → not an English note → keep as-is
        String raw = "著者 Here is日本語テキスト";
        assertThat(client.cleanTranslationResponse(raw)).isEqualTo("著者 Here is日本語テキスト");
    }

    @Test
    void cleanTranslationResponse_preambleWithoutColon_returnedAsIs() {
        // "please note text here" starts with artifact prefix but has no colon
        // → safety guard: return unchanged text (covers the no-colon branch)
        String text = "please note this has no colon so it is returned unchanged";
        String result = client.cleanTranslationResponse(text);
        assertThat(result).isEqualTo(text);
    }

    @Test
    void cleanTranslationResponse_multiLineInput_truncatedAtFirstNewline() {
        // All translatable values are single-line; everything after \n is always discarded
        String raw = "第一行\n第二行";
        assertThat(client.cleanTranslationResponse(raw)).isEqualTo("第一行");
    }

    @Test
    void cleanTranslationResponse_nonAsciiParentheticalAfterNewline_truncated() {
        // Real-world case: "総合レポート\n(total reports can be translated as \"総合\" which means...)"
        // Previously NOT stripped because isAsciiParenthetical returned false (non-ASCII inside).
        String raw = "総合レポート\n(total reports can be translated as \"総合\" which means \"total\" or \"comprehensive\".)";
        assertThat(client.cleanTranslationResponse(raw)).isEqualTo("総合レポート");
    }

    @Test
    void cleanTranslationResponse_parentheticalExplanationWithTranslatedWordAtEnd_stripped() {
        // "合計レポート (The translation of 'Total Reports' in Japanese is '合計レポート'.)"
        // The first 40 chars of suffix are ASCII → stripped
        String raw = "合計レポート (The translation of \"Total Reports\" in Japanese is \"合計レポート\".)";
        assertThat(client.cleanTranslationResponse(raw)).isEqualTo("合計レポート");
    }

    @Test
    void cleanTranslationResponse_parentheticalTranslationInJapanese_stripped() {
        // "(Translation in Japanese: レポートの詳細)" after newline → artifact line prefix
        String raw = "レポートの詳細\n(Translation in Japanese: レポートの詳細)";
        assertThat(client.cleanTranslationResponse(raw)).isEqualTo("レポートの詳細");
    }

    // ── isNonLatinScriptLanguage — additional switch cases ───────────────────

    @Test
    void translate_chineseTargetAsciiResponse_treatedAsFallback() {
        wireMock.stubFor(post(urlEqualTo(TRANSLATE_PATH))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "translationResults": [
                                    { "translatedTimestamp": 0, "languageCode": "zh",
                                      "value": "Only ASCII text here, no Chinese characters." }
                                  ]
                                }
                                """)));

        Optional<String> result = client.translate("Hello", "en", "zh");

        assertThat(result).as("ASCII-only response for Chinese target must be rejected").isEmpty();
    }

    @Test
    void translate_koreanTargetAsciiResponse_treatedAsFallback() {
        wireMock.stubFor(post(urlEqualTo(TRANSLATE_PATH))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "translationResults": [
                                    { "translatedTimestamp": 0, "languageCode": "ko",
                                      "value": "Only ASCII text, no Korean." }
                                  ]
                                }
                                """)));

        Optional<String> result = client.translate("Hello", "en", "ko");

        assertThat(result).as("ASCII-only response for Korean target must be rejected").isEmpty();
    }

    @Test
    void translate_arabicTargetAsciiResponse_treatedAsFallback() {
        wireMock.stubFor(post(urlEqualTo(TRANSLATE_PATH))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "translationResults": [
                                    { "translatedTimestamp": 0, "languageCode": "ar",
                                      "value": "Only ASCII text, no Arabic script." }
                                  ]
                                }
                                """)));

        Optional<String> result = client.translate("Hello", "en", "ar");

        assertThat(result).as("ASCII-only response for Arabic target must be rejected").isEmpty();
    }
}

