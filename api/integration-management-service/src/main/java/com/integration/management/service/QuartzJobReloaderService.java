package com.integration.management.service;

import com.integration.execution.contract.model.JobReloadResultDto;
import com.integration.management.config.lock.DistributedLockService;
import com.integration.management.config.properties.JobReloadProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Reloads all active ArcGIS integration Quartz job schedules on application startup.
 * This ensures scheduled integrations resume after restarts or pod recycling.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class QuartzJobReloaderService {

    private final ArcGISScheduleService scheduleService;
    private final ConfluenceScheduleService confluenceScheduleService;
    private final JobReloadProperties jobReloadProperties;
    private final DistributedLockService distributedLockService;

    private final ReentrantLock localReloadLock = new ReentrantLock();

    private final ScheduledExecutorService retryScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "job-reload-retry");
        t.setDaemon(true);
        return t;
    });

    private volatile Instant lastReloadInstant;

    @SuppressWarnings("unused")
    @PreDestroy
    void shutdownScheduler() {
        retryScheduler.shutdownNow();
    }

    @SuppressWarnings("unused")
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        if (!jobReloadProperties.isEnabled()) {
            log.info("Job reload on startup is disabled, skipping job reloading");
            return;
        }
        log.info("Application ready event received, initiating job reload process");
        if (jobReloadProperties.isAsync()) {
            reloadJobsAsync();
        } else {
            reloadJobsSync();
        }
    }

    @Async
    public void reloadJobsAsync() {
        log.info("Starting asynchronous job reload process");
        CompletableFuture<Void> completion = new CompletableFuture<>();
        scheduleAttempt(1, 0, completion);

        if (jobReloadProperties.getTimeoutMillis() > 0) {
            try {
                completion.get(jobReloadProperties.getTimeoutMillis(), TimeUnit.MILLISECONDS);
            } catch (Exception e) {
                log.error("Job reload process timed out after {} ms", jobReloadProperties.getTimeoutMillis(), e);
                completion.cancel(true);
            }
        }
    }

    public void reloadJobsSync() {
        log.info("Starting synchronous job reload process");
        try {
            performJobReloadWithScheduledRetries();
        } catch (Exception e) {
            log.error("Synchronous job reload process failed", e);
        }
    }

    private void scheduleAttempt(int attempt, long delayMillis, CompletableFuture<Void> completionFuture) {
        long effectiveDelay = Math.max(0L, delayMillis);
        retryScheduler.schedule(
                () -> doAttempt(attempt, getMaxAttempts(), completionFuture),
                effectiveDelay,
                TimeUnit.MILLISECONDS);
    }

    private void doAttempt(int attempt, int maxAttempts, CompletableFuture<Void> completionFuture) {
        if (completionFuture.isDone()) {
            return;
        }
        try {
            log.info("Job reload attempt {} of {}", attempt, maxAttempts);
            attemptReloadOnce();
            log.info("Job reload process completed successfully on attempt {}", attempt);
            completionFuture.complete(null);
        } catch (Exception e) {
            log.error("Job reload attempt {} failed: {}", attempt, e.getMessage());
            if (attempt < maxAttempts && jobReloadProperties.isRetryFailed()) {
                long delay = jobReloadProperties.getRetryDelayMillis();
                log.info("Retrying job reload in {} ms", delay);
                scheduleAttempt(attempt + 1, delay, completionFuture);
            } else {
                log.error("Job reload failed after {} attempts, giving up", attempt);
                completionFuture.completeExceptionally(e);
            }
        }
    }

    private void attemptReloadOnce() {
        if (!localReloadLock.tryLock()) {
            log.warn("Another job reload is in progress locally; skipping this attempt");
            throw new IllegalStateException("Local reload already in progress");
        }
        try {
            distributedLockService.executeWithLockOrThrow("job-reload-global-lock", () -> {
                List<JobReloadResultDto> results = new java.util.ArrayList<>(scheduleService.reloadAllJobs());
                results.addAll(confluenceScheduleService.reloadAllJobs());
                processReloadResults(results);
                int resumed = scheduleService.resumeErrorTriggers();
                if (resumed > 0) {
                    log.info("Auto-resumed {} ArcGIS triggers from ERROR state after reload", resumed);
                }
                lastReloadInstant = Instant.now();
            });
        } finally {
            localReloadLock.unlock();
        }
    }

    private void performJobReloadWithScheduledRetries() {
        CompletableFuture<Void> completion = new CompletableFuture<>();
        scheduleAttempt(1, 0, completion);
        try {
            completion.get();
        } catch (Exception e) {
            log.error("Synchronous scheduled-retry flow encountered an error", e);
        }
        if (lastReloadInstant == null) {
            log.error("Job reload process failed completely after all retry attempts");
        }
    }

    private void processReloadResults(List<JobReloadResultDto> results) {
        long successCount = results.stream().filter(JobReloadResultDto::isSuccess).count();
        long failureCount = results.size() - successCount;
        log.info("Job reload summary: {} successful, {} failed out of {} total jobs",
                successCount, failureCount, results.size());

        if (jobReloadProperties.isVerboseLogging()) {
            results.forEach(result -> {
                if (result.isSuccess()) {
                    log.info("Successfully reloaded job: {} ({}) - Cron: {}",
                            result.getIntegrationName(), result.getIntegrationId(), result.getCronExpression());
                } else {
                    log.warn("Failed to reload job: {} ({}) - Error: {}",
                            result.getIntegrationName(), result.getIntegrationId(), result.getErrorMessage());
                }
            });
        } else if (failureCount > 0) {
            results.stream()
                    .filter(result -> !result.isSuccess())
                    .forEach(result -> log.warn("Failed to reload job: {} ({}) - {}",
                            result.getIntegrationName(), result.getIntegrationId(), result.getErrorMessage()));
        }

        int[] healthStats = scheduleService.getJobSchedulingHealthStats();
        if (healthStats[0] >= 0 && healthStats[1] >= 0) {
            log.info("Job scheduling health: {}/{} expected jobs are scheduled in Quartz",
                    healthStats[1], healthStats[0]);
        }
    }

    private int getMaxAttempts() {
        return jobReloadProperties.isRetryFailed()
                ? Math.max(1, jobReloadProperties.getMaxRetryAttempts())
                : 1;
    }

    public List<JobReloadResultDto> manualReload() {
        log.info("Manual job reload triggered");
        try {
            AtomicReference<List<JobReloadResultDto>> ref = new AtomicReference<>();
            distributedLockService.executeWithLockOrThrow("job-reload-global-lock", () -> {
                List<JobReloadResultDto> results = new java.util.ArrayList<>(scheduleService.reloadAllJobs());
                results.addAll(confluenceScheduleService.reloadAllJobs());
                processReloadResults(results);
                lastReloadInstant = Instant.now();
                ref.set(results);
            });
            return ref.get();
        } catch (Exception e) {
            log.error("Manual job reload failed", e);
            throw new RuntimeException("Manual job reload failed: " + e.getMessage(), e);
        }
    }

    public Optional<Instant> getLastReloadInstant() {
        return Optional.ofNullable(lastReloadInstant);
    }
}
