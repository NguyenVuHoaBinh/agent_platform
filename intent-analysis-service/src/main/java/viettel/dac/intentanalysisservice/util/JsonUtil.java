package viettel.dac.intentanalysisservice.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import viettel.dac.intentanalysisservice.exception.JsonProcessingException;

import java.io.IOException;
import java.util.List;

/**
 * Utility class for JSON operations.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JsonUtil {

    private final ObjectMapper objectMapper;

    /**
     * Convert an object to JSON string.
     *
     * @param object The object to convert
     * @return JSON string representation
     */
    public String toJson(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            log.error("Error converting object to JSON: {}", e.getMessage());
            throw new JsonProcessingException("Failed to convert object to JSON", e);
        }
    }

    /**
     * Convert a JSON string to an object.
     *
     * @param json JSON string
     * @param valueType The class of the object
     * @param <T> Type of the object
     * @return The converted object
     */
    public <T> T fromJson(String json, Class<T> valueType) {
        try {
            return objectMapper.readValue(json, valueType);
        } catch (IOException e) {
            log.error("Error converting JSON to object: {}", e.getMessage());
            throw new JsonProcessingException("Failed to convert JSON to object", e);
        }
    }

    /**
     * Convert a JSON string to a list of objects.
     *
     * @param json JSON string
     * @param elementType The class of the list elements
     * @param <T> Type of the list elements
     * @return List of converted objects
     */
    public <T> List<T> fromJsonList(String json, Class<T> elementType) {
        try {
            return objectMapper.readValue(json, objectMapper.getTypeFactory().constructCollectionType(List.class, elementType));
        } catch (IOException e) {
            log.error("Error converting JSON to list: {}", e.getMessage());
            throw new JsonProcessingException("Failed to convert JSON to list", e);
        }
    }

    /**
     * Extract a substring containing a valid JSON array from a potentially larger string.
     *
     * @param text The text containing JSON
     * @return A valid JSON array substring
     */
    public String extractJsonArray(String text) {
        try {
            int startIndex = text.indexOf('[');
            int endIndex = text.lastIndexOf(']') + 1;

            if (startIndex >= 0 && endIndex > startIndex) {
                return text.substring(startIndex, endIndex);
            }

            throw new JsonProcessingException("No JSON array found in text");
        } catch (Exception e) {
            log.error("Error extracting JSON array: {}", e.getMessage());
            throw new JsonProcessingException("Failed to extract JSON array", e);
        }
    }

    /**
     * Parse a JSON string to a JsonNode.
     *
     * @param json JSON string
     * @return JsonNode representation
     */
    public JsonNode parseJson(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (IOException e) {
            log.error("Error parsing JSON: {}", e.getMessage());
            throw new JsonProcessingException("Failed to parse JSON", e);
        }
    }

    /**
     * Convert a JSON string to a complex type using TypeReference.
     *
     * @param json JSON string
     * @param typeReference TypeReference describing the type
     * @param <T> Type to convert to
     * @return The converted object
     */
    public <T> T fromJson(String json, TypeReference<T> typeReference) {
        try {
            return objectMapper.readValue(json, typeReference);
        } catch (IOException e) {
            log.error("Error converting JSON to complex type: {}", e.getMessage());
            throw new JsonProcessingException("Failed to convert JSON to complex type", e);
        }
    }
}
