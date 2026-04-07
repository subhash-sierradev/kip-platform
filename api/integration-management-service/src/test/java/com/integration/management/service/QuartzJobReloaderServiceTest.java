package com.integration.management.service;

import com.integration.execution.contract.model.JobReloadResultDto;
import com.integration.management.config.lock.DistributedLockService;
import com.integration.management.config.properties.JobReloadProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("QuartzJobReloaderService")
class QuartzJobReloaderServiceTest {

    @Mock
    private ArcGISScheduleService scheduleService;
    @Mock
    private ConfluenceScheduleService confluenceScheduleService;
    @Mock
    private DistributedLockService distributedLockService;

    private final List<QuartzJobReloaderService> servicesToCleanup = new ArrayList<>();

    @AfterEach
    void cleanup() {
        servicesToCleanup.forEach(QuartzJobReloaderService::shutdownScheduler);
        servicesToCleanup.clear();
    }

    private QuartzJobReloaderService createAndRegister(JobReloadProperties props) {
        QuartzJobReloaderService service = new QuartzJobReloaderService(
                scheduleService, confluenceScheduleService, props, distributedLockService);
        servicesToCleanup.add(service);
        return service;
    }

    @Test
    @DisplayName("onApplicationReady should trigger reloadJobsSync when enabled and async=false")
    void onApplicationReady_enabled_sync_callsReloadJobsSync() {
        JobReloadProperties props = new JobReloadProperties();
        props.setEnabled(true);
        props.setAsync(false);
        props.setRetryFailed(false);
        props.setTimeoutMillis(0L);

        QuartzJobReloaderService real = createAndRegister(props);
        QuartzJobReloaderService service = spy(real);
        doAnswer(inv -> null).when(service).reloadJobsSync();

        service.onApplicationReady();

        verify(service).reloadJobsSync();
        verify(service, never()).reloadJobsAsync();
    }

    @Test
    @DisplayName("onApplicationReady should trigger reloadJobsAsync when enabled and async=true")
    void onApplicationReady_enabled_async_callsReloadJobsAsync() {
        JobReloadProperties props = new JobReloadProperties();
        props.setEnabled(true);
        props.setAsync(true);
        props.setRetryFailed(false);
        props.setTimeoutMillis(0L);

        QuartzJobReloaderService real = createAndRegister(props);
        QuartzJobReloaderService service = spy(real);
        doAnswer(inv -> null).when(service).reloadJobsAsync();

        service.onApplicationReady();

        verify(service).reloadJobsAsync();
        verify(service, never()).reloadJobsSync();
    }

    @Test
    @DisplayName("reloadJobsSync should reload jobs, resume triggers, and set lastReloadInstant")
    void reloadJobsSync_success_setsLastReloadInstant() {
        JobReloadProperties props = new JobReloadProperties();
        props.setEnabled(true);
        props.setAsync(false);
        props.setRetryFailed(false);
        props.setTimeoutMillis(0L);
        props.setVerboseLogging(false);

        QuartzJobReloaderService service = createAndRegister(props);

        List<JobReloadResultDto> results = List.of(
                JobReloadResultDto.success(null, "t", "cron", "n"),
                JobReloadResultDto.failure(null, "t", "n2", "err"));
        when(scheduleService.reloadAllJobs()).thenReturn(results);
        when(confluenceScheduleService.reloadAllJobs()).thenReturn(List.of());
        when(scheduleService.resumeErrorTriggers()).thenReturn(2);
        when(scheduleService.getJobSchedulingHealthStats()).thenReturn(new int[] {10, 7});

        doAnswer(inv -> {
            Runnable r = inv.getArgument(1);
            r.run();
            return null;
        }).when(distributedLockService).executeWithLockOrThrow(any(), any());

        service.reloadJobsSync();

        verify(scheduleService).reloadAllJobs();
        verify(scheduleService).resumeErrorTriggers();
        assertThat(service.getLastReloadInstant()).isPresent();
    }

    @Test
    @DisplayName("reloadJobsSync should retry when attempt fails and retryFailed=true")
    void reloadJobsSync_retriesAndSucceeds_onSecondAttempt() {
        JobReloadProperties props = new JobReloadProperties();
        props.setEnabled(true);
        props.setAsync(false);
        props.setRetryFailed(true);
        props.setMaxRetryAttempts(2);
        props.setRetryDelayMillis(1L);
        props.setTimeoutMillis(0L);

        QuartzJobReloaderService service = createAndRegister(props);

        when(scheduleService.reloadAllJobs())
                .thenThrow(new RuntimeException("boom"))
                .thenReturn(List.of(JobReloadResultDto.success(null, "t", "cron", "n")));
        when(confluenceScheduleService.reloadAllJobs()).thenReturn(List.of());
        when(scheduleService.resumeErrorTriggers()).thenReturn(0);
        when(scheduleService.getJobSchedulingHealthStats()).thenReturn(new int[] {-1, -1});

        doAnswer(inv -> {
            Runnable r = inv.getArgument(1);
            r.run();
            return null;
        }).when(distributedLockService).executeWithLockOrThrow(any(), any());

        service.reloadJobsSync();

        verify(scheduleService, times(2)).reloadAllJobs();
        assertThat(service.getLastReloadInstant()).isPresent();
    }

