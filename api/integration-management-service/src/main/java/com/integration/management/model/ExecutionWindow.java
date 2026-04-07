package com.integration.management.model;

import java.time.Instant;

/**
 * Immutable value representing the time window for an integration
 * job execution. Both boundaries are in UTC.
 *
 * @param windowStart inclusive start of the processing window
 * @param windowEnd   exclusive end of the processing window
 */
public record ExecutionWindow(Instant windowStart, Instant windowEnd) {
}
