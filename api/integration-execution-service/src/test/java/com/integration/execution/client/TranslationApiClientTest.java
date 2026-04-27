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
}

