package com.integration.execution.arcgisverification;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.integration.execution.config.properties.ArcGISVerificationProperties;
import com.integration.execution.contract.rest.response.arcgis.ArcGISVerificationPageResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.CoreMatchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@DisplayName("ArcGISVerificationService")
class ArcGISVerificationServiceTest {

    private static final String TOKEN_URL = "https://arcgis.test/oauth2/token";
    private static final String FEATURE_URL = "https://arcgis.test/FeatureServer";
    private static final String TOKEN_RESPONSE =
            "{\"access_token\":\"test-token\",\"expires_in\":7200}";

    private RestTemplate restTemplate;
    private MockRestServiceServer mockServer;
    private ArcGISVerificationProperties properties;
    private ArcGISVerificationService service;

    @BeforeEach
    void setUp() {
        restTemplate = new RestTemplate();
        mockServer = MockRestServiceServer.createServer(restTemplate);
        properties = new ArcGISVerificationProperties();
        properties.setFeatureServiceUrl(FEATURE_URL);
        properties.setClientId("client-id");
        properties.setClientSecret("client-secret");
        properties.setTokenUrl(TOKEN_URL);
        service = new ArcGISVerificationService(properties, restTemplate, new ObjectMapper());
    }

    @Nested
    @DisplayName("buildWhereClause")
    class BuildWhereClause {

        @Test
        @DisplayName("returns 1=1 when both filters are null")
        void buildWhereClause_noFilters_returnsAll() {
            assertThat(service.buildWhereClause(null, null)).isEqualTo("1=1");
        }

        @Test
        @DisplayName("returns 1=1 when both filters are blank")
        void buildWhereClause_blankFilters_returnsAll() {
            assertThat(service.buildWhereClause("   ", "  ")).isEqualTo("1=1");
        }

        @Test
        @DisplayName("filters by OBJECTID when objectId is provided")
        void buildWhereClause_objectId_buildsObjectIdFilter() {
            assertThat(service.buildWhereClause("42", null))
                    .isEqualTo("OBJECTID = 42");
        }

        @Test
        @DisplayName("throws NumberFormatException for non-numeric objectId")
        void buildWhereClause_nonNumericObjectId_throwsNumberFormatException() {
            assertThatThrownBy(() -> service.buildWhereClause("abc", null))
                    .isInstanceOf(NumberFormatException.class);
        }

        @Test
        @DisplayName("filters by external_location_id when locationId is provided")
        void buildWhereClause_locationId_buildsLocationFilter() {
            assertThat(service.buildWhereClause(null, "LOC-001"))
                    .isEqualTo("external_location_id = 'LOC-001'");
        }

        @Test
        @DisplayName("escapes single quotes in locationId")
        void buildWhereClause_locationIdWithSingleQuote_escapesIt() {
            assertThat(service.buildWhereClause(null, "O'Brien"))
                    .isEqualTo("external_location_id = 'O''Brien'");
        }

        @Test
        @DisplayName("objectId takes priority over locationId")
        void buildWhereClause_bothProvided_objectIdWins() {
            assertThat(service.buildWhereClause("7", "LOC-001"))
                    .isEqualTo("OBJECTID = 7");
        }
    }

    @Nested
    @DisplayName("getAccessToken")
    class GetAccessToken {

        @Test
        @DisplayName("fetches and caches token on first call")
        void getAccessToken_firstCall_fetchesFromTokenUrl() {
            mockServer.expect(method(HttpMethod.POST))
                    .andRespond(withSuccess(TOKEN_RESPONSE, MediaType.APPLICATION_JSON));

            String token = service.getAccessToken();

            assertThat(token).isEqualTo("test-token");
            mockServer.verify();
        }

        @Test
        @DisplayName("returns cached token on second call without HTTP request")
        void getAccessToken_secondCall_usesCachedToken() {
            mockServer.expect(method(HttpMethod.POST))
                    .andRespond(withSuccess(TOKEN_RESPONSE, MediaType.APPLICATION_JSON));

            service.getAccessToken(); // first call — fetches
            String cachedResult = service.getAccessToken(); // second call — cached

            assertThat(cachedResult).isEqualTo("test-token");
            mockServer.verify(); // only one HTTP call was made
        }

        @Test
        @DisplayName("throws IllegalStateException when token endpoint returns error")
        void getAccessToken_arcGisError_throwsIllegalState() {
            String errorResponse = "{\"error\":{\"code\":498,\"message\":\"invalid client\"}}";
            mockServer.expect(method(HttpMethod.POST))
                    .andRespond(withSuccess(errorResponse, MediaType.APPLICATION_JSON));

            assertThatThrownBy(() -> service.getAccessToken())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("ArcGIS token endpoint returned error");
        }
    }

