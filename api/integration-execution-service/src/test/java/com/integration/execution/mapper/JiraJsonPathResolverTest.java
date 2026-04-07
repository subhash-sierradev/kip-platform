package com.integration.execution.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JiraJsonPathResolverTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private JiraJsonPathResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new JiraJsonPathResolver();
    }

    @Test
    void extractValue_whenPathNullBlankOrNodeNull_returnsNull() throws Exception {
        JsonNode json = objectMapper.readTree("{\"field\":\"value\"}");

        assertThat(resolver.extractValue(null, json)).isNull();
        assertThat(resolver.extractValue("   ", json)).isNull();
        assertThat(resolver.extractValue("field", null)).isNull();
    }

    @Test
    void extractValue_whenPropertyExists_returnsPrimitiveText() throws Exception {
        JsonNode json = objectMapper.readTree("{\"count\":5,\"active\":true,\"field\":\"value\"}");

        assertThat(resolver.extractValue("field", json)).isEqualTo("value");
        assertThat(resolver.extractValue("count", json)).isEqualTo("5");
        assertThat(resolver.extractValue("active", json)).isEqualTo("true");
    }

    @Test
    void extractValue_whenObjectNodeHasPreferredDisplayFields_returnsPreferredValue() throws Exception {
        JsonNode json = objectMapper.readTree("""
                {
                  \"assignee\": {\"displayName\": \"Jane Doe\"},
                  \"project\": {\"key\": \"SCRUM\"},
                  \"priority\": {\"name\": \"High\"},
                  \"raw\": {\"id\": 10}
                }
                """);

        assertThat(resolver.extractValue("assignee", json)).isEqualTo("Jane Doe");
        assertThat(resolver.extractValue("project", json)).isEqualTo("SCRUM");
        assertThat(resolver.extractValue("priority", json)).isEqualTo("High");
        assertThat(resolver.extractValue("raw", json)).isEqualTo("{\"id\":10}");
    }

    @Test
    void extractValue_whenArrayIndexedOrNumericProperty_returnsIndexedValue() throws Exception {
        JsonNode json = objectMapper.readTree("""
                {
                  \"items\": [\"zero\", \"one\", \"two\"],
                  \"wrapper\": {
                    \"values\": [\"a\", \"b\"]
                  }
                }
                """);

        assertThat(resolver.extractValue("items[1]", json)).isEqualTo("one");
        assertThat(resolver.extractValue("wrapper.values[0]", json)).isEqualTo("a");
        assertThat(resolver.extractValue("items.2", json)).isEqualTo("two");
    }

    @Test
    void extractValue_whenWildcardOverArray_joinsResolvedValues() throws Exception {
        JsonNode json = objectMapper.readTree("""
                {
                  \"issues\": [
                    {\"fields\": {\"summary\": \"First\"}},
                    {\"fields\": {\"summary\": \"Second\"}}
                  ]
                }
                """);

        assertThat(resolver.extractValue("issues[].fields.summary", json)).isEqualTo("First, Second");
    }

    @Test
    void extractValue_whenWildcardTargetsEmptyArray_returnsEmptyString() throws Exception {
        JsonNode json = objectMapper.readTree("{\"issues\": []}");

        assertThat(resolver.extractValue("issues[].fields.summary", json)).isEqualTo("");
    }

    @Test
    void extractValue_whenSingleNodeIsArray_flattensNestedValues() throws Exception {
        JsonNode json = objectMapper.readTree("""
                {
                  \"labels\": [\"bug\", \"urgent\", \"backend\"],
                  \"nested\": [[\"one\"], [\"two\", null], []]
                }
                """);

        assertThat(resolver.extractValue("labels", json)).isEqualTo("bug, urgent, backend");
        assertThat(resolver.extractValue("nested", json)).isEqualTo("one, two");
    }

    @Test
    void extractValue_whenQuotedPropertyNamesUsed_resolvesProperty() throws Exception {
        JsonNode json = objectMapper.readTree("{\"custom field\":{\"inner.value\":\"resolved\"}}");

        assertThat(resolver.extractValue("['custom field']['inner.value']", json)).isEqualTo("resolved");
        assertThat(resolver.extractValue("[\"custom field\"][\"inner.value\"]", json)).isEqualTo("resolved");
    }

    @Test
    void extractValue_whenPathMissingOrIndexInvalid_returnsNull() throws Exception {
        JsonNode json = objectMapper.readTree("{\"items\":[\"zero\"]}");

        assertThat(resolver.extractValue("missing", json)).isNull();
        assertThat(resolver.extractValue("items[3]", json)).isNull();
        assertThat(resolver.extractValue("items[-1]", json)).isNull();
        assertThat(resolver.extractValue("[]", json)).isNull();
    }

    @Test
    void extractValue_whenMultipleWildcardResultsAreBlank_returnsEmptyString() throws Exception {
        JsonNode json = objectMapper.readTree("""
                {
                  \"issues\": [
                    {\"fields\": {\"summary\": null}},
                    {\"fields\": {\"summary\": \"\"}}
                  ]
                }
                """);

        assertThat(resolver.extractValue("issues[].fields.summary", json)).isEqualTo("");
    }
}