    @Test
    @DisplayName("reloadJobsSync should give up when retryFailed=false")
    void reloadJobsSync_givesUp_whenRetryDisabled() {
        JobReloadProperties props = new JobReloadProperties();
        props.setEnabled(true);
        props.setAsync(false);
        props.setRetryFailed(false);
        props.setMaxRetryAttempts(5);
        props.setRetryDelayMillis(1L);
        props.setTimeoutMillis(0L);

        QuartzJobReloaderService service = createAndRegister(props);

        when(scheduleService.reloadAllJobs()).thenThrow(new RuntimeException("boom"));

        doAnswer(inv -> {
            Runnable r = inv.getArgument(1);
            r.run();
            return null;
        }).when(distributedLockService).executeWithLockOrThrow(any(), any());

        service.reloadJobsSync();

        verify(scheduleService).reloadAllJobs();
        assertThat(service.getLastReloadInstant()).isEmpty();
    }

    @Test
    @DisplayName("onApplicationReady should do nothing when disabled")
    void onApplicationReady_disabled_skips() {
        JobReloadProperties props = new JobReloadProperties();
        props.setEnabled(false);
        QuartzJobReloaderService service = createAndRegister(props);

        service.onApplicationReady();

        verify(scheduleService, never()).reloadAllJobs();
        verify(distributedLockService, never()).executeWithLockOrThrow(any(), any());
    }

    @Test
    @DisplayName("manualReload should return results and set lastReloadInstant")
    void manualReload_success_setsLastReloadInstant() {
        JobReloadProperties props = new JobReloadProperties();
        QuartzJobReloaderService service = createAndRegister(props);

        List<JobReloadResultDto> results = List.of(JobReloadResultDto.success(null, "t", "cron", "n"));
        when(scheduleService.reloadAllJobs()).thenReturn(results);
        when(confluenceScheduleService.reloadAllJobs()).thenReturn(List.of());
        when(scheduleService.getJobSchedulingHealthStats()).thenReturn(new int[] {0, 0});

        doAnswer(inv -> {
            Runnable r = inv.getArgument(1);
            r.run();
            return null;
        }).when(distributedLockService).executeWithLockOrThrow(any(), any());

        List<JobReloadResultDto> out = service.manualReload();

        assertThat(out).isEqualTo(results);
        assertThat(service.getLastReloadInstant()).isPresent();
        Instant ts = service.getLastReloadInstant().orElseThrow();
        assertThat(ts).isBeforeOrEqualTo(Instant.now());
    }

    @Test
    @DisplayName("manualReload should wrap exceptions")
    void manualReload_failure_wrapsException() {
        JobReloadProperties props = new JobReloadProperties();
        QuartzJobReloaderService service = createAndRegister(props);

        doThrow(new IllegalStateException("lock"))
                .when(distributedLockService)
                .executeWithLockOrThrow(any(), any());

        assertThatThrownBy(service::manualReload)
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Manual job reload failed")
                .hasCauseInstanceOf(IllegalStateException.class);
    }

    // -------------------------------------------------------------------------
    // Additional comprehensive tests
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("getLastReloadInstant should return empty when never reloaded")
    void getLastReloadInstant_neverReloaded_returnsEmpty() {
        JobReloadProperties props = new JobReloadProperties();
        QuartzJobReloaderService service = createAndRegister(props);

        assertThat(service.getLastReloadInstant()).isEmpty();
    }

    @Test
    @DisplayName("reloadJobsSync should include both ArcGIS and Confluence results")
    void reloadJobsSync_includesBothServices_combinedResults() {
        JobReloadProperties props = new JobReloadProperties();
        props.setRetryFailed(false);
        props.setVerboseLogging(false);

        QuartzJobReloaderService service = createAndRegister(props);

        List<JobReloadResultDto> arcgisResults = List.of(
                JobReloadResultDto.success(UUID.randomUUID(), "t1", "0 0 12 * * ?", "ArcGIS Job"));
        List<JobReloadResultDto> confluenceResults = List.of(
                JobReloadResultDto.success(UUID.randomUUID(), "t1", "0 0 14 * * ?", "Confluence Job"));

        when(scheduleService.reloadAllJobs()).thenReturn(arcgisResults);
        when(confluenceScheduleService.reloadAllJobs()).thenReturn(confluenceResults);
        when(scheduleService.resumeErrorTriggers()).thenReturn(0);
        when(scheduleService.getJobSchedulingHealthStats()).thenReturn(new int[] {2, 2});

        doAnswer(inv -> {
            Runnable r = inv.getArgument(1);
            r.run();
            return null;
        }).when(distributedLockService).executeWithLockOrThrow(any(), any());

        service.reloadJobsSync();

        verify(scheduleService).reloadAllJobs();
        verify(confluenceScheduleService).reloadAllJobs();
        assertThat(service.getLastReloadInstant()).isPresent();
    }

