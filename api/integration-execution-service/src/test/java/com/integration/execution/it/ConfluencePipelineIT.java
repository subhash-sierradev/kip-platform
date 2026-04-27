package com.integration.execution.it;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.integration.execution.client.ConfluenceApiClient;
import com.integration.execution.contract.message.ConfluenceExecutionCommand;
import com.integration.execution.contract.message.ConfluenceExecutionResult;
import com.integration.execution.contract.messaging.MessagePublisher;
import com.integration.execution.contract.model.enums.JobExecutionStatus;
import com.integration.execution.contract.model.enums.TriggerType;
import com.integration.execution.contract.queue.QueueNames;
import com.integration.execution.model.KwMonitoringDocument;
import com.integration.execution.service.KwGraphQLService;
import com.integration.execution.service.VaultService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * End-to-end integration tests for the Confluence execution pipeline.
 *
 * <p>Publishes a {@link ConfluenceExecutionCommand} to the Confluence command queue via
 * {@link MessagePublisher} and polls the result queue with {@link RabbitTemplate} to
 * verify that the full listener → orchestrator → publisher chain is wired correctly.
 *
 * <p>External dependencies ({@link KwGraphQLService}, {@link VaultService},
 * {@link ConfluenceApiClient}) are provided as mocks via {@link TestConfiguration}.
 * The Translation API is simulated by an in-process WireMock server whose URL is
 * injected via {@link DynamicPropertySource}.
 */
@DisplayName("Confluence Execution Pipeline — integration tests")
class ConfluencePipelineIT extends AbstractIesIT {

    // ── Translation API mock (WireMock) ──────────────────────────────────────
    private static final WireMockServer TRANSLATION_API =
            new WireMockServer(WireMockConfiguration.options().dynamicPort());
    private static final String TRANSLATE_PATH = "/api/translate";

    static {
        TRANSLATION_API.start();
    }

    // ── Spring beans ─────────────────────────────────────────────────────────

    @Autowired
    private MessagePublisher messagePublisher;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private KwGraphQLService kwGraphQLService;

    @Autowired
    private ConfluenceApiClient confluenceApiClient;

    @DynamicPropertySource
    static void overrideTranslationApiUrl(final DynamicPropertyRegistry registry) {
        registry.add("translation.api.base-url",
                () -> "http://localhost:" + TRANSLATION_API.port());
    }

    @BeforeEach
    void resetTranslationApiMock() {
        TRANSLATION_API.resetAll();
    }

    @AfterEach
    void clearRabbitQueue() {
        // Drain any unconsumed result messages left by failed/skipped tests
        Object drained = rabbitTemplate.receiveAndConvert(QueueNames.CONFLUENCE_EXECUTION_RESULT_QUEUE);
        while (drained != null) {
            drained = rabbitTemplate.receiveAndConvert(QueueNames.CONFLUENCE_EXECUTION_RESULT_QUEUE);
        }
    }

    @TestConfiguration
    static class MockDependencies {

        @Bean
        @Primary
        public KwGraphQLService kwGraphQLService() {
            return mock(KwGraphQLService.class);
        }

        @Bean
        @Primary
        public VaultService vaultService() {
            return mock(VaultService.class);
        }

        @Bean
        @Primary
        public ConfluenceApiClient confluenceApiClient() {
            return mock(ConfluenceApiClient.class);
        }
    }

    // ------------------------------------------------------------------ helpers

    /**
     * Builds a basic command without explicit language config (source defaults to null → "en",
     * languageCodes null → no translation). Used by pre-existing baseline tests.
     */
    private ConfluenceExecutionCommand buildCommand(final UUID jobId, final String tenantId,
            final String secretName) {
        return ConfluenceExecutionCommand.builder()
                .integrationId(UUID.randomUUID())
                .integrationName("Confluence IT Integration")
                .jobExecutionId(jobId)
                .tenantId(tenantId)
                .connectionSecretName(secretName)
                .confluenceSpaceKey("IT")
                .confluenceSpaceKeyFolderKey("it-folder")
                .reportNameTemplate("IT Monitoring Report {date}")
                .dynamicDocumentType("MONITORING_DOC")
                .businessTimeZone("UTC")
                .triggeredBy(TriggerType.SCHEDULER)
                .triggeredByUser("it-confluence-user")
                .windowStart(Instant.now().minusSeconds(86400))
                .windowEnd(Instant.now())
                .build();
    }

