package com.integration.management.config.lock;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * Simple distributed lock implementation using PostgreSQL advisory locks.
 * Ensures a critical section can run only once across all application instances.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DistributedLockService {

    private final DataSource dataSource;

    /**
     * Executes the action if the lock can be acquired immediately; otherwise throws IllegalStateException.
     * The lock is held for the duration of the action and then released.
     *
     * @param lockName logical lock name
     * @param action   code to execute while holding the lock
     */
    public void executeWithLockOrThrow(String lockName, Runnable action) {
        if (!tryExecuteWithLock(lockName, action)) {
            throw new IllegalStateException("Lock '" + lockName + "' is currently held by another process");
        }
    }

    /**
     * Attempts to acquire the lock immediately and, if successful, runs the action while holding the lock.
     * Returns true if the lock was acquired and action executed; false otherwise.
     */
    public boolean tryExecuteWithLock(String lockName, Runnable action) {
        long key = toAdvisoryKey(lockName);
        Connection connection = null;
        boolean previousAutoCommit = true;
        try {
            connection = DataSourceUtils.getConnection(dataSource);
            previousAutoCommit = connection.getAutoCommit();
            boolean acquired = tryAdvisoryLock(connection, key);
            if (!acquired) {
                return false;
            }
            try {
                action.run();
            } finally {
                releaseAdvisoryLock(connection, key);
            }
            return true;
        } catch (Exception e) {
            throw new RuntimeException("Failed during locked execution for '" + lockName + "': " + e.getMessage(), e);
        } finally {
            if (connection != null) {
                try {
                    connection.setAutoCommit(previousAutoCommit);
                } catch (Exception ignore) {
                }
                DataSourceUtils.releaseConnection(connection, dataSource);
            }
        }
    }

    private boolean tryAdvisoryLock(Connection connection, long key) throws Exception {
        try (PreparedStatement ps = connection.prepareStatement("select pg_try_advisory_lock(?)")) {
            ps.setLong(1, key);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getBoolean(1);
                }
                return false;
            }
        }
    }

    private void releaseAdvisoryLock(Connection connection, long key) throws Exception {
        try (PreparedStatement ps = connection.prepareStatement("select pg_advisory_unlock(?)")) {
            ps.setLong(1, key);
            ps.execute();
        }
    }

    private long toAdvisoryKey(String name) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(name.getBytes());
            return ByteBuffer.wrap(digest, 0, 8).getLong();
        } catch (Exception e) {
            return name.hashCode();
        }
    }
}