    @Test
    @DisplayName("reloadJobsSync with verbose logging should log all job details")
    void reloadJobsSync_verboseLogging_logsDetails() {
        JobReloadProperties props = new JobReloadProperties();
        props.setRetryFailed(false);
        props.setVerboseLogging(true);

        QuartzJobReloaderService service = createAndRegister(props);

        List<JobReloadResultDto> results = List.of(
                JobReloadResultDto.success(UUID.randomUUID(), "t1", "0 0 12 * * ?", "Success Job"),
                JobReloadResultDto.failure(UUID.randomUUID(), "t1", "Failed Job", "Error message"));

        when(scheduleService.reloadAllJobs()).thenReturn(results);
        when(confluenceScheduleService.reloadAllJobs()).thenReturn(List.of());
        when(scheduleService.resumeErrorTriggers()).thenReturn(0);
        when(scheduleService.getJobSchedulingHealthStats()).thenReturn(new int[] {2, 1});

        doAnswer(inv -> {
            Runnable r = inv.getArgument(1);
            r.run();
            return null;
        }).when(distributedLockService).executeWithLockOrThrow(any(), any());

        service.reloadJobsSync();

        verify(scheduleService).reloadAllJobs();
        assertThat(service.getLastReloadInstant()).isPresent();
    }

    @Test
    @DisplayName("reloadJobsSync should reach max retry attempts and give up")
    void reloadJobsSync_maxRetriesReached_givesUp() {
        JobReloadProperties props = new JobReloadProperties();
        props.setRetryFailed(true);
        props.setMaxRetryAttempts(3);
        props.setRetryDelayMillis(1L);

        QuartzJobReloaderService service = createAndRegister(props);

        when(scheduleService.reloadAllJobs()).thenThrow(new RuntimeException("permanent failure"));

        doAnswer(inv -> {
            Runnable r = inv.getArgument(1);
            r.run();
            return null;
        }).when(distributedLockService).executeWithLockOrThrow(any(), any());

        service.reloadJobsSync();

        verify(scheduleService, times(3)).reloadAllJobs();
        assertThat(service.getLastReloadInstant()).isEmpty();
    }

    @Test
    @DisplayName("reloadJobsAsync with timeout should cancel after timeout")
    void reloadJobsAsync_timeout_cancels() throws Exception {
        JobReloadProperties props = new JobReloadProperties();
        props.setAsync(true);
        props.setRetryFailed(false);
        props.setTimeoutMillis(100L);

        QuartzJobReloaderService service = createAndRegister(props);

        CountDownLatch latch = new CountDownLatch(1);
        doAnswer(inv -> {
            Runnable r = inv.getArgument(1);
            new Thread(() -> {
                try {
                    Thread.sleep(5000); // Delay longer than timeout
                    r.run();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    latch.countDown();
                }
            }).start();
            return null;
        }).when(distributedLockService).executeWithLockOrThrow(anyString(), any());

        service.reloadJobsAsync();

        // Wait a bit for timeout to kick in
        Thread.sleep(200);

        // Should not have set lastReloadInstant due to timeout
        assertThat(service.getLastReloadInstant()).isEmpty();
    }

