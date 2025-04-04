// File: src/main/java/viettel/dac/toolserviceregistry/service/ToolParameterService.java
package viettel.dac.toolserviceregistry.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import viettel.dac.toolserviceregistry.exception.ToolNotFoundException;
import viettel.dac.toolserviceregistry.exception.ToolParameterNotFoundException;
import viettel.dac.toolserviceregistry.model.dto.ToolParameterDTO;
import viettel.dac.toolserviceregistry.model.entity.Tool;
import viettel.dac.toolserviceregistry.model.entity.ToolParameter;
import viettel.dac.toolserviceregistry.model.enums.ParameterSource;
import viettel.dac.toolserviceregistry.repository.ToolParameterRepository;
import viettel.dac.toolserviceregistry.repository.ToolRepository;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for managing tool parameters.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ToolParameterService {
    private final ToolParameterRepository parameterRepository;
    private final ToolRepository toolRepository;

    /**
     * Gets a parameter by ID.
     *
     * @param parameterId The ID of the parameter
     * @return The parameter DTO
     */
    public ToolParameterDTO getParameter(String parameterId) {
        log.debug("Getting parameter: {}", parameterId);

        ToolParameter parameter = parameterRepository.findById(parameterId)
                .orElseThrow(() -> new ToolParameterNotFoundException(parameterId));

        return mapToDto(parameter);
    }

    /**
     * Gets all parameters for a tool.
     *
     * @param toolId The ID of the tool
     * @return Map of parameter DTOs by name
     */
    public Map<String, ToolParameterDTO> getParametersByToolId(String toolId) {
        log.debug("Getting parameters for tool: {}", toolId);

        Tool tool = toolRepository.findById(toolId)
                .orElseThrow(() -> new ToolNotFoundException(toolId));

        Map<String, ToolParameterDTO> parameters = new HashMap<>();

        for (ToolParameter parameter : tool.getParameters()) {
            parameters.put(parameter.getName(), mapToDto(parameter));
        }

        return parameters;
    }

    /**
     * Gets all parameters for a tool by source.
     *
     * @param toolId The ID of the tool
     * @param source The parameter source
     * @return List of parameter DTOs
     */
    public List<ToolParameterDTO> getParametersBySource(String toolId, ParameterSource source) {
        log.debug("Getting parameters with source {} for tool: {}", source, toolId);

        Tool tool = toolRepository.findById(toolId)
                .orElseThrow(() -> new ToolNotFoundException(toolId));

        return tool.getParameters().stream()
                .filter(param -> param.getParameterSource() == source)
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    /**
     * Map a ToolParameter entity to a ToolParameterDTO.
     *
     * @param parameter The entity to map
     * @return The mapped DTO
     */
    private ToolParameterDTO mapToDto(ToolParameter parameter) {
        if (parameter == null) {
            return null;
        }

        List<String> allowedValues = null;
        if (parameter.getAllowedValues() != null && !parameter.getAllowedValues().isEmpty()) {
            allowedValues = Arrays.asList(parameter.getAllowedValues().split(","));
        }

        return ToolParameterDTO.builder()
                .id(parameter.getId())
                .name(parameter.getName())
                .description(parameter.getDescription())
                .parameterType(parameter.getParameterType())
                .required(parameter.isRequired())
                .defaultValue(parameter.getDefaultValue())
                .validationPattern(parameter.getValidationPattern())
                .validationMessage(parameter.getValidationMessage())
                .conditionalOn(parameter.getConditionalOn())
                .priority(parameter.getPriority())
                .examples(parameter.getExamples())
                .suggestionQuery(parameter.getSuggestionQuery())
                .parameterSource(parameter.getParameterSource())
                .minValue(parameter.getMinValue())
                .maxValue(parameter.getMaxValue())
                .minLength(parameter.getMinLength())
                .maxLength(parameter.getMaxLength())
                .allowedValues(allowedValues)
                .formatHint(parameter.getFormatHint())
                .sensitive(parameter.isSensitive())
                .isArray(parameter.isArray())
                .arrayItemType(parameter.getArrayItemType())
                .objectSchema(parameter.getObjectSchema())
                .extractionPath(parameter.getExtractionPath())
                .build();
    }
}