package viettel.dac.intentanalysisservice.validator;

import lombok.RequiredArgsConstructor;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import viettel.dac.intentanalysisservice.dto.ToolDTO;
import viettel.dac.intentanalysisservice.dto.ToolParameterDTO;
import viettel.dac.intentanalysisservice.model.IntentWithParameters;
import viettel.dac.intentanalysisservice.service.ToolService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Validator for intent parameters against tool specifications.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ParameterValidator {

    private final ToolService toolService;

    /**
     * Validate parameters for an intent against tool specifications.
     *
     * @param intent The intent with parameters to validate
     * @return List of validation errors (empty if valid)
     */
    public List<ValidationError> validateParameters(IntentWithParameters intent) {
        List<ValidationError> errors = new ArrayList<>();

        // Get tool definition for the intent
        ToolDTO tool = toolService.getToolByName(intent.getIntent());
        if (tool == null) {
            errors.add(new ValidationError("Unknown intent: " + intent.getIntent()));
            return errors;
        }

        // No parameters defined, nothing to validate
        if (tool.getParameters() == null || tool.getParameters().isEmpty()) {
            return errors;
        }

        // Check for required parameters and validate types
        for (ToolParameterDTO param : tool.getParameters()) {
            if (param.isRequired() &&
                    (intent.getParameters() == null ||
                            !intent.getParameters().containsKey(param.getName()) ||
                            intent.getParameters().get(param.getName()) == null)) {

                errors.add(new ValidationError(
                        "Required parameter missing: " + param.getName(),
                        param.getName(),
                        "MISSING_REQUIRED"
                ));
                continue;
            }

            // Skip validation for missing optional parameters
            if (!param.isRequired() &&
                    (intent.getParameters() == null ||
                            !intent.getParameters().containsKey(param.getName()) ||
                            intent.getParameters().get(param.getName()) == null)) {
                continue;
            }

            // Validate parameter type and format
            if (intent.getParameters() != null && intent.getParameters().containsKey(param.getName())) {
                Object value = intent.getParameters().get(param.getName());
                List<ValidationError> typeErrors = validateParameterType(param, value);
                errors.addAll(typeErrors);
            }
        }

        return errors;
    }

    /**
     * Normalize and convert parameters to the correct types.
     *
     * @param intent The intent with parameters to normalize
     * @return The normalized intent
     */
    public IntentWithParameters normalizeParameters(IntentWithParameters intent) {
        ToolDTO tool = toolService.getToolByName(intent.getIntent());
        if (tool == null || tool.getParameters() == null || intent.getParameters() == null) {
            return intent;
        }

        // Create a map of parameter name to definition
        Map<String, ToolParameterDTO> paramDefs = tool.getParameters().stream()
                .collect(Collectors.toMap(ToolParameterDTO::getName, p -> p));

        // Normalize each parameter
        for (Map.Entry<String, Object> entry : new ArrayList<>(intent.getParameters().entrySet())) {
            String paramName = entry.getKey();
            Object paramValue = entry.getValue();

            ToolParameterDTO paramDef = paramDefs.get(paramName);
            if (paramDef == null) {
                // Unknown parameter, leave as-is
                continue;
            }

            if (paramValue == null) {
                // Null value, leave as-is
                continue;
            }

            // Normalize and convert the value based on type
            try {
                Object normalizedValue = normalizeValue(paramValue, paramDef.getParameterType());
                intent.getParameters().put(paramName, normalizedValue);
            } catch (Exception e) {
                log.warn("Error normalizing parameter {}: {}", paramName, e.getMessage());
            }
        }

        return intent;
    }

    /**
     * Validate a parameter value against its type definition.
     *
     * @param param The parameter definition
     * @param value The parameter value
     * @return List of validation errors (empty if valid)
     */
    private List<ValidationError> validateParameterType(ToolParameterDTO param, Object value) {
        List<ValidationError> errors = new ArrayList<>();
        String paramType = param.getParameterType().toLowerCase();

        switch (paramType) {
            case "string":
                if (value != null && !(value instanceof String)) {
                    errors.add(new ValidationError(
                            "Invalid type for parameter " + param.getName() + ": expected string",
                            param.getName(),
                            "INVALID_TYPE"
                    ));
                }
                break;

            case "number":
            case "float":
            case "double":
                if (value != null && !(value instanceof Number) && !(value instanceof String)) {
                    errors.add(new ValidationError(
                            "Invalid type for parameter " + param.getName() + ": expected number",
                            param.getName(),
                            "INVALID_TYPE"
                    ));
                } else if (value instanceof String) {
                    try {
                        Double.parseDouble((String) value);
                    } catch (NumberFormatException e) {
                        errors.add(new ValidationError(
                                "Invalid value for parameter " + param.getName() + ": not a valid number",
                                param.getName(),
                                "INVALID_FORMAT"
                        ));
                    }
                }
                break;

            case "integer":
            case "int":
                if (value != null && !(value instanceof Integer) && !(value instanceof Long) && !(value instanceof String)) {
                    errors.add(new ValidationError(
                            "Invalid type for parameter " + param.getName() + ": expected integer",
                            param.getName(),
                            "INVALID_TYPE"
                    ));
                } else if (value instanceof String) {
                    try {
                        Integer.parseInt((String) value);
                    } catch (NumberFormatException e) {
                        errors.add(new ValidationError(
                                "Invalid value for parameter " + param.getName() + ": not a valid integer",
                                param.getName(),
                                "INVALID_FORMAT"
                        ));
                    }
                } else if (value instanceof Double || value instanceof Float) {
                    errors.add(new ValidationError(
                            "Invalid value for parameter " + param.getName() + ": decimal value provided for integer parameter",
                            param.getName(),
                            "INVALID_FORMAT"
                    ));
                }
                break;

            case "boolean":
                if (value != null && !(value instanceof Boolean) && !(value instanceof String)) {
                    errors.add(new ValidationError(
                            "Invalid type for parameter " + param.getName() + ": expected boolean",
                            param.getName(),
                            "INVALID_TYPE"
                    ));
                } else if (value instanceof String) {
                    String strValue = ((String) value).toLowerCase();
                    if (!strValue.equals("true") && !strValue.equals("false")) {
                        errors.add(new ValidationError(
                                "Invalid value for parameter " + param.getName() + ": expected 'true' or 'false'",
                                param.getName(),
                                "INVALID_FORMAT"
                        ));
                    }
                }
                break;

            case "date":
                if (value != null && !(value instanceof String) && !(value instanceof Map)) {
                    errors.add(new ValidationError(
                            "Invalid type for parameter " + param.getName() + ": expected date string or object",
                            param.getName(),
                            "INVALID_TYPE"
                    ));
                } else if (value instanceof String) {
                    try {
                        LocalDate.parse((String) value);
                    } catch (DateTimeParseException e) {
                        try {
                            // Try alternative formats
                            LocalDateTime.parse((String) value, DateTimeFormatter.ISO_DATE_TIME);
                        } catch (DateTimeParseException e2) {
                            errors.add(new ValidationError(
                                    "Invalid value for parameter " + param.getName() + ": not a valid date format",
                                    param.getName(),
                                    "INVALID_FORMAT"
                            ));
                        }
                    }
                }
                break;

            case "array":
            case "list":
                if (value != null && !(value instanceof List)) {
                    errors.add(new ValidationError(
                            "Invalid type for parameter " + param.getName() + ": expected array",
                            param.getName(),
                            "INVALID_TYPE"
                    ));
                }
                break;

            case "object":
            case "map":
                if (value != null && !(value instanceof Map)) {
                    errors.add(new ValidationError(
                            "Invalid type for parameter " + param.getName() + ": expected object",
                            param.getName(),
                            "INVALID_TYPE"
                    ));
                }
                break;
        }

        return errors;
    }

    /**
     * Normalize and convert a value to the correct type.
     *
     * @param value The value to normalize
     * @param type The target type
     * @return The normalized value
     */
    private Object normalizeValue(Object value, String type) {
        if (value == null) {
            return null;
        }

        String paramType = type.toLowerCase();

        switch (paramType) {
            case "string":
                return value.toString();

            case "number":
            case "float":
            case "double":
                if (value instanceof String) {
                    try {
                        return Double.parseDouble((String) value);
                    } catch (NumberFormatException e) {
                        return value;
                    }
                } else if (value instanceof Number) {
                    return ((Number) value).doubleValue();
                }
                return value;

            case "integer":
            case "int":
                if (value instanceof String) {
                    try {
                        return Integer.parseInt((String) value);
                    } catch (NumberFormatException e) {
                        try {
                            // Try parsing as double and converting to int
                            return (int) Double.parseDouble((String) value);
                        } catch (NumberFormatException e2) {
                            return value;
                        }
                    }
                } else if (value instanceof Number) {
                    return ((Number) value).intValue();
                }
                return value;

            case "boolean":
                if (value instanceof String) {
                    String strValue = ((String) value).toLowerCase();
                    return "true".equals(strValue) || "yes".equals(strValue) || "1".equals(strValue);
                }
                return value;

            case "date":
                if (value instanceof String) {
                    try {
                        return LocalDate.parse((String) value);
                    } catch (DateTimeParseException e) {
                        try {
                            return LocalDateTime.parse((String) value, DateTimeFormatter.ISO_DATE_TIME);
                        } catch (DateTimeParseException e2) {
                            return value;
                        }
                    }
                }
                return value;

            default:
                return value;
        }
    }

    /**
     * Representation of a validation error.
     */
    @Data
    @AllArgsConstructor
    public static class ValidationError {
        private String message;
        private String parameter;
        private String code;

        public ValidationError(String message) {
            this.message = message;
            this.code = "VALIDATION_ERROR";
        }
    }
}
