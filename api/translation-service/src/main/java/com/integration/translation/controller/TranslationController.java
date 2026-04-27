package com.integration.translation.controller;

import com.integration.translation.model.request.TranslationRequest;
import com.integration.translation.model.response.TranslationResponse;
import com.integration.translation.service.TranslationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller exposing the Translation API.
 *
 * <h3>Endpoint summary</h3>
 * <table border="1">
 *   <caption>Translation endpoints</caption>
 *   <tr><th>Method</th><th>Path</th><th>Description</th></tr>
 *   <tr>
 *     <td>POST</td>
 *     <td>/api/translate</td>
 *     <td>Translate text into one or more target languages via
 *         the configured {@link TranslationService} backend.</td>
 *   </tr>
 * </table>
 *
 * <h3>Example request</h3>
 * <pre>{@code
 * POST /api/translate
 * Content-Type: application/json
 *
 * {
 *   "textToTranslate": "Hello world. Please translate this document.",
 *   "sourceLanguage": "en",
 *   "languageCodes": ["ja", "ru"]
 * }
 * }</pre>
 *
 * <h3>Example response</h3>
 * <pre>{@code
 * HTTP/1.1 200 OK
 * Content-Type: application/json;charset=UTF-8
 *
 * {
 *   "cognitiveServicesUsage": { "translatorTranslateTextCharacterCount": 88 },
 *   "translationResults": [
 *     { "languageCode": "ja", "value": "こんにちは…", "translatedTimestamp": 1776694594 },
 *     { "languageCode": "ru", "value": "Привет мир…", "translatedTimestamp": 1776694596 }
 *   ],
 *   "extractOnDisk": false
 * }
 * }</pre>
 *
 * <h3>Error responses</h3>
 * <ul>
 *   <li>{@code 400 Bad Request} — missing or invalid request fields</li>
 *   <li>{@code 422 Unprocessable Entity} — non-recoverable translation failure</li>
 *   <li>{@code 500 Internal Server Error} — unexpected runtime error</li>
 * </ul>
 *
 * <p>All error responses use the RFC 7807 {@code application/problem+json} format.</p>
 */
@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping(
        path = "/api/translate",
        produces = MediaType.APPLICATION_JSON_VALUE
)
public class TranslationController {

    private final TranslationService translationService;

    /**
     * Translates the submitted text into every target language specified in
     * {@code languageCodes}.
     *
     * <p>The request body is fully validated before the service layer is
     * invoked. Constraint violations result in an immediate 400 response
     * (see {@link com.integration.translation.exception.GlobalExceptionHandler}).</p>
     *
     * @param request validated translation request body
     * @return 200 OK with the assembled {@link TranslationResponse}
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TranslationResponse> translate(
            @Valid @RequestBody final TranslationRequest request) {

        log.info("POST /api/translate — sourceLang={}, targets={}, textLength={}",
                request.getSourceLanguage(),
                request.getLanguageCodes(),
                request.getTextToTranslate().length());

        TranslationResponse response = translationService.translate(request);

        log.info("POST /api/translate — {} result(s) returned",
                response.getTranslationResults() == null
                    ? 0
                    : response.getTranslationResults().size());

        return ResponseEntity.ok(response);
    }
}