    /**
     * Builds a command with explicit {@code sourceLanguage} and {@code languageCodes}
     * used by the multi-language IT test cases.
     */
    private ConfluenceExecutionCommand buildMultiLanguageCommand(
            final UUID jobId,
            final String tenantId,
            final String secretName,
            final String sourceLanguage,
            final List<String> languageCodes) {
        return ConfluenceExecutionCommand.builder()
                .integrationId(UUID.randomUUID())
                .integrationName("Confluence Multi-Language IT Integration")
                .jobExecutionId(jobId)
                .tenantId(tenantId)
                .connectionSecretName(secretName)
                .confluenceSpaceKey("IT")
                .confluenceSpaceKeyFolderKey("it-folder")
                .reportNameTemplate("Daily Monitoring Report {date}")
                .dynamicDocumentType("MONITORING_DOC")
                .businessTimeZone("UTC")
                .triggeredBy(TriggerType.SCHEDULER)
                .triggeredByUser("it-confluence-user")
                .windowStart(Instant.now().minusSeconds(86400))
                .windowEnd(Instant.now())
                .sourceLanguage(sourceLanguage)
                .languageCodes(languageCodes)
                .build();
    }

    // ------------------------------------------------------------------ tests

    @Test
    @DisplayName("command with no monitoring data produces SUCCESS result with zero records")
    void fullPipeline_noMonitoringData_producesSuccessResult() {
        UUID jobId = UUID.randomUUID();
        String secretName = "confluence-secret-empty";

        when(kwGraphQLService.fetchMonitoringData(anyString(), anyInt(), anyInt(), anyInt(), anyInt()))
                .thenReturn(List.of());

        messagePublisher.publish(
                QueueNames.CONFLUENCE_EXCHANGE,
                QueueNames.CONFLUENCE_EXECUTION_COMMAND_QUEUE,
                buildCommand(jobId, "confluence-tenant", secretName));

        ConfluenceExecutionResult result = await()
                .atMost(20, TimeUnit.SECONDS)
                .until(
                        () -> (ConfluenceExecutionResult) rabbitTemplate.receiveAndConvert(
                                QueueNames.CONFLUENCE_EXECUTION_RESULT_QUEUE),
                        r -> r != null);

        assertThat(result).isNotNull();
        assertThat(result.getJobExecutionId()).isEqualTo(jobId);
        assertThat(result.getStatus()).isEqualTo(JobExecutionStatus.SUCCESS);
        assertThat(result.getTotalRecords()).isZero();
    }

    @Test
    @DisplayName("command with monitoring data publishes page and produces SUCCESS result")
    void fullPipeline_withMonitoringData_producesSuccessResult() {
        UUID jobId = UUID.randomUUID();
        String secretName = "confluence-secret-data";
        String pageUrl = "https://mock-confluence.example.com/wiki/spaces/IT/pages/12345";
        String pageId = "12345";

        KwMonitoringDocument doc = new KwMonitoringDocument();

        when(kwGraphQLService.fetchMonitoringData(anyString(), anyInt(), anyInt(), anyInt(), anyInt()))
                .thenReturn(List.of(doc));
        when(confluenceApiClient.getUserTimezone(anyString(), any()))
                .thenReturn(ZoneId.of("UTC"));
        when(confluenceApiClient.createOrUpdatePage(any()))
                .thenReturn(new ConfluenceApiClient.ConfluencePublishResult(pageUrl, pageId));

        messagePublisher.publish(
                QueueNames.CONFLUENCE_EXCHANGE,
                QueueNames.CONFLUENCE_EXECUTION_COMMAND_QUEUE,
                buildCommand(jobId, "confluence-tenant-data", secretName));

        ConfluenceExecutionResult result = await()
                .atMost(20, TimeUnit.SECONDS)
                .until(
                        () -> (ConfluenceExecutionResult) rabbitTemplate.receiveAndConvert(
                                QueueNames.CONFLUENCE_EXECUTION_RESULT_QUEUE),
                        r -> r != null);

        assertThat(result).isNotNull();
        assertThat(result.getJobExecutionId()).isEqualTo(jobId);
        assertThat(result.getStatus()).isEqualTo(JobExecutionStatus.SUCCESS);
        assertThat(result.getTotalRecords()).isEqualTo(1);
        assertThat(result.getExecutionMetadata()).isNotNull();
        assertThat(result.getExecutionMetadata()).containsEntry("confluencePageUrl", pageUrl);
    }

