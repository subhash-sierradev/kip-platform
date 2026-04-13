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
                  "issues": [
                    {"fields": {"summary": null}},
                    {"fields": {"summary": ""}}
                  ]
                }
                """);

        assertThat(resolver.extractValue("issues[].fields.summary", json)).isEqualTo("");
    }

    // -----------------------------------------------------------------------
    // Additional branch-coverage tests
    // -----------------------------------------------------------------------

    @Test
    void extractValue_whenPropertyNodeIsArrayAndKeyIsNumeric_resolvesIndexedElement() throws Exception {
        // Covers resolvePropertyNode: node.isArray() && key.matches("\\d+") branch
        JsonNode json = objectMapper.readTree("{\"items\": [\"zero\", \"one\", \"two\"]}");

        // Using dot-notation with a numeric key on an array node
        assertThat(resolver.extractValue("items.1", json)).isEqualTo("one");
    }

    @Test
    void extractValue_whenPropertyKeyIsBlank_returnsNull() throws Exception {
        // Covers resolvePropertyNode: key.isBlank() early return
        JsonNode json = objectMapper.readTree("{\"field\": \"value\"}");
        // Path that would result in a blank key: bracket with space-only content
        // "[' ']" parses to a property with key " " which is blank
        assertThat(resolver.extractValue("[' ']", json)).isNull();
    }

    @Test
    void extractValue_whenSingleNodeIsNull_returnsEmptyString() throws Exception {
        // Covers formatSingleNode: node.isNull() branch
        JsonNode json = objectMapper.readTree("{\"field\": null}");
        assertThat(resolver.extractValue("field", json)).isEqualTo("");
    }

    @Test
    void extractValue_whenObjectHasName_returnsName() throws Exception {
        // Covers formatObjectNode: has("name") branch
        JsonNode json = objectMapper.readTree("{\"obj\": {\"name\": \"TestName\", \"displayName\": \"Display\"}}");
        assertThat(resolver.extractValue("obj", json)).isEqualTo("TestName");
    }

    @Test
    void extractValue_whenObjectHasDisplayNameButNotName_returnsDisplayName() throws Exception {
        // Covers formatObjectNode: has("displayName") branch (no "name" key)
        JsonNode json = objectMapper.readTree("{\"obj\": {\"displayName\": \"Display\", \"key\": \"K\"}}");
        assertThat(resolver.extractValue("obj", json)).isEqualTo("Display");
    }

    @Test
    void extractValue_whenObjectHasOnlyKey_returnsKey() throws Exception {
        // Covers formatObjectNode: has("key") branch (no name, no displayName)
        JsonNode json = objectMapper.readTree("{\"obj\": {\"key\": \"K-1\"}}");
        assertThat(resolver.extractValue("obj", json)).isEqualTo("K-1");
    }

    @Test
    void extractValue_whenObjectHasNoneOfPreferredFields_returnsToString() throws Exception {
        // Covers formatObjectNode: fallback to node.toString()
        JsonNode json = objectMapper.readTree("{\"obj\": {\"id\": 42}}");
        assertThat(resolver.extractValue("obj", json)).isEqualTo("{\"id\":42}");
    }

    @Test
    void extractValue_whenWildcardAppliedToNonArray_returnsNull() throws Exception {
        // Covers resolveWildcardNode: !node.isArray() early return
        JsonNode json = objectMapper.readTree("{\"field\": \"notArray\"}");
        assertThat(resolver.extractValue("field[]", json)).isNull();
    }

    @Test
    void extractValue_whenIndexOutOfBounds_returnsNull() throws Exception {
        // Covers resolveIndexedNode: index >= node.size() branch
        JsonNode json = objectMapper.readTree("{\"items\": [\"only\"]}");
        assertThat(resolver.extractValue("items[5]", json)).isNull();
    }

    @Test
    void extractValue_whenNullIndexValue_returnsNull() throws Exception {
        // Covers resolveIndexedNode: node is not array
        JsonNode json = objectMapper.readTree("{\"notArray\": \"value\"}");
        assertThat(resolver.extractValue("notArray[0]", json)).isNull();
    }

    @Test
    void extractValue_whenArrayContainsObjects_formatsEachObjectItem() throws Exception {
        // Covers formatArrayItem: node.isObject() branch within array
        JsonNode json = objectMapper.readTree("""
                {
                  "items": [
                    {"name": "First"},
                    {"displayName": "Second"},
                    {"key": "Third"},
                    {"id": 4}
                  ]
                }
                """);
        assertThat(resolver.extractValue("items", json)).isEqualTo("First, Second, Third, {\"id\":4}");
    }

    @Test
    void extractValue_whenArrayContainsNullElements_skipsNulls() throws Exception {
        // Covers formatArrayItem: node.isNull() returns ""
        JsonNode json = objectMapper.readTree("{\"items\": [null, \"valid\", null]}");
        assertThat(resolver.extractValue("items", json)).isEqualTo("valid");
    }

    @Test
    void extractValue_whenNestedArrayContainsObjects_flattensCorrectly() throws Exception {
        // Covers formatArrayItem: node.isArray() recursive branch with objects
        JsonNode json = objectMapper.readTree("""
                {
                  "nested": [
                    [{"name": "A"}, {"name": "B"}],
                    [{"key": "C"}]
                  ]
                }
                """);
        assertThat(resolver.extractValue("nested", json)).isEqualTo("A, B, C");
    }

    @Test
    void extractValue_whenMultipleResolvedNodesAreAllBlank_returnsEmptyString() throws Exception {
        // Covers formatResolvedNodes: joined is empty after filtering blanks
        JsonNode json = objectMapper.readTree("""
                {
                  "items": [
                    {"fields": {"value": null}},
                    {"fields": {"value": ""}}
                  ]
                }
                """);
        assertThat(resolver.extractValue("items[].fields.value", json)).isEqualTo("");
    }

    @Test
    void extractValue_whenSingleNodeIsArrayOfAllNulls_returnsEmptyString() throws Exception {
        // Covers formatSingleNode: node.isArray() with all null/blank items
        JsonNode json = objectMapper.readTree("{\"items\": [null, null]}");
        assertThat(resolver.extractValue("items", json)).isEqualTo("");
    }

    @Test
    void extractValue_whenStripQuotesReceivedShortString_returnsUnchanged() throws Exception {
        // Covers stripQuotes: token.length() < 2 branch
        JsonNode json = objectMapper.readTree("{\"a\": \"val\"}");
        // Single character bracket content "[a]" => not quoted
        assertThat(resolver.extractValue("[a]", json)).isEqualTo("val");
    }

    @Test
    void extractValue_whenStripQuotesReceivedNonQuotedString_returnsUnchanged() throws Exception {
        // Covers stripQuotes: neither single nor double quoted
        JsonNode json = objectMapper.readTree("{\"ab\": \"val\"}");
        assertThat(resolver.extractValue("[ab]", json)).isEqualTo("val");
    }

    @Test
    void extractValue_whenNodeIsNullNode_skipsIt() throws Exception {
        // Covers resolveNodesByToken: node.isNull() continue branch
        JsonNode json = objectMapper.readTree("{\"field\": null}");
        assertThat(resolver.extractValue("field.subfield", json)).isNull();
    }

    @Test
    void extractValue_whenPropertyNodeOnArrayWithNonNumericKey_returnsNull() throws Exception {
        // Covers resolvePropertyNode: node.isArray() but key is not numeric
        JsonNode json = objectMapper.readTree("{\"items\": [\"a\", \"b\"]}");
        assertThat(resolver.extractValue("items.notANumber", json)).isNull();
    }

    @Test
    void extractValue_whenArrayOfAllBlanksInNestedArray_returnsEmptyString() throws Exception {
        // Covers formatArrayItem: nested values all blank after filtering
        JsonNode json = objectMapper.readTree("{\"items\": [[null], [\"\"]]}");
        assertThat(resolver.extractValue("items", json)).isEqualTo("");
    }
}
