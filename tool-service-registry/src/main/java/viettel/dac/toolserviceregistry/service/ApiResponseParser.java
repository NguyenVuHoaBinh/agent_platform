package viettel.dac.toolserviceregistry.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Service for parsing API responses and extracting values.
 * Enhanced with caching and handling of various response formats.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ApiResponseParser {
    private final ObjectMapper objectMapper;

    /**
     * Extracts a value from an API response using a path expression.
     *
     * @param response The API response text
     * @param path The path to extract (JSONPath or XPath)
     * @param format The format of the response (json, xml)
     * @return The extracted value or null if not found
     */
    @Cacheable(cacheNames = "responseExtraction", key = "#response.hashCode() + '-' + #path + '-' + #format")
    public Object extractValue(String response, String path, String format) {
        log.debug("Extracting value from {} response with path: {}", format, path);

        if (response == null || response.isEmpty() || path == null || path.isEmpty()) {
            return null;
        }

        try {
            if ("json".equalsIgnoreCase(format) || format == null) {
                return extractJsonValue(response, path);
            } else if ("xml".equalsIgnoreCase(format)) {
                return extractXmlValue(response, path);
            } else {
                log.warn("Unsupported response format: {}", format);
                return null;
            }
        } catch (Exception e) {
            log.warn("Error extracting value from response: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Extracts a value from a JSON response using JSONPath.
     *
     * @param response The JSON response text
     * @param path The JSONPath expression
     * @return The extracted value or null if not found
     */
    private Object extractJsonValue(String response, String path) {
        try {
            // Use JsonPath for extraction
            Object result = JsonPath.read(response, path);

            // Convert LinkedHashMap to regular Map for better serialization
            if (result instanceof LinkedHashMap) {
                return new LinkedHashMap<>((LinkedHashMap<?, ?>) result);
            } else if (result instanceof List && !((List<?>) result).isEmpty() && ((List<?>) result).get(0) instanceof LinkedHashMap) {
                List<Object> convertedList = new ArrayList<>();
                for (Object item : (List<?>) result) {
                    if (item instanceof LinkedHashMap) {
                        convertedList.add(new LinkedHashMap<>((LinkedHashMap<?, ?>) item));
                    } else {
                        convertedList.add(item);
                    }
                }
                return convertedList;
            }

            return result;
        } catch (PathNotFoundException e) {
            log.debug("Path not found in JSON: {}", path);
            return null;
        } catch (Exception e) {
            log.warn("Error extracting JSON value: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Extracts a value from an XML response using XPath.
     *
     * @param response The XML response text
     * @param path The XPath expression
     * @return The extracted value or null if not found
     */
    private Object extractXmlValue(String response, String path) {
        // This would use XPath, simplified implementation for now
        log.warn("XML extraction not fully implemented yet");
        return null;
    }

    /**
     * Analyzes a JSON response to suggest extraction paths.
     *
     * @param response The JSON response text
     * @return List of suggested paths with descriptions
     */
    public List<String> suggestJsonPaths(String response) {
        List<String> suggestions = new ArrayList<>();

        try {
            JsonNode rootNode = objectMapper.readTree(response);
            suggestJsonPathsRecursive(rootNode, "$", suggestions, 0);
        } catch (IOException e) {
            log.warn("Error parsing JSON for path suggestions: {}", e.getMessage());
        }

        return suggestions;
    }

    /**
     * Recursively traverses JSON structure to suggest paths.
     *
     * @param node The current JSON node
     * @param currentPath The path to this node
     * @param suggestions List to add suggestions to
     * @param depth Current depth in the JSON tree
     */
    private void suggestJsonPathsRecursive(JsonNode node, String currentPath, List<String> suggestions, int depth) {
        // Limit recursion depth to avoid excessive suggestions
        if (depth > 5) {
            return;
        }

        if (node.isObject()) {
            // Add the current object path
            if (depth > 0) {
                suggestions.add(currentPath + " (Object)");
            }

            // Process object fields
            node.fields().forEachRemaining(entry -> {
                String fieldName = entry.getKey();
                JsonNode fieldValue = entry.getValue();
                String newPath = currentPath + "." + fieldName;

                if (fieldValue.isValueNode()) {
                    suggestions.add(newPath + " (" + getNodeTypeDescription(fieldValue) + ")");
                } else {
                    suggestJsonPathsRecursive(fieldValue, newPath, suggestions, depth + 1);
                }
            });
        } else if (node.isArray()) {
            // Add the current array path
            suggestions.add(currentPath + " (Array)");

            // Add suggestion for first array element if it exists
            if (node.size() > 0) {
                JsonNode firstElement = node.get(0);
                String arrayIndexPath = currentPath + "[0]";

                if (firstElement.isValueNode()) {
                    suggestions.add(arrayIndexPath + " (" + getNodeTypeDescription(firstElement) + ")");
                } else {
                    suggestJsonPathsRecursive(firstElement, arrayIndexPath, suggestions, depth + 1);
                }
            }

            // Add suggestion for all array elements
            suggestions.add(currentPath + "[*] (All array elements)");
        }
    }

    /**
     * Gets a human-readable description of a JSON node type.
     *
     * @param node The JSON node
     * @return Description of the node type
     */
    private String getNodeTypeDescription(JsonNode node) {
        if (node.isTextual()) {
            return "String";
        } else if (node.isNumber()) {
            return node.isInt() ? "Integer" : "Number";
        } else if (node.isBoolean()) {
            return "Boolean";
        } else if (node.isNull()) {
            return "Null";
        } else {
            return "Value";
        }
    }
}