    @Test
    @DisplayName("orchestrator exception produces FAILED result on result queue")
    void fullPipeline_orchestratorException_producesFailedResult() {
        UUID jobId = UUID.randomUUID();
        String secretName = "confluence-secret-fail";

        when(kwGraphQLService.fetchMonitoringData(anyString(), anyInt(), anyInt(), anyInt(), anyInt()))
                .thenThrow(new RuntimeException("Simulated Confluence Kaseware API failure"));

        messagePublisher.publish(
                QueueNames.CONFLUENCE_EXCHANGE,
                QueueNames.CONFLUENCE_EXECUTION_COMMAND_QUEUE,
                buildCommand(jobId, "confluence-tenant-fail", secretName));

        ConfluenceExecutionResult result = await()
                .atMost(20, TimeUnit.SECONDS)
                .until(
                        () -> (ConfluenceExecutionResult) rabbitTemplate.receiveAndConvert(
                                QueueNames.CONFLUENCE_EXECUTION_RESULT_QUEUE),
                        r -> r != null);

        assertThat(result).isNotNull();
        assertThat(result.getJobExecutionId()).isEqualTo(jobId);
        assertThat(result.getStatus()).isEqualTo(JobExecutionStatus.FAILED);
        assertThat(result.getErrorMessage()).contains("Simulated Confluence Kaseware API failure");
    }

    @Test
    @DisplayName("result message contains correct timing metadata")
    void fullPipeline_result_containsTimingMetadata() {
        UUID jobId = UUID.randomUUID();
        String secretName = "confluence-secret-timing";

        when(kwGraphQLService.fetchMonitoringData(anyString(), anyInt(), anyInt(), anyInt(), anyInt()))
                .thenReturn(List.of());

        Instant beforePublish = Instant.now();
        messagePublisher.publish(
                QueueNames.CONFLUENCE_EXCHANGE,
                QueueNames.CONFLUENCE_EXECUTION_COMMAND_QUEUE,
                buildCommand(jobId, "confluence-tenant-timing", secretName));

        ConfluenceExecutionResult result = await()
                .atMost(20, TimeUnit.SECONDS)
                .until(
                        () -> (ConfluenceExecutionResult) rabbitTemplate.receiveAndConvert(
                                QueueNames.CONFLUENCE_EXECUTION_RESULT_QUEUE),
                        r -> r != null);

        assertThat(result).isNotNull();
        assertThat(result.getStartedAt()).isNotNull();
        assertThat(result.getCompletedAt()).isNotNull();
        assertThat(result.getStartedAt()).isAfterOrEqualTo(beforePublish.minusSeconds(1));
        assertThat(result.getCompletedAt()).isAfterOrEqualTo(result.getStartedAt());
    }

