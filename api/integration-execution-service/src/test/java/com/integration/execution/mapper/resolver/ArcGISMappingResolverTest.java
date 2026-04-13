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

    // -----------------------------------------------------------------------
    // Additional branch-coverage tests
    // -----------------------------------------------------------------------

    @Test
    void partitionFeaturesForAddOrUpdate_nonObjectFeaturesInArray_areSkipped() {
        ArrayNode features = objectMapper.createArrayNode();
        features.add("not-an-object");
        features.add(42);
        features.add(featureWithExternalId("loc-1"));

        when(cacheService.getMapping(anyString(), anyString())).thenReturn(null);
        when(arcgisApiClient.queryFeaturesWithWhere(anyString(), anyString()))
                .thenReturn(objectMapper.createObjectNode());

        ApplyEditsPartition result =
                resolver.partitionFeaturesForAddOrUpdate(features, "secret", "https://arcgis/FeatureServer");

        // Only loc-1 is processed; non-objects are skipped in both extractExternalLocationIds and partitionFeatures
        assertThat(result.adds()).hasSize(1);
        assertThat(result.updates()).isEmpty();
    }

    @Test
    void partitionFeaturesForAddOrUpdate_featureWithMissingAttributesNode_goesToAdds() {
        ArrayNode features = objectMapper.createArrayNode();
        // Feature without "attributes" key at all
        ObjectNode featureNoAttrs = objectMapper.createObjectNode();
        featureNoAttrs.put("geometry", "point");
        features.add(featureNoAttrs);

        ApplyEditsPartition result =
                resolver.partitionFeaturesForAddOrUpdate(features, "secret", "https://arcgis/FeatureServer");

        // No external_location_id found → early return → all original features returned as adds
        assertThat(result.adds()).hasSize(1);
        assertThat(result.updates()).isEmpty();
    }

    @Test
    void partitionFeaturesForAddOrUpdate_featureWithNonObjectAttributes_goesToAdds() {
        ArrayNode features = objectMapper.createArrayNode();
        ObjectNode feature = objectMapper.createObjectNode();
        feature.put("attributes", "not-an-object");
        features.add(feature);

        ApplyEditsPartition result =
                resolver.partitionFeaturesForAddOrUpdate(features, "secret", "https://arcgis/FeatureServer");

        // attributes is not an object, extractExternalLocationIds skips it → empty IDs → early return with all features as adds
        assertThat(result.adds()).hasSize(1);
        assertThat(result.updates()).isEmpty();
    }

    @Test
    void partitionFeaturesForAddOrUpdate_featureWithNullExternalId_goesToAdds() {
        ArrayNode features = objectMapper.createArrayNode();
        ObjectNode feature = objectMapper.createObjectNode();
        ObjectNode attrs = objectMapper.createObjectNode();
        attrs.put("name", "test"); // no external_location_id
        feature.set("attributes", attrs);
        features.add(feature);
        // Also add a feature with valid external ID to trigger the mapping flow
        features.add(featureWithExternalId("loc-1"));

        when(cacheService.getMapping(anyString(), anyString())).thenReturn(null);
        when(arcgisApiClient.queryFeaturesWithWhere(anyString(), anyString()))
                .thenReturn(objectMapper.createObjectNode());

        ApplyEditsPartition result =
                resolver.partitionFeaturesForAddOrUpdate(features, "secret", "https://arcgis/FeatureServer");

        assertThat(result.adds()).hasSize(2); // both go to adds since no mapping resolved
        assertThat(result.updates()).isEmpty();
    }

    @Test
    void partitionFeaturesForAddOrUpdate_featureWithBlankExternalId_isSkippedInExtraction() {
        ArrayNode features = objectMapper.createArrayNode();
        ObjectNode feature = objectMapper.createObjectNode();
        ObjectNode attrs = objectMapper.createObjectNode();
        attrs.put("external_location_id", "   "); // blank
        feature.set("attributes", attrs);
        features.add(feature);

        ApplyEditsPartition result =
                resolver.partitionFeaturesForAddOrUpdate(features, "secret", "https://arcgis/FeatureServer");

        // Blank external_location_id is not extracted → no external IDs → all returned as adds
        assertThat(result.adds()).hasSize(1);
        assertThat(result.updates()).isEmpty();
    }

    @Test
    void partitionFeaturesForAddOrUpdate_arcGisApiThrowsException_keepsUnresolvedAsAdds() {
        ArrayNode features = objectMapper.createArrayNode();
        features.add(featureWithExternalId("loc-error"));

        when(cacheService.getMapping(anyString(), anyString())).thenReturn(null);
        when(arcgisApiClient.queryFeaturesWithWhere(anyString(), anyString()))
                .thenThrow(new RuntimeException("ArcGIS connection failed"));

        ApplyEditsPartition result =
                resolver.partitionFeaturesForAddOrUpdate(features, "secret", "https://arcgis/FeatureServer");

        assertThat(result.adds()).hasSize(1);
        assertThat(result.updates()).isEmpty();
    }

    @Test
    void partitionFeaturesForAddOrUpdate_arcGisResponseWithNonArrayFeatures_returnsEmpty() throws Exception {
        ArrayNode features = objectMapper.createArrayNode();
        features.add(featureWithExternalId("loc-1"));

        when(cacheService.getMapping(anyString(), anyString())).thenReturn(null);
        when(arcgisApiClient.queryFeaturesWithWhere(anyString(), anyString()))
                .thenReturn(objectMapper.readTree("{\"features\": \"not-array\"}"));

        ApplyEditsPartition result =
                resolver.partitionFeaturesForAddOrUpdate(features, "secret", "https://arcgis/FeatureServer");

        assertThat(result.adds()).hasSize(1);
        assertThat(result.updates()).isEmpty();
    }

    @Test
    void partitionFeaturesForAddOrUpdate_arcGisFeatureWithNullAttributes_isSkipped() throws Exception {
        ArrayNode features = objectMapper.createArrayNode();
        features.add(featureWithExternalId("loc-1"));

        when(cacheService.getMapping(anyString(), anyString())).thenReturn(null);
        when(arcgisApiClient.queryFeaturesWithWhere(anyString(), anyString()))
                .thenReturn(objectMapper.readTree("""
                        {
                          "features": [
                            {"geometry": {"x": 1, "y": 2}}
                          ]
                        }
                        """));

        ApplyEditsPartition result =
                resolver.partitionFeaturesForAddOrUpdate(features, "secret", "https://arcgis/FeatureServer");

        assertThat(result.adds()).hasSize(1);
        assertThat(result.updates()).isEmpty();
    }

    @Test
    void partitionFeaturesForAddOrUpdate_arcGisFeatureWithZeroObjectId_isSkipped() throws Exception {
        ArrayNode features = objectMapper.createArrayNode();
        features.add(featureWithExternalId("loc-1"));

        when(cacheService.getMapping(anyString(), anyString())).thenReturn(null);
        when(arcgisApiClient.queryFeaturesWithWhere(anyString(), anyString()))
                .thenReturn(objectMapper.readTree("""
                        {
                          "features": [
                            {"attributes": {"OBJECTID": 0, "external_location_id": "loc-1"}}
                          ]
                        }
                        """));

        ApplyEditsPartition result =
                resolver.partitionFeaturesForAddOrUpdate(features, "secret", "https://arcgis/FeatureServer");

        assertThat(result.adds()).hasSize(1);
        assertThat(result.updates()).isEmpty();
    }

    @Test
    void partitionFeaturesForAddOrUpdate_arcGisFeatureWithBlankExternalId_isSkipped() throws Exception {
        ArrayNode features = objectMapper.createArrayNode();
        features.add(featureWithExternalId("loc-1"));

        when(cacheService.getMapping(anyString(), anyString())).thenReturn(null);
        when(arcgisApiClient.queryFeaturesWithWhere(anyString(), anyString()))
                .thenReturn(objectMapper.readTree("""
                        {
                          "features": [
                            {"attributes": {"OBJECTID": 100, "external_location_id": "  "}}
                          ]
                        }
                        """));

        ApplyEditsPartition result =
                resolver.partitionFeaturesForAddOrUpdate(features, "secret", "https://arcgis/FeatureServer");

        assertThat(result.adds()).hasSize(1);
        assertThat(result.updates()).isEmpty();
    }

    @Test
    void partitionFeaturesForAddOrUpdate_nullArcGisResponse_keepsAsAdds() {
        ArrayNode features = objectMapper.createArrayNode();
        features.add(featureWithExternalId("loc-1"));

        when(cacheService.getMapping(anyString(), anyString())).thenReturn(null);
        when(arcgisApiClient.queryFeaturesWithWhere(anyString(), anyString())).thenReturn(null);

        ApplyEditsPartition result =
                resolver.partitionFeaturesForAddOrUpdate(features, "secret", "https://arcgis/FeatureServer");

        assertThat(result.adds()).hasSize(1);
        assertThat(result.updates()).isEmpty();
    }

    @Test
    void partitionFeaturesForAddOrUpdate_emptyFeaturesArray_returnsEmptyPartition() {
        ArrayNode features = objectMapper.createArrayNode();

        ApplyEditsPartition result =
                resolver.partitionFeaturesForAddOrUpdate(features, "secret", "https://arcgis/FeatureServer");

        assertThat(result.adds()).isEmpty();
        assertThat(result.updates()).isEmpty();
    }

    @Test
    void partitionFeaturesForAddOrUpdate_mixedCacheHitsAndMisses_correctlyPartitions() throws Exception {
        ArrayNode features = objectMapper.createArrayNode();
        features.add(featureWithExternalId("cached-1"));
        features.add(featureWithExternalId("uncached-1"));
        features.add(featureWithExternalId("uncached-2"));

        // cached-1 is in cache
        when(cacheService.getMapping(org.mockito.ArgumentMatchers.eq("cached-1"), anyString())).thenReturn(5001L);
        when(cacheService.getMapping(org.mockito.ArgumentMatchers.eq("uncached-1"), anyString())).thenReturn(null);
        when(cacheService.getMapping(org.mockito.ArgumentMatchers.eq("uncached-2"), anyString())).thenReturn(null);

        // ArcGIS resolves uncached-1 only
        when(arcgisApiClient.queryFeaturesWithWhere(anyString(), anyString()))
                .thenReturn(objectMapper.readTree("""
                        {
                          "features": [
                            {"attributes": {"OBJECTID": 5002, "external_location_id": "uncached-1"}}
                          ]
                        }
                        """));

        ApplyEditsPartition result =
                resolver.partitionFeaturesForAddOrUpdate(features, "secret", "https://arcgis/FeatureServer");

        assertThat(result.updates()).hasSize(2); // cached-1 and uncached-1
        assertThat(result.adds()).hasSize(1);    // uncached-2
    }
}


