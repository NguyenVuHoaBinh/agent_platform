// File: src/main/java/viettel/dac/toolserviceregistry/controller/ParameterController.java
package viettel.dac.toolserviceregistry.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import viettel.dac.toolserviceregistry.model.dto.ToolParameterDTO;
import viettel.dac.toolserviceregistry.model.dto.ValidationResult;
import viettel.dac.toolserviceregistry.model.enums.ParameterSource;
import viettel.dac.toolserviceregistry.model.enums.ParameterType;
import viettel.dac.toolserviceregistry.service.ParameterValidationService;
import viettel.dac.toolserviceregistry.service.ToolParameterService;

import java.util.*;
import java.util.stream.Collectors;

/**
 * REST controller for parameter operations.
 */
@RestController
@RequestMapping("/parameters")
@RequiredArgsConstructor
@Slf4j
public class ParameterController {
    private final ParameterValidationService validationService;
    private final ToolParameterService parameterService;

    /**
     * Validates a parameter value.
     *
     * @param parameterId The ID of the parameter
     * @param value The value to validate
     * @return Validation result
     */
    @PostMapping("/{parameterId}/validate")
    public ResponseEntity<ValidationResult> validateParameter(
            @PathVariable String parameterId,
            @RequestBody Object value) {
        log.info("Validating parameter: {}", parameterId);

        ToolParameterDTO parameter = parameterService.getParameter(parameterId);
        ValidationResult result = validationService.validateParameterValue(parameter, value);

        return ResponseEntity.ok(result);
    }

    /**
     * Gets parameter suggestions.
     *
     * @param parameterId The ID of the parameter
     * @param prefix Optional prefix to filter suggestions
     * @return List of suggestions
     */
    @GetMapping("/{parameterId}/suggestions")
    public ResponseEntity<List<String>> getParameterSuggestions(
            @PathVariable String parameterId,
            @RequestParam(required = false) String prefix) {
        log.info("Getting suggestions for parameter: {}", parameterId);

        ToolParameterDTO parameter = parameterService.getParameter(parameterId);
        List<String> suggestions = validationService.suggestParameterValues(parameter, prefix);

        return ResponseEntity.ok(suggestions);
    }

    /**
     * Validates multiple parameters.
     *
     * @param toolId The ID of the tool
     * @param values The parameter values to validate
     * @return Map of validation results by parameter name
     */
    @PostMapping("/validate-batch")
    public ResponseEntity<Map<String, ValidationResult>> validateParameters(
            @RequestParam String toolId,
            @RequestBody Map<String, Object> values) {
        log.info("Validating parameters for tool: {}", toolId);

        Map<String, ToolParameterDTO> parameters = parameterService.getParametersByToolId(toolId);
        Map<String, ValidationResult> results = validationService.validateParameters(parameters, values);

        return ResponseEntity.ok(results);
    }

    /**
     * Gets all parameter types.
     *
     * @return List of parameter types with descriptions
     */
    @GetMapping("/types")
    public List<Map<String, String>> getParameterTypes() {
        return Arrays.stream(ParameterType.values())
                .map(type -> {
                    Map<String, String> typeInfo = new HashMap<>();
                    typeInfo.put("name", type.name());
                    typeInfo.put("description", getParameterTypeDescription(type));
                    return typeInfo;
                })
                .collect(Collectors.toList());
    }

    /**
     * Gets all parameter sources.
     *
     * @return List of parameter sources with descriptions
     */
    @GetMapping("/sources")
    public List<Map<String, String>> getParameterSources() {
        return Arrays.stream(ParameterSource.values())
                .map(source -> {
                    Map<String, String> sourceInfo = new HashMap<>();
                    sourceInfo.put("name", source.name());
                    sourceInfo.put("description", getParameterSourceDescription(source));
                    return sourceInfo;
                })
                .collect(Collectors.toList());
    }

    /**
     * Gets a description for a parameter type.
     *
     * @param type The parameter type
     * @return The description of the parameter type
     */
    private String getParameterTypeDescription(ParameterType type) {
        switch (type) {
            case STRING:
                return "Text values";
            case NUMBER:
                return "Numeric values (integers or decimals)";
            case BOOLEAN:
                return "True/false values";
            case ARRAY:
                return "List of values";
            case OBJECT:
                return "Complex structured object";
            case DATE:
                return "Date values";
            case DATETIME:
                return "Date and time values";
            case EMAIL:
                return "Email addresses";
            case URL:
                return "URL/URI values";
            case FILE:
                return "File references";
            case ENUM:
                return "Enumeration of predefined values";
            case JSON:
                return "JSON formatted string";
            case XML:
                return "XML formatted string";
            case SECRET:
                return "Sensitive values (passwords, tokens, etc.)";
            default:
                return "Unknown parameter type";
        }
    }

    /**
     * Gets a description for a parameter source.
     *
     * @param source The parameter source
     * @return The description of the parameter source
     */
    private String getParameterSourceDescription(ParameterSource source) {
        switch (source) {
            case USER_INPUT:
                return "Parameter must be provided by the user";
            case SYSTEM_PROVIDED:
                return "Parameter is provided by the system";
            case DEPENDENT_TOOL:
                return "Parameter comes from another tool";
            case DEFAULT_VALUE:
                return "Parameter uses its default value if not specified";
            case CONTEXT_VARIABLE:
                return "Parameter comes from execution context";
            case API_RESPONSE:
                return "Parameter comes from an API response";
            case COMPUTED:
                return "Parameter is computed during execution";
            default:
                return "Unknown parameter source";
        }
    }
}