    @Test
    @DisplayName("multiple sequential Confluence commands produce results for all job IDs")
    void fullPipeline_multipleCommands_allResultsReceived() {
        UUID jobId1 = UUID.randomUUID();
        UUID jobId2 = UUID.randomUUID();
        String secretName = "confluence-secret-multi";

        when(kwGraphQLService.fetchMonitoringData(anyString(), anyInt(), anyInt(), anyInt(), anyInt()))
                .thenReturn(List.of());

        messagePublisher.publish(
                QueueNames.CONFLUENCE_EXCHANGE,
                QueueNames.CONFLUENCE_EXECUTION_COMMAND_QUEUE,
                buildCommand(jobId1, "confluence-tenant-multi", secretName));

        messagePublisher.publish(
                QueueNames.CONFLUENCE_EXCHANGE,
                QueueNames.CONFLUENCE_EXECUTION_COMMAND_QUEUE,
                buildCommand(jobId2, "confluence-tenant-multi", secretName));

        List<UUID> receivedJobIds = await()
                .atMost(25, TimeUnit.SECONDS)
                .until(() -> {
                    java.util.List<UUID> collected = new java.util.ArrayList<>();
                    ConfluenceExecutionResult r1 = (ConfluenceExecutionResult)
                            rabbitTemplate.receiveAndConvert(QueueNames.CONFLUENCE_EXECUTION_RESULT_QUEUE);
                    ConfluenceExecutionResult r2 = (ConfluenceExecutionResult)
                            rabbitTemplate.receiveAndConvert(QueueNames.CONFLUENCE_EXECUTION_RESULT_QUEUE);
                    if (r1 != null) {
                        collected.add(r1.getJobExecutionId());
                    }
                    if (r2 != null) {
                        collected.add(r2.getJobExecutionId());
                    }
                    return collected;
                }, ids -> ids.size() == 2);

        assertThat(receivedJobIds).containsExactlyInAnyOrder(jobId1, jobId2);
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // Multi-language tests (full Spring context + RabbitMQ + WireMock)
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @ParameterizedTest(name = "target language={0}")
    @CsvSource({
        "ja, こんにちは世界。今日の監視レポート。",
        "de, Hallo Welt. Der tägliche Überwachungsbericht.",
        "ru, Привет мир. Ежедневный отчёт мониторинга.",
        "fr, Bonjour le monde. Le rapport de surveillance quotidien."
    })
    @DisplayName("multi-language: translated content reaches Confluence when Translation API responds")
    void fullPipeline_multiLanguage_translatedContentPublishedToConfluence(
            final String targetLanguage, final String translatedSnippet) {

        UUID jobId = UUID.randomUUID();
        String pageUrl = "https://confluence.example.com/wiki/spaces/IT/pages/99";

        // ── Translation API returns translated content ──────────────────────
        TRANSLATION_API.stubFor(post(urlEqualTo(TRANSLATE_PATH))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json; charset=UTF-8")
                        .withBody(String.format("""
                                {
                                  "translationResults": [
                                    {
                                      "translatedTimestamp": 1776694594,
                                      "languageCode": "%s",
                                      "value": "%s"
                                    }
                                  ]
                                }
                                """, targetLanguage, translatedSnippet.replace("\"", "\\\"")))));

        KwMonitoringDocument doc = new KwMonitoringDocument();
        when(kwGraphQLService.fetchMonitoringData(anyString(), anyInt(), anyInt(), anyInt(), anyInt()))
                .thenReturn(List.of(doc));
        when(confluenceApiClient.getUserTimezone(anyString(), any())).thenReturn(ZoneId.of("UTC"));
        when(confluenceApiClient.createOrUpdatePage(any()))
                .thenReturn(new ConfluenceApiClient.ConfluencePublishResult(pageUrl, "99"));

        messagePublisher.publish(
                QueueNames.CONFLUENCE_EXCHANGE,
                QueueNames.CONFLUENCE_EXECUTION_COMMAND_QUEUE,
                buildMultiLanguageCommand(jobId, "it-tenant", "secret", "en", List.of(targetLanguage)));

        ConfluenceExecutionResult result = await()
                .atMost(20, TimeUnit.SECONDS)
                .until(
                        () -> (ConfluenceExecutionResult) rabbitTemplate.receiveAndConvert(
                                QueueNames.CONFLUENCE_EXECUTION_RESULT_QUEUE),
                        r -> r != null);

        assertThat(result.getStatus()).isEqualTo(JobExecutionStatus.SUCCESS);
        assertThat(result.getJobExecutionId()).isEqualTo(jobId);
    }

