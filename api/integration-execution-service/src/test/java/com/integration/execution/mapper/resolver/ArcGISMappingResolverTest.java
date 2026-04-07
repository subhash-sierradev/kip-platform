package com.integration.execution.mapper.resolver;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.integration.execution.client.ArcGISApiClient;
import com.integration.execution.contract.model.ApplyEditsPartition;
import com.integration.execution.service.ArcGISObjectMappingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ArcGISMappingResolverTest {

    @Mock
    private ArcGISApiClient arcgisApiClient;

    @Mock
    private ArcGISObjectMappingService cacheService;

    private ArcGISMappingResolver resolver;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        resolver = new ArcGISMappingResolver(arcgisApiClient, objectMapper, cacheService);
    }

    @Test
    void partitionFeaturesForAddOrUpdate_noExternalIds_returnsAllAsAdds() {
        ArrayNode features = objectMapper.createArrayNode();
        ObjectNode feature = objectMapper.createObjectNode();
        feature.set("attributes", objectMapper.createObjectNode().put("name", "A"));
        features.add(feature);

        ApplyEditsPartition result =
                resolver.partitionFeaturesForAddOrUpdate(features, "secret", "https://arcgis/FeatureServer");

        assertThat(result.adds()).hasSize(1);
        assertThat(result.updates()).isEmpty();
        verify(cacheService, never()).getMapping(anyString(), anyString());
        verify(arcgisApiClient, never()).queryFeaturesWithWhere(anyString(), anyString());
    }

    @Test
    void partitionFeaturesForAddOrUpdate_cacheHit_partitionsIntoUpdates() {
        ArrayNode features = objectMapper.createArrayNode();
        features.add(featureWithExternalId("loc-1"));

        when(cacheService.getMapping(anyString(), anyString())).thenReturn(1001L);

        ApplyEditsPartition result =
                resolver.partitionFeaturesForAddOrUpdate(features, "secret", "https://arcgis/FeatureServer");

        assertThat(result.adds()).isEmpty();
        assertThat(result.updates()).hasSize(1);
        assertThat(result.updates().get(0).path("attributes").path("OBJECTID").asLong()).isEqualTo(1001L);
        verify(arcgisApiClient, never()).queryFeaturesWithWhere(anyString(), anyString());
    }

    @Test
    void partitionFeaturesForAddOrUpdate_cacheMiss_fetchesFromArcGisAndCachesMappings() throws Exception {
        ArrayNode features = objectMapper.createArrayNode();
        features.add(featureWithExternalId("loc-1"));
        features.add(featureWithExternalId("loc-2"));

        when(cacheService.getMapping(anyString(), anyString())).thenReturn(null);
        when(arcgisApiClient.queryFeaturesWithWhere(anyString(), anyString()))
                .thenReturn(objectMapper.readTree("""
                        {
                          "features": [
                            {"attributes": {"OBJECTID": 2001, "external_location_id": "loc-1"}}
                          ]
                        }
                        """));

        ApplyEditsPartition result =
                resolver.partitionFeaturesForAddOrUpdate(features, "secret", "https://arcgis/FeatureServer");

        assertThat(result.updates()).hasSize(1);
        assertThat(result.adds()).hasSize(1);
        assertThat(result.updates().get(0).path("attributes").path("OBJECTID").asLong()).isEqualTo(2001L);
        verify(cacheService).putMapping(anyString(), anyString(), org.mockito.ArgumentMatchers.eq(2001L));
    }

    @Test
    void partitionFeaturesForAddOrUpdate_arcGisReturnsInvalidPayload_keepsAsAdds() throws Exception {
        ArrayNode features = objectMapper.createArrayNode();
        features.add(featureWithExternalId("loc-1"));

        when(cacheService.getMapping(anyString(), anyString())).thenReturn(null);
        when(arcgisApiClient.queryFeaturesWithWhere(anyString(), anyString()))
                .thenReturn(objectMapper.readTree("{}"));

        ApplyEditsPartition result =
                resolver.partitionFeaturesForAddOrUpdate(features, "secret", "https://arcgis/FeatureServer");

        assertThat(result.adds()).hasSize(1);
        assertThat(result.updates()).isEmpty();
    }

    private ObjectNode featureWithExternalId(String externalId) {
        ObjectNode attributes = objectMapper.createObjectNode();
        attributes.put("external_location_id", externalId);
        ObjectNode feature = objectMapper.createObjectNode();
        feature.set("attributes", attributes);
        return feature;
    }
}
