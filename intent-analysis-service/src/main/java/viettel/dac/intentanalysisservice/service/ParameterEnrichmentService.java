package viettel.dac.intentanalysisservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import viettel.dac.intentanalysisservice.dto.ToolDTO;
import viettel.dac.intentanalysisservice.dto.ToolParameterDTO;
import viettel.dac.intentanalysisservice.model.IntentWithParameters;
import viettel.dac.intentanalysisservice.validator.ParameterValidator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for enriching and transforming parameters.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ParameterEnrichmentService {

    private final ToolService toolService;
    private final ParameterValidator validator;

    /**
     * Enrich and transform parameters for an intent.
     *
     * @param intent The intent with parameters to enrich
     * @return The enriched intent
     */
    public IntentWithParameters enrichParameters(IntentWithParameters intent) {
        if (intent == null) {
            return null;
        }

        // Validate and normalize parameters
        IntentWithParameters normalizedIntent = validator.normalizeParameters(intent);
        List<ParameterValidator.ValidationError> errors = validator.validateParameters(normalizedIntent);

        if (!errors.isEmpty()) {
            log.debug("Validation errors for intent {}: {}", intent.getIntent(), errors);
        }

        // Fill in default values for missing optional parameters
        normalizedIntent = fillDefaultValues(normalizedIntent);

        // Add metadata about validation status
        addValidationMetadata(normalizedIntent, errors);

        return normalizedIntent;
    }

    /**
     * Enrich and transform parameters for multiple intents.
     *
     * @param intents List of intents with parameters to enrich
     * @return List of enriched intents
     */
    public List<IntentWithParameters> enrichParameters(List<IntentWithParameters> intents) {
        if (intents == null) {
            return null;
        }

        return intents.stream()
                .map(this::enrichParameters)
                .collect(Collectors.toList());
    }

    /**
     * Fill in default values for missing optional parameters.
     *
     * @param intent The intent to fill defaults for
     * @return The intent with defaults filled
     */
    private IntentWithParameters fillDefaultValues(IntentWithParameters intent) {
        ToolDTO tool = toolService.getToolByName(intent.getIntent());
        if (tool == null || tool.getParameters() == null || intent.getParameters() == null) {
            return intent;
        }

        Map<String, Object> enrichedParams = new HashMap<>(intent.getParameters());

        for (ToolParameterDTO param : tool.getParameters()) {
            // Skip required parameters or parameters that already have a value
            if (param.isRequired() || enrichedParams.containsKey(param.getName())) {
                continue;
            }

            // Add default value if available
            if (param.getDefaultValue() != null && !param.getDefaultValue().isEmpty()) {
                // Convert default value to appropriate type
                Object defaultValue = convertDefaultValue(param.getDefaultValue(), param.getParameterType());
                enrichedParams.put(param.getName(), defaultValue);
            }
        }

        intent.setParameters(enrichedParams);
        return intent;
    }

    /**
     * Convert default value string to appropriate type.
     *
     * @param defaultValue The default value as string
     * @param paramType The parameter type
     * @return Converted default value
     */
    private Object convertDefaultValue(String defaultValue, String paramType) {
        if (defaultValue == null) {
            return null;
        }

        switch (paramType.toLowerCase()) {
            case "integer":
            case "int":
                try {
                    return Integer.parseInt(defaultValue);
                } catch (NumberFormatException e) {
                    return defaultValue;
                }

            case "number":
            case "float":
            case "double":
                try {
                    return Double.parseDouble(defaultValue);
                } catch (NumberFormatException e) {
                    return defaultValue;
                }

            case "boolean":
                return Boolean.parseBoolean(defaultValue);

            default:
                return defaultValue;
        }
    }

    /**
     * Add metadata about validation status to the intent.
     *
     * @param intent The intent to add metadata to
     * @param errors List of validation errors
     */
    private void addValidationMetadata(IntentWithParameters intent, List<ParameterValidator.ValidationError> errors) {
        // We can't directly add metadata to IntentWithParameters, but in a real implementation
        // you would add metadata about validation status

        // For this example, we'll log validation issues
        if (!errors.isEmpty()) {
            log.debug("Validation issues for intent {}: {}", intent.getIntent(),
                    errors.stream().map(ParameterValidator.ValidationError::getMessage)
                            .collect(Collectors.joining(", ")));
        }
    }
}