package com.integration.execution.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
public class JiraJsonPathResolver {

    private static final Pattern PATH_TOKEN_PATTERN = Pattern.compile("([^.\\[\\]]+)|\\[(.*?)\\]");
    private static final String ARRAY_JOIN_DELIMITER = ", ";

    public String extractValue(String jsonPath, JsonNode jsonNode) {
        if (jsonPath == null || jsonPath.trim().isEmpty() || jsonNode == null) {
            return null;
        }

        List<PathToken> pathTokens = parsePathTokens(jsonPath);
        if (pathTokens.isEmpty()) {
            return null;
        }

        List<JsonNode> currentNodes = List.of(jsonNode);
        for (int i = 0; i < pathTokens.size(); i++) {
            PathToken token = pathTokens.get(i);
            boolean wildcardFromArray = token.type == PathTokenType.WILDCARD
                    && currentNodes.stream().anyMatch(JsonNode::isArray);
            currentNodes = resolveNodesByToken(currentNodes, token);
            if (currentNodes.isEmpty()) {
                if (wildcardFromArray) {
                    return "";
                }
                return null;
            }
        }

        return formatResolvedNodes(currentNodes);
    }

    private List<PathToken> parsePathTokens(String jsonPath) {
        List<PathToken> tokens = new ArrayList<>();
        Matcher tokenMatcher = PATH_TOKEN_PATTERN.matcher(jsonPath.trim());

        while (tokenMatcher.find()) {
            String property = tokenMatcher.group(1);
            if (property != null && !property.trim().isEmpty()) {
                tokens.add(PathToken.property(property.trim()));
                continue;
            }

            String bracketToken = tokenMatcher.group(2) == null ? "" : tokenMatcher.group(2).trim();
            if (bracketToken.isEmpty()) {
                tokens.add(PathToken.wildcard());
                continue;
            }

            if (bracketToken.matches("\\d+")) {
                tokens.add(PathToken.index(Integer.parseInt(bracketToken)));
                continue;
            }

            tokens.add(PathToken.property(stripQuotes(bracketToken)));
        }

        return tokens;
    }

    private String stripQuotes(String token) {
        if (token == null || token.length() < 2) {
            return token;
        }

        boolean singleQuoted = token.startsWith("'") && token.endsWith("'");
        boolean doubleQuoted = token.startsWith("\"") && token.endsWith("\"");
        if (singleQuoted || doubleQuoted) {
            return token.substring(1, token.length() - 1);
        }

        return token;
    }

    private List<JsonNode> resolveNodesByToken(List<JsonNode> currentNodes, PathToken token) {
        List<JsonNode> nextNodes = new ArrayList<>();

        for (JsonNode node : currentNodes) {
            if (node == null || node.isNull()) {
                continue;
            }

            switch (token.type) {
                case WILDCARD:
                    resolveWildcardNode(node, nextNodes);
                    break;
                case INDEX:
                    resolveIndexedNode(node, token.index, nextNodes);
                    break;
                case PROPERTY:
                    resolvePropertyNode(node, token.key, nextNodes);
                    break;
                default:
                    break;
            }
        }

        return nextNodes;
    }

    private void resolveWildcardNode(JsonNode node, List<JsonNode> target) {
        if (!node.isArray()) {
            return;
        }

        for (JsonNode element : node) {
            target.add(element);
        }
    }

    private void resolveIndexedNode(JsonNode node, Integer index, List<JsonNode> target) {
        if (!node.isArray() || index == null || index < 0 || index >= node.size()) {
            return;
        }
        target.add(node.get(index));
    }

    private void resolvePropertyNode(JsonNode node, String key, List<JsonNode> target) {
        if (key == null || key.isBlank()) {
            return;
        }

        if (node.isObject()) {
            if (node.has(key)) {
                target.add(node.get(key));
            }
            return;
        }

        if (node.isArray() && key.matches("\\d+")) {
            int index = Integer.parseInt(key);
            resolveIndexedNode(node, index, target);
        }
    }

    private String formatResolvedNodes(List<JsonNode> nodes) {
        if (nodes.isEmpty()) {
            return null;
        }

        if (nodes.size() == 1) {
            return formatSingleNode(nodes.getFirst());
        }

        String joined = nodes.stream()
                .map(this::formatArrayItem)
                .filter(item -> item != null && !item.isBlank())
                .collect(Collectors.joining(ARRAY_JOIN_DELIMITER));

        return joined.isEmpty() ? "" : joined;
    }

    private String formatSingleNode(JsonNode node) {
        if (node == null || node.isNull()) {
            return "";
        }

        if (node.isArray()) {
            List<String> arrayValues = new ArrayList<>();
            for (JsonNode item : node) {
                String formatted = formatArrayItem(item);
                if (formatted != null && !formatted.isBlank()) {
                    arrayValues.add(formatted);
                }
            }
            String joined = String.join(ARRAY_JOIN_DELIMITER, arrayValues);
            return joined.isEmpty() ? "" : joined;
        }

        if (node.isTextual() || node.isNumber() || node.isBoolean()) {
            return node.asText();
        }

        if (node.isObject()) {
            return formatObjectNode(node);
        }

        return node.asText();
    }

    private String formatArrayItem(JsonNode node) {
        if (node == null || node.isNull()) {
            return "";
        }

        if (node.isArray()) {
            List<String> nestedValues = new ArrayList<>();
            node.forEach(item -> {
                String formatted = formatArrayItem(item);
                if (formatted != null && !formatted.isBlank()) {
                    nestedValues.add(formatted);
                }
            });
            return String.join(ARRAY_JOIN_DELIMITER, nestedValues);
        }

        if (node.isObject()) {
            return formatObjectNode(node);
        }

        return node.asText();
    }

    private String formatObjectNode(JsonNode node) {
        if (node.has("name")) {
            return node.get("name").asText();
        }
        if (node.has("displayName")) {
            return node.get("displayName").asText();
        }
        if (node.has("key")) {
            return node.get("key").asText();
        }
        return node.toString();
    }

    private static final class PathToken {
        private final PathTokenType type;
        private final String key;
        private final Integer index;

        private PathToken(PathTokenType type, String key, Integer index) {
            this.type = type;
            this.key = key;
            this.index = index;
        }

        private static PathToken property(String key) {
            return new PathToken(PathTokenType.PROPERTY, key, null);
        }

        private static PathToken index(Integer index) {
            return new PathToken(PathTokenType.INDEX, null, index);
        }

        private static PathToken wildcard() {
            return new PathToken(PathTokenType.WILDCARD, null, null);
        }
    }

    private enum PathTokenType {
        PROPERTY,
        INDEX,
        WILDCARD
    }
}