package com.integration.management.config.lock;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("DistributedLockService")
class DistributedLockServiceTest {

    @Mock
    private DataSource dataSource;
    @Mock
    private Connection connection;
    @Mock
    private PreparedStatement tryLockStatement;
    @Mock
    private PreparedStatement unlockStatement;
    @Mock
    private ResultSet resultSet;

    @Test
    @DisplayName("tryExecuteWithLock should run action and release lock when acquired")
    void tryExecuteWithLock_acquired_runsAndReleases() throws Exception {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getAutoCommit()).thenReturn(true);
        when(connection.prepareStatement("select pg_try_advisory_lock(?)")).thenReturn(tryLockStatement);
        when(connection.prepareStatement("select pg_advisory_unlock(?)")).thenReturn(unlockStatement);
        when(tryLockStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getBoolean(1)).thenReturn(true);

        DistributedLockService service = new DistributedLockService(dataSource);
        AtomicBoolean ran = new AtomicBoolean(false);

        boolean acquired = service.tryExecuteWithLock("lock-A", () -> ran.set(true));

        assertThat(acquired).isTrue();
        assertThat(ran.get()).isTrue();
        verify(tryLockStatement).setLong(eq(1), anyLong());
        verify(unlockStatement).setLong(eq(1), anyLong());
        verify(unlockStatement).execute();
    }

    @Test
    @DisplayName("tryExecuteWithLock should return false and not run action when not acquired")
    void tryExecuteWithLock_notAcquired_returnsFalse() throws Exception {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getAutoCommit()).thenReturn(true);
        when(connection.prepareStatement("select pg_try_advisory_lock(?)")).thenReturn(tryLockStatement);
        when(tryLockStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getBoolean(1)).thenReturn(false);

        DistributedLockService service = new DistributedLockService(dataSource);
        AtomicBoolean ran = new AtomicBoolean(false);

        boolean acquired = service.tryExecuteWithLock("lock-B", () -> ran.set(true));

        assertThat(acquired).isFalse();
        assertThat(ran.get()).isFalse();
        verify(connection, never()).prepareStatement("select pg_advisory_unlock(?)");
    }

    @Test
    @DisplayName("executeWithLockOrThrow should throw when lock not acquired")
    void executeWithLockOrThrow_notAcquired_throws() throws Exception {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getAutoCommit()).thenReturn(true);
        when(connection.prepareStatement("select pg_try_advisory_lock(?)")).thenReturn(tryLockStatement);
        when(tryLockStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getBoolean(1)).thenReturn(false);

        DistributedLockService service = new DistributedLockService(dataSource);

        assertThatThrownBy(() -> service.executeWithLockOrThrow("lock-C", () -> {
        }))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("currently held");
    }

    @Test
    @DisplayName("executeWithLockOrThrow should run action when lock acquired")
    void executeWithLockOrThrow_acquired_runsAction() throws Exception {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getAutoCommit()).thenReturn(true);
        when(connection.prepareStatement("select pg_try_advisory_lock(?)")).thenReturn(tryLockStatement);
        when(connection.prepareStatement("select pg_advisory_unlock(?)")).thenReturn(unlockStatement);
        when(tryLockStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getBoolean(1)).thenReturn(true);

        DistributedLockService service = new DistributedLockService(dataSource);
        AtomicBoolean ran = new AtomicBoolean(false);

        service.executeWithLockOrThrow("lock-D", () -> ran.set(true));

        assertThat(ran.get()).isTrue();
        verify(unlockStatement).execute();
    }

    @Test
    @DisplayName("tryExecuteWithLock should release lock even when action throws exception")
    void tryExecuteWithLock_actionThrows_releasesLock() throws Exception {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getAutoCommit()).thenReturn(true);
        when(connection.prepareStatement("select pg_try_advisory_lock(?)")).thenReturn(tryLockStatement);
        when(connection.prepareStatement("select pg_advisory_unlock(?)")).thenReturn(unlockStatement);
        when(tryLockStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getBoolean(1)).thenReturn(true);

        DistributedLockService service = new DistributedLockService(dataSource);

        assertThatThrownBy(() -> service.tryExecuteWithLock("lock-E", () -> {
            throw new RuntimeException("Action failed");
        }))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed during locked execution")
                .hasMessageContaining("Action failed");

        verify(unlockStatement).execute();
    }

    @Test
    @DisplayName("tryExecuteWithLock should handle connection exceptions gracefully")
    void tryExecuteWithLock_connectionException_wrapsException() throws Exception {
        when(dataSource.getConnection()).thenThrow(new RuntimeException("Database unavailable"));

        DistributedLockService service = new DistributedLockService(dataSource);

        assertThatThrownBy(() -> service.tryExecuteWithLock("lock-F", () -> {
        }))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed during locked execution")
                .hasMessageContaining("Database unavailable");
    }

    @Test
    @DisplayName("tryExecuteWithLock should handle resultSet returning false in next()")
    void tryExecuteWithLock_resultSetNextFalse_returnsFalse() throws Exception {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getAutoCommit()).thenReturn(true);
        when(connection.prepareStatement("select pg_try_advisory_lock(?)")).thenReturn(tryLockStatement);
        when(tryLockStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        DistributedLockService service = new DistributedLockService(dataSource);
        AtomicBoolean ran = new AtomicBoolean(false);

        boolean acquired = service.tryExecuteWithLock("lock-G", () -> ran.set(true));

        assertThat(acquired).isFalse();
        assertThat(ran.get()).isFalse();
    }

    @Test
    @DisplayName("tryExecuteWithLock should restore autoCommit setting")
    void tryExecuteWithLock_restoresAutoCommit() throws Exception {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getAutoCommit()).thenReturn(false);
        when(connection.prepareStatement("select pg_try_advisory_lock(?)")).thenReturn(tryLockStatement);
        when(connection.prepareStatement("select pg_advisory_unlock(?)")).thenReturn(unlockStatement);
        when(tryLockStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getBoolean(1)).thenReturn(true);

        DistributedLockService service = new DistributedLockService(dataSource);

        service.tryExecuteWithLock("lock-H", () -> {
        });

        verify(connection).setAutoCommit(false);
    }

    @Test
    @DisplayName("tryExecuteWithLock should suppress setAutoCommit exception in finally block")
    void tryExecuteWithLock_setAutoCommitThrows_suppressesException() throws Exception {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getAutoCommit()).thenReturn(true);
        when(connection.prepareStatement("select pg_try_advisory_lock(?)")).thenReturn(tryLockStatement);
        when(connection.prepareStatement("select pg_advisory_unlock(?)")).thenReturn(unlockStatement);
        when(tryLockStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getBoolean(1)).thenReturn(true);
        doThrow(new RuntimeException("setAutoCommit failed")).when(connection).setAutoCommit(true);

        DistributedLockService service = new DistributedLockService(dataSource);
        AtomicBoolean ran = new AtomicBoolean(false);

        boolean acquired = service.tryExecuteWithLock("lock-I", () -> ran.set(true));

        assertThat(acquired).isTrue();
        assertThat(ran.get()).isTrue();
    }

    @Test
    @DisplayName("toAdvisoryKey should generate consistent keys for same lock name")
    void toAdvisoryKey_sameName_generatesSameKey() throws Exception {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getAutoCommit()).thenReturn(true);
        when(connection.prepareStatement("select pg_try_advisory_lock(?)")).thenReturn(tryLockStatement);
        when(connection.prepareStatement("select pg_advisory_unlock(?)")).thenReturn(unlockStatement);
        when(tryLockStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getBoolean(1)).thenReturn(true);

        DistributedLockService service = new DistributedLockService(dataSource);

        service.tryExecuteWithLock("consistent-lock", () -> {
        });
        service.tryExecuteWithLock("consistent-lock", () -> {
        });

        verify(tryLockStatement, times(2)).setLong(eq(1), anyLong());
    }
}