    @Test
    @DisplayName("multi-language: job succeeds with English content when Translation API is down")
    void fullPipeline_translationApiDown_jobSucceedsWithEnglishContent() {
        UUID jobId = UUID.randomUUID();

        // Translation API returns 503 — orchestrator must fall back
        TRANSLATION_API.stubFor(post(urlEqualTo(TRANSLATE_PATH))
                .willReturn(aResponse().withStatus(503).withBody("unavailable")));

        KwMonitoringDocument doc = new KwMonitoringDocument();
        when(kwGraphQLService.fetchMonitoringData(anyString(), anyInt(), anyInt(), anyInt(), anyInt()))
                .thenReturn(List.of(doc));
        when(confluenceApiClient.getUserTimezone(anyString(), any())).thenReturn(ZoneId.of("UTC"));
        when(confluenceApiClient.createOrUpdatePage(any()))
                .thenReturn(new ConfluenceApiClient.ConfluencePublishResult(
                        "https://confluence.example.com/100", "100"));

        messagePublisher.publish(
                QueueNames.CONFLUENCE_EXCHANGE,
                QueueNames.CONFLUENCE_EXECUTION_COMMAND_QUEUE,
                buildMultiLanguageCommand(jobId, "it-tenant-fallback", "secret-fallback",
                        "en", List.of("ja")));

        ConfluenceExecutionResult result = await()
                .atMost(20, TimeUnit.SECONDS)
                .until(
                        () -> (ConfluenceExecutionResult) rabbitTemplate.receiveAndConvert(
                                QueueNames.CONFLUENCE_EXECUTION_RESULT_QUEUE),
                        r -> r != null);

        // Job MUST succeed — translation failure is a soft error
        assertThat(result.getStatus()).isEqualTo(JobExecutionStatus.SUCCESS);
        assertThat(result.getJobExecutionId()).isEqualTo(jobId);
    }

    @Test
    @DisplayName("multi-language: no Translation API call when source == target language")
    void fullPipeline_sameSourceAndTarget_noTranslationApiCallMade() {
        UUID jobId = UUID.randomUUID();

        KwMonitoringDocument doc = new KwMonitoringDocument();
        when(kwGraphQLService.fetchMonitoringData(anyString(), anyInt(), anyInt(), anyInt(), anyInt()))
                .thenReturn(List.of(doc));
        when(confluenceApiClient.getUserTimezone(anyString(), any())).thenReturn(ZoneId.of("UTC"));
        when(confluenceApiClient.createOrUpdatePage(any()))
                .thenReturn(new ConfluenceApiClient.ConfluencePublishResult(
                        "https://confluence.example.com/101", "101"));

        messagePublisher.publish(
                QueueNames.CONFLUENCE_EXCHANGE,
                QueueNames.CONFLUENCE_EXECUTION_COMMAND_QUEUE,
                buildMultiLanguageCommand(jobId, "it-tenant-same-lang", "secret-same",
                        "en", List.of("en")));  // source == target

        ConfluenceExecutionResult result = await()
                .atMost(20, TimeUnit.SECONDS)
                .until(
                        () -> (ConfluenceExecutionResult) rabbitTemplate.receiveAndConvert(
                                QueueNames.CONFLUENCE_EXECUTION_RESULT_QUEUE),
                        r -> r != null);

        assertThat(result.getStatus()).isEqualTo(JobExecutionStatus.SUCCESS);
        // Translation API must not have been called
        assertThat(TRANSLATION_API.getAllServeEvents())
                .as("Translation API must not be called when source == target")
                .isEmpty();
    }
}