    @Nested
    @DisplayName("queryFeatures")
    class QueryFeatures {

        @Test
        @DisplayName("returns parsed features for a successful response")
        void queryFeatures_validResponse_returnsFeatures() {
            String featureResponse = """
                    {
                      "features": [
                        {"attributes": {"OBJECTID": 1, "external_location_id": "LOC-001"}},
                        {"attributes": {"OBJECTID": 2, "external_location_id": "LOC-002"}}
                      ],
                      "exceededTransferLimit": false
                    }
                    """;
            mockServer.expect(method(HttpMethod.POST))
                    .andRespond(withSuccess(TOKEN_RESPONSE, MediaType.APPLICATION_JSON));
            mockServer.expect(method(HttpMethod.GET))
                    .andRespond(withSuccess(featureResponse, MediaType.APPLICATION_JSON));

            ArcGISVerificationPageResponse response = service.queryFeatures(0, null, null);

            assertThat(response.getFetchedCount()).isEqualTo(2);
            assertThat(response.getOffset()).isZero();
            assertThat(response.isExceededTransferLimit()).isFalse();
            assertThat(response.getFeatures()).hasSize(2);
            assertThat(response.getFeatures().get(0)).containsEntry("OBJECTID", 1L);
            assertThat(response.getFeatures().get(0))
                    .containsEntry("external_location_id", "LOC-001");
        }

        @Test
        @DisplayName("sets exceededTransferLimit=true when ArcGIS returns it")
        void queryFeatures_exceededTransferLimit_setsFlag() {
            String featureResponse = "{\"features\":[],\"exceededTransferLimit\":true}";
            mockServer.expect(method(HttpMethod.POST))
                    .andRespond(withSuccess(TOKEN_RESPONSE, MediaType.APPLICATION_JSON));
            mockServer.expect(method(HttpMethod.GET))
                    .andRespond(withSuccess(featureResponse, MediaType.APPLICATION_JSON));

            ArcGISVerificationPageResponse response = service.queryFeatures(1000, null, null);

            assertThat(response.isExceededTransferLimit()).isTrue();
            assertThat(response.getOffset()).isEqualTo(1000);
        }

        @Test
        @DisplayName("throws IllegalStateException when ArcGIS returns error node")
        void queryFeatures_arcGisQueryError_throwsIllegalState() {
            String errorResponse = "{\"error\":{\"code\":400,\"message\":\"invalid token\"}}";
            mockServer.expect(method(HttpMethod.POST))
                    .andRespond(withSuccess(TOKEN_RESPONSE, MediaType.APPLICATION_JSON));
            mockServer.expect(method(HttpMethod.GET))
                    .andRespond(withSuccess(errorResponse, MediaType.APPLICATION_JSON));

            assertThatThrownBy(() -> service.queryFeatures(0, null, null))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("ArcGIS query returned error");
        }

        @Test
        @DisplayName("uses OBJECTID DESC ordering so newest records appear first")
        void queryFeatures_noFilter_usesDescendingObjectIdOrder() {
            String featureResponse = "{\"features\":[],\"exceededTransferLimit\":false}";
            mockServer.expect(method(HttpMethod.POST))
                    .andRespond(withSuccess(TOKEN_RESPONSE, MediaType.APPLICATION_JSON));
            mockServer.expect(requestTo(containsString("OBJECTID%20DESC")))
                    .andExpect(method(HttpMethod.GET))
                    .andRespond(withSuccess(featureResponse, MediaType.APPLICATION_JSON));

            service.queryFeatures(0, null, null);

            mockServer.verify();
        }

        @Test
        @DisplayName("returns empty features list when features array is absent")
        void queryFeatures_missingFeaturesArray_returnsEmpty() {
            mockServer.expect(method(HttpMethod.POST))
                    .andRespond(withSuccess(TOKEN_RESPONSE, MediaType.APPLICATION_JSON));
            mockServer.expect(method(HttpMethod.GET))
                    .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

            ArcGISVerificationPageResponse response = service.queryFeatures(0, null, null);

            assertThat(response.getFetchedCount()).isZero();
            assertThat(response.getFeatures()).isEmpty();
        }
    }

    @Test
    @DisplayName("PAGE_SIZE constant is 1000")
    void pageSizeConstant_isOneThousand() {
        assertThat(ArcGISVerificationService.PAGE_SIZE).isEqualTo(1000);
    }
}
