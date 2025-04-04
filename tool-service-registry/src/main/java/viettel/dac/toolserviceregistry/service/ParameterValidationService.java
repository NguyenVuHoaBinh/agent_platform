// File: src/main/java/viettel/dac/toolserviceregistry/service/ParameterValidationService.java
package viettel.dac.toolserviceregistry.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import viettel.dac.toolserviceregistry.model.dto.ToolParameterDTO;
import viettel.dac.toolserviceregistry.model.dto.ValidationResult;
import viettel.dac.toolserviceregistry.model.enums.ParameterType;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Service for parameter validation and suggestions.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ParameterValidationService {
    private final ObjectMapper objectMapper;

    /**
     * Validates a parameter value against its constraints.
     *
     * @param parameter The parameter definition
     * @param value The value to validate
     * @return Validation result
     */
    public ValidationResult validateParameterValue(ToolParameterDTO parameter, Object value) {
        log.debug("Validating parameter {} with value {}", parameter.getName(), value);

        ValidationResult result = new ValidationResult();
        result.setValid(true);

        // Check if required parameter is missing
        if (parameter.isRequired() && (value == null || (value instanceof String && ((String) value).isEmpty()))) {
            result.setValid(false);
            result.setMessage("Required parameter is missing");
            return result;
        }

        // If value is null and parameter is not required, it's valid
        if (value == null) {
            return result;
        }

        try {
            // Type validation
            switch (parameter.getParameterType()) {
                case STRING:
                    validateString(parameter, value, result);
                    break;
                case NUMBER:
                    validateNumber(parameter, value, result);
                    break;
                case BOOLEAN:
                    validateBoolean(parameter, value, result);
                    break;
                case DATE:
                case DATETIME:
                    validateDate(parameter, value, result);
                    break;
                case EMAIL:
                    validateEmail(parameter, value, result);
                    break;
                case URL:
                    validateUrl(parameter, value, result);
                    break;
                case ENUM:
                    validateEnum(parameter, value, result);
                    break;
                case ARRAY:
                    validateArray(parameter, value, result);
                    break;
                case OBJECT:
                    validateObject(parameter, value, result);
                    break;
                case JSON:
                    validateJson(parameter, value, result);
                    break;
                case XML:
                    validateXml(parameter, value, result);
                    break;
                case FILE:
                    validateFile(parameter, value, result);
                    break;
                case SECRET:
                    validateSecret(parameter, value, result);
                    break;
                default:
                    // For unknown types, just check custom validation pattern
                    validateWithCustomPattern(parameter, value, result);
            }
        } catch (Exception e) {
            result.setValid(false);
            result.setMessage("Validation error: " + e.getMessage());
            log.warn("Parameter validation error", e);
        }

        return result;
    }

    /**
     * Generates parameter suggestions based on the parameter definition.
     *
     * @param parameter The parameter definition
     * @param prefix Optional prefix to filter suggestions
     * @return List of suggestions
     */
    public List<String> suggestParameterValues(ToolParameterDTO parameter, String prefix) {
        log.debug("Generating suggestions for parameter {}", parameter.getName());

        // Start with allowed values if available
        List<String> suggestions = new ArrayList<>();
        if (parameter.getAllowedValues() != null && !parameter.getAllowedValues().isEmpty()) {
            suggestions.addAll(parameter.getAllowedValues());
        }

        // Add examples if available
        if (parameter.getExamples() != null && !parameter.getExamples().isEmpty()) {
            try {
                String[] examples = parameter.getExamples().split(",");
                Collections.addAll(suggestions, examples);
            } catch (Exception e) {
                log.warn("Error parsing parameter examples", e);
            }
        }

        // If default value is available and not already in suggestions, add it
        if (parameter.getDefaultValue() != null && !parameter.getDefaultValue().isEmpty()
                && !suggestions.contains(parameter.getDefaultValue())) {
            suggestions.add(parameter.getDefaultValue());
        }

        // Filter suggestions by prefix if provided
        if (prefix != null && !prefix.isEmpty()) {
            suggestions = suggestions.stream()
                    .filter(s -> s.toLowerCase().startsWith(prefix.toLowerCase()))
                    .toList();
        }

        // For numeric parameters, generate range suggestions
        if (parameter.getParameterType() == ParameterType.NUMBER &&
                parameter.getMinValue() != null && parameter.getMaxValue() != null) {
            try {
                double min = Double.parseDouble(parameter.getMinValue());
                double max = Double.parseDouble(parameter.getMaxValue());

                // Generate 5 equally spaced values in the range
                if (suggestions.isEmpty() && max > min) {
                    double step = (max - min) / 4;
                    for (int i = 0; i <= 4; i++) {
                        double value = min + (step * i);
                        if (value == (long) value) {
                            suggestions.add(String.valueOf((long) value));
                        } else {
                            suggestions.add(String.valueOf(value));
                        }
                    }
                }
            } catch (NumberFormatException e) {
                log.warn("Error generating numeric suggestions", e);
            }
        }

        // Generate suggestions for boolean parameters
        if (parameter.getParameterType() == ParameterType.BOOLEAN && suggestions.isEmpty()) {
            suggestions.add("true");
            suggestions.add("false");
        }

        // TODO: Add custom suggestion generation logic based on suggestionQuery

        return suggestions;
    }

    /**
     * Validates a map of parameter values against their definitions.
     *
     * @param parameters Map of parameter definitions
     * @param values Map of parameter values
     * @return Map of validation results by parameter name
     */
    public Map<String, ValidationResult> validateParameters(
            Map<String, ToolParameterDTO> parameters,
            Map<String, Object> values) {

        Map<String, ValidationResult> results = new HashMap<>();

        for (Map.Entry<String, ToolParameterDTO> entry : parameters.entrySet()) {
            String paramName = entry.getKey();
            ToolParameterDTO parameter = entry.getValue();
            Object value = values.get(paramName);

            results.put(paramName, validateParameterValue(parameter, value));
        }

        return results;
    }

    // Private validation methods for different parameter types

    private void validateString(ToolParameterDTO parameter, Object value, ValidationResult result) {
        if (!(value instanceof String)) {
            result.setValid(false);
            result.setMessage("Value must be a string");
            return;
        }

        String stringValue = (String) value;

        // Check min length
        if (parameter.getMinLength() != null && stringValue.length() < parameter.getMinLength()) {
            result.setValid(false);
            result.setMessage("Value must be at least " + parameter.getMinLength() + " characters long");
            return;
        }

        // Check max length
        if (parameter.getMaxLength() != null && stringValue.length() > parameter.getMaxLength()) {
            result.setValid(false);
            result.setMessage("Value must be at most " + parameter.getMaxLength() + " characters long");
            return;
        }

        // Check allowed values
        if (parameter.getAllowedValues() != null && !parameter.getAllowedValues().isEmpty()
                && !parameter.getAllowedValues().contains(stringValue)) {
            result.setValid(false);
            result.setMessage("Value must be one of: " + String.join(", ", parameter.getAllowedValues()));
            return;
        }

        // Check custom validation pattern
        validateWithCustomPattern(parameter, value, result);
    }

    private void validateNumber(ToolParameterDTO parameter, Object value, ValidationResult result) {
        double numValue;
        if (value instanceof Number) {
            numValue = ((Number) value).doubleValue();
        } else if (value instanceof String) {
            try {
                numValue = Double.parseDouble((String) value);
            } catch (NumberFormatException e) {
                result.setValid(false);
                result.setMessage("Value must be a valid number");
                return;
            }
        } else {
            result.setValid(false);
            result.setMessage("Value must be a number");
            return;
        }

        // Check min value
        if (parameter.getMinValue() != null && !parameter.getMinValue().isEmpty()) {
            try {
                double min = Double.parseDouble(parameter.getMinValue());
                if (numValue < min) {
                    result.setValid(false);
                    result.setMessage("Value must be at least " + min);
                    return;
                }
            } catch (NumberFormatException e) {
                log.warn("Invalid min value in parameter definition", e);
            }
        }

        // Check max value
        if (parameter.getMaxValue() != null && !parameter.getMaxValue().isEmpty()) {
            try {
                double max = Double.parseDouble(parameter.getMaxValue());
                if (numValue > max) {
                    result.setValid(false);
                    result.setMessage("Value must be at most " + max);
                    return;
                }
            } catch (NumberFormatException e) {
                log.warn("Invalid max value in parameter definition", e);
            }
        }
    }

    private void validateBoolean(ToolParameterDTO parameter, Object value, ValidationResult result) {
        if (value instanceof Boolean) {
            return; // Valid boolean
        }

        if (value instanceof String) {
            String strValue = ((String) value).toLowerCase();
            if (strValue.equals("true") || strValue.equals("false")) {
                return; // Valid boolean string
            }
        }

        result.setValid(false);
        result.setMessage("Value must be a boolean (true or false)");
    }

    private void validateDate(ToolParameterDTO parameter, Object value, ValidationResult result) {
        if (!(value instanceof String)) {
            result.setValid(false);
            result.setMessage("Date value must be a string");
            return;
        }

        String strValue = (String) value;

        // Define regex patterns for date and datetime
        String datePattern = "^\\d{4}-(0[1-9]|1[0-2])-(0[1-9]|[12][0-9]|3[01])$";
        String datetimePattern = "^\\d{4}-(0[1-9]|1[0-2])-(0[1-9]|[12][0-9]|3[01])[T ](0[0-9]|1[0-9]|2[0-3]):[0-5][0-9]:[0-5][0-9](\\.\\d+)?(Z|[+-]\\d{2}:?\\d{2})?$";

        boolean isValid;
        if (parameter.getParameterType() == ParameterType.DATE) {
            isValid = Pattern.matches(datePattern, strValue);
        } else { // DATETIME
            isValid = Pattern.matches(datetimePattern, strValue);
        }

        if (!isValid) {
            result.setValid(false);
            result.setMessage("Invalid date format. Expected format: " +
                    (parameter.getParameterType() == ParameterType.DATE ? "YYYY-MM-DD" : "YYYY-MM-DDTHH:MM:SS"));
        }
    }

    private void validateEmail(ToolParameterDTO parameter, Object value, ValidationResult result) {
        if (!(value instanceof String)) {
            result.setValid(false);
            result.setMessage("Email must be a string");
            return;
        }

        String emailPattern = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";
        if (!Pattern.matches(emailPattern, (String) value)) {
            result.setValid(false);
            result.setMessage("Invalid email format");
        }
    }

    private void validateUrl(ToolParameterDTO parameter, Object value, ValidationResult result) {
        if (!(value instanceof String)) {
            result.setValid(false);
            result.setMessage("URL must be a string");
            return;
        }

        String urlPattern = "^(https?|ftp)://[^\\s/$.?#].[^\\s]*$";
        if (!Pattern.matches(urlPattern, (String) value)) {
            result.setValid(false);
            result.setMessage("Invalid URL format");
        }
    }

    private void validateEnum(ToolParameterDTO parameter, Object value, ValidationResult result) {
        if (parameter.getAllowedValues() == null || parameter.getAllowedValues().isEmpty()) {
            result.setValid(false);
            result.setMessage("Enum parameter has no allowed values defined");
            return;
        }

        String strValue = value.toString();
        if (!parameter.getAllowedValues().contains(strValue)) {
            result.setValid(false);
            result.setMessage("Value must be one of: " + String.join(", ", parameter.getAllowedValues()));
        }
    }

    private void validateArray(ToolParameterDTO parameter, Object value, ValidationResult result) {
        if (value instanceof List || value instanceof Object[]) {
            // Valid array type
            return;
        }

        if (value instanceof String) {
            try {
                List<?> list = objectMapper.readValue((String) value, List.class);
                // Valid array JSON
                return;
            } catch (Exception e) {
                // Not valid JSON array, check CSV format
                String[] parts = ((String) value).split(",");
                if (parts.length > 0) {
                    // Valid CSV format
                    return;
                }
            }
        }

        result.setValid(false);
        result.setMessage("Value must be an array");
    }

    private void validateObject(ToolParameterDTO parameter, Object value, ValidationResult result) {
        if (value instanceof Map) {
            // Valid object type
            return;
        }

        if (value instanceof String) {
            try {
                // Try to parse as JSON object
                Object parsed = objectMapper.readValue((String) value, Object.class);
                if (parsed instanceof Map) {
                    // Valid JSON object
                    if (parameter.getObjectSchema() != null && !parameter.getObjectSchema().isEmpty()) {
                        // TODO: Implement JSON schema validation
                    }
                    return;
                }
            } catch (Exception e) {
                // Not valid JSON
            }
        }

        result.setValid(false);
        result.setMessage("Value must be a valid object");
    }

    private void validateJson(ToolParameterDTO parameter, Object value, ValidationResult result) {
        if (!(value instanceof String)) {
            result.setValid(false);
            result.setMessage("JSON must be a string");
            return;
        }

        try {
            objectMapper.readValue((String) value, Object.class);
            // Valid JSON
        } catch (Exception e) {
            result.setValid(false);
            result.setMessage("Invalid JSON format: " + e.getMessage());
        }
    }

    private void validateXml(ToolParameterDTO parameter, Object value, ValidationResult result) {
        if (!(value instanceof String)) {
            result.setValid(false);
            result.setMessage("XML must be a string");
            return;
        }

        String xmlString = (String) value;
        if (!xmlString.trim().startsWith("<") || !xmlString.trim().endsWith(">")) {
            result.setValid(false);
            result.setMessage("Invalid XML format");
            return;
        }

        // TODO: Add more comprehensive XML validation if needed
    }

    private void validateFile(ToolParameterDTO parameter, Object value, ValidationResult result) {
        // Basic file validation - more comprehensive validation would depend on context
        if (value == null) {
            result.setValid(false);
            result.setMessage("File is required");
        }
    }

    private void validateSecret(ToolParameterDTO parameter, Object value, ValidationResult result) {
        if (!(value instanceof String)) {
            result.setValid(false);
            result.setMessage("Secret must be a string");
            return;
        }

        String secret = (String) value;

        // Check min length
        if (parameter.getMinLength() != null && secret.length() < parameter.getMinLength()) {
            result.setValid(false);
            result.setMessage("Secret must be at least " + parameter.getMinLength() + " characters long");
            return;
        }

        // Check max length
        if (parameter.getMaxLength() != null && secret.length() > parameter.getMaxLength()) {
            result.setValid(false);
            result.setMessage("Secret must be at most " + parameter.getMaxLength() + " characters long");
            return;
        }

        // Check custom validation pattern
        validateWithCustomPattern(parameter, value, result);
    }

    private void validateWithCustomPattern(ToolParameterDTO parameter, Object value, ValidationResult result) {
        if (parameter.getValidationPattern() != null && !parameter.getValidationPattern().isEmpty()) {
            String strValue = value.toString();
            if (!Pattern.matches(parameter.getValidationPattern(), strValue)) {
                result.setValid(false);
                result.setMessage(parameter.getValidationMessage() != null ?
                        parameter.getValidationMessage() : "Value does not match required pattern");
            }
        }
    }
}