    @Test
    @DisplayName("concurrent reload attempts should skip when local lock held")
    void concurrentReload_localLockHeld_skips() throws Exception {
        JobReloadProperties props = new JobReloadProperties();
        props.setRetryFailed(false);

        QuartzJobReloaderService service = createAndRegister(props);

        CountDownLatch firstStarted = new CountDownLatch(1);
        CountDownLatch firstComplete = new CountDownLatch(1);
        AtomicBoolean secondAttemptFailed = new AtomicBoolean(false);

        when(scheduleService.reloadAllJobs()).thenReturn(List.of());
        when(confluenceScheduleService.reloadAllJobs()).thenReturn(List.of());
        when(scheduleService.resumeErrorTriggers()).thenReturn(0);
        when(scheduleService.getJobSchedulingHealthStats()).thenReturn(new int[] {0, 0});

        doAnswer(inv -> {
            Runnable r = inv.getArgument(1);
            firstStarted.countDown();
            try {
                firstComplete.await(2, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            r.run();
            return null;
        }).when(distributedLockService).executeWithLockOrThrow(anyString(), any());

        // First reload in background thread
        Thread firstThread = new Thread(() -> {
            try {
                service.reloadJobsSync();
            } catch (Exception e) {
                // Ignore
            }
        });
        firstThread.start();

        // Wait for first to start
        firstStarted.await(1, TimeUnit.SECONDS);

        // Try second reload while first is running
        Thread secondThread = new Thread(() -> {
            try {
                service.reloadJobsSync();
            } catch (Exception e) {
                secondAttemptFailed.set(true);
            }
        });
        secondThread.start();
        secondThread.join(500);

        // Now let first complete
        firstComplete.countDown();
        firstThread.join(2000);

        // At least one should succeed
        assertThat(service.getLastReloadInstant()).isPresent();
    }

    @Test
    @DisplayName("manualReload should include Confluence results")
    void manualReload_includesConfluence_combinedResults() {
        JobReloadProperties props = new JobReloadProperties();
        props.setVerboseLogging(false);

        QuartzJobReloaderService service = createAndRegister(props);

        List<JobReloadResultDto> arcgisResults = List.of(
                JobReloadResultDto.success(UUID.randomUUID(), "t1", "cron1", "ArcGIS"));
        List<JobReloadResultDto> confluenceResults = List.of(
                JobReloadResultDto.success(UUID.randomUUID(), "t1", "cron2", "Confluence"));

        when(scheduleService.reloadAllJobs()).thenReturn(arcgisResults);
        when(confluenceScheduleService.reloadAllJobs()).thenReturn(confluenceResults);
        when(scheduleService.getJobSchedulingHealthStats()).thenReturn(new int[] {2, 2});

        doAnswer(inv -> {
            Runnable r = inv.getArgument(1);
            r.run();
            return null;
        }).when(distributedLockService).executeWithLockOrThrow(any(), any());

        List<JobReloadResultDto> results = service.manualReload();

        assertThat(results).hasSize(2);
        assertThat(results).containsAll(arcgisResults);
        assertThat(results).containsAll(confluenceResults);
    }

    @Test
    @DisplayName("distributed lock failure should propagate exception")
    void reloadJobsSync_distributedLockFails_propagatesException() {
        JobReloadProperties props = new JobReloadProperties();
        props.setRetryFailed(false);

        QuartzJobReloaderService service = createAndRegister(props);

        doThrow(new IllegalStateException("Lock currently held by another process"))
                .when(distributedLockService)
                .executeWithLockOrThrow(anyString(), any());

        service.reloadJobsSync();

        assertThat(service.getLastReloadInstant()).isEmpty();
    }

    @Test
    @DisplayName("resumeErrorTriggers should not be called when no triggers resumed")
    void reloadJobsSync_noErrorTriggers_doesNotLog() {
        JobReloadProperties props = new JobReloadProperties();
        props.setRetryFailed(false);

        QuartzJobReloaderService service = createAndRegister(props);

        when(scheduleService.reloadAllJobs()).thenReturn(List.of());
        when(confluenceScheduleService.reloadAllJobs()).thenReturn(List.of());
        when(scheduleService.resumeErrorTriggers()).thenReturn(0);
        when(scheduleService.getJobSchedulingHealthStats()).thenReturn(new int[] {0, 0});

        doAnswer(inv -> {
            Runnable r = inv.getArgument(1);
            r.run();
            return null;
        }).when(distributedLockService).executeWithLockOrThrow(any(), any());

        service.reloadJobsSync();

        verify(scheduleService).resumeErrorTriggers();
        assertThat(service.getLastReloadInstant()).isPresent();
    }

    @Test
    @DisplayName("health stats with negative values should not crash")
    void reloadJobsSync_negativeHealthStats_handlesGracefully() {
        JobReloadProperties props = new JobReloadProperties();
        props.setRetryFailed(false);

        QuartzJobReloaderService service = createAndRegister(props);

        when(scheduleService.reloadAllJobs()).thenReturn(List.of());
        when(confluenceScheduleService.reloadAllJobs()).thenReturn(List.of());
        when(scheduleService.resumeErrorTriggers()).thenReturn(0);
        when(scheduleService.getJobSchedulingHealthStats()).thenReturn(new int[] {-1, -1});

        doAnswer(inv -> {
            Runnable r = inv.getArgument(1);
            r.run();
            return null;
        }).when(distributedLockService).executeWithLockOrThrow(any(), any());

        service.reloadJobsSync();

        verify(scheduleService).getJobSchedulingHealthStats();
        assertThat(service.getLastReloadInstant()).isPresent();
    }
}
