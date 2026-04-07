package com.integration.management.mapper;

import com.integration.management.entity.AuditLog;
import com.integration.management.model.dto.response.AuditLogResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AuditLogMapper")
class AuditLogMapperTest {

    private final AuditLogMapper mapper = new AuditLogMapper() {
        @Override
        public AuditLogResponse toResponse(AuditLog auditLog) {
            return null;
        }
    };

    @Test
    @DisplayName("mapMetadataToString returns null for null/empty")
    void mapMetadataToString_nullOrEmpty_returnsNull() {
        assertThat(mapper.mapMetadataToString(null)).isNull();
        assertThat(mapper.mapMetadataToString(Map.of())).isNull();
    }

    @Test
    @DisplayName("mapMetadataToString joins entries")
    void mapMetadataToString_joinsEntries() {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("a", 1);
        metadata.put("b", "x");

        assertThat(mapper.mapMetadataToString(metadata)).isEqualTo("a=1, b=x");
    }

    @Test
    @DisplayName("toResponse mapping uses metadata directly")
    void toResponse_metadataMappedToDetails() {
        AuditLogMapperImpl impl = new AuditLogMapperImpl();

        UUID id = UUID.fromString("00000000-0000-0000-0000-000000000001");
        AuditLog auditLog = AuditLog.builder()
                .id(id)
                .clientIpAddress("198.51.100.14")
                .timestamp(Instant.parse("2026-02-17T00:00:00Z"))
                .metadata(Map.of("k", "v"))
                .build();

        AuditLogResponse response = impl.toResponse(auditLog);
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(id);
        assertThat(response.getClientIpAddress()).isEqualTo("198.51.100.14");
        assertThat(response.getTimestamp()).isEqualTo(Instant.parse("2026-02-17T00:00:00Z"));
        assertThat(response.getDetails()).isEqualTo("k=v");
    }
}
