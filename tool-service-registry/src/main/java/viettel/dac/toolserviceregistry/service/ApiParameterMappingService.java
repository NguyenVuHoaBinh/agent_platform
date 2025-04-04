// File: src/main/java/viettel/dac/toolserviceregistry/service/ApiParameterMappingService.java
package viettel.dac.toolserviceregistry.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.errors.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import viettel.dac.toolserviceregistry.exception.InvalidParameterMappingException;
import viettel.dac.toolserviceregistry.exception.ToolNotFoundException;
import viettel.dac.toolserviceregistry.exception.ToolParameterNotFoundException;
import viettel.dac.toolserviceregistry.exception.ToolTypeNotCompatibleException;
import viettel.dac.toolserviceregistry.model.dto.ApiParameterMappingDTO;
import viettel.dac.toolserviceregistry.model.entity.ApiParameterMapping;
import viettel.dac.toolserviceregistry.model.entity.ApiToolMetadata;
import viettel.dac.toolserviceregistry.model.entity.Tool;
import viettel.dac.toolserviceregistry.model.entity.ToolParameter;
import viettel.dac.toolserviceregistry.model.enums.ApiParameterLocation;
import viettel.dac.toolserviceregistry.model.enums.ParameterSource;
import viettel.dac.toolserviceregistry.model.enums.ToolType;
import viettel.dac.toolserviceregistry.model.request.ApiParameterMappingRequest;
import viettel.dac.toolserviceregistry.repository.ApiParameterMappingRepository;
import viettel.dac.toolserviceregistry.repository.ApiToolMetadataRepository;
import viettel.dac.toolserviceregistry.repository.ToolParameterRepository;
import viettel.dac.toolserviceregistry.repository.ToolRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing API parameter mappings.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ApiParameterMappingService {
    private final ApiParameterMappingRepository apiParameterMappingRepository;
    private final ApiToolMetadataRepository apiToolMetadataRepository;
    private final ToolRepository toolRepository;
    private final ToolParameterRepository toolParameterRepository;

    /**
     * Get parameter mappings for an API tool.
     *
     * @param toolId The ID of the API tool
     * @return List of parameter mappings
     */
    public List<ApiParameterMappingDTO> getParameterMappings(String toolId) {
        log.debug("Getting API parameter mappings for tool: {}", toolId);

        Tool tool = toolRepository.findById(toolId)
                .orElseThrow(() -> new ToolNotFoundException(toolId));

        if (tool.getToolType() != ToolType.API_TOOL) {
            throw new ToolTypeNotCompatibleException(toolId, "API_TOOL");
        }

        ApiToolMetadata metadata = apiToolMetadataRepository.findByToolId(toolId)
                .orElseThrow(() -> new ToolTypeNotCompatibleException(toolId, "API_TOOL missing metadata"));

        List<ApiParameterMapping> mappings = apiParameterMappingRepository.findByApiToolMetadataId(metadata.getId());

        return mappings.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    /**
     * Create a new parameter mapping for an API tool.
     *
     * @param toolId The ID of the API tool
     * @param parameterId The ID of the tool parameter
     * @param request The mapping request
     * @return The created mapping
     */
    @Transactional
    public ApiParameterMappingDTO createParameterMapping(
            String toolId, String parameterId, ApiParameterMappingRequest request) {
        log.debug("Creating API parameter mapping for tool: {} and parameter: {}", toolId, parameterId);

        Tool tool = toolRepository.findById(toolId)
                .orElseThrow(() -> new ToolNotFoundException(toolId));

        if (tool.getToolType() != ToolType.API_TOOL) {
            throw new ToolTypeNotCompatibleException(toolId, "API_TOOL");
        }

        ToolParameter parameter = toolParameterRepository.findById(parameterId)
                .orElseThrow(() -> new ToolParameterNotFoundException(parameterId));

        // Verify parameter belongs to the tool
        if (!parameter.getTool().getId().equals(toolId)) {
            throw new InvalidParameterMappingException(parameter.getName(),
                    "Parameter does not belong to the specified tool");
        }

        ApiToolMetadata metadata = apiToolMetadataRepository.findByToolId(toolId)
                .orElseThrow(() -> new ToolTypeNotCompatibleException(toolId, "API_TOOL missing metadata"));

        // Create new mapping
        ApiParameterMapping mapping = new ApiParameterMapping();
        mapping.setId(UUID.randomUUID().toString());
        mapping.setApiToolMetadata(metadata);
        mapping.setToolParameter(parameter);
        mapping.setApiLocation(request.getApiLocation());
        mapping.setApiParameterName(request.getApiParameterName());
        mapping.setRequiredForApi(request.isRequiredForApi());
        mapping.setTransformationExpression(request.getTransformationExpression());
        mapping.setResponseExtractionPath(request.getResponseExtractionPath());

        // If this is a response parameter, update parameter source
        if (request.getApiLocation() == ApiParameterLocation.RESPONSE) {
            parameter.setParameterSource(ParameterSource.API_RESPONSE);
            parameter.setExtractionPath(request.getResponseExtractionPath());
            toolParameterRepository.save(parameter);
        }

        ApiParameterMapping savedMapping = apiParameterMappingRepository.save(mapping);

        return mapToDto(savedMapping);
    }

    /**
     * Update an existing parameter mapping.
     *
     * @param mappingId The ID of the mapping to update
     * @param request The mapping request
     * @return The updated mapping
     */
    @Transactional
    public ApiParameterMappingDTO updateParameterMapping(String mappingId, ApiParameterMappingRequest request) {
        log.debug("Updating API parameter mapping: {}", mappingId);

        ApiParameterMapping mapping = apiParameterMappingRepository.findById(mappingId)
                .orElseThrow(() -> new ResourceNotFoundException("API parameter mapping", mappingId));

        mapping.setApiLocation(request.getApiLocation());
        mapping.setApiParameterName(request.getApiParameterName());
        mapping.setRequiredForApi(request.isRequiredForApi());
        mapping.setTransformationExpression(request.getTransformationExpression());
        mapping.setResponseExtractionPath(request.getResponseExtractionPath());

        // If this is a response parameter, update parameter source
        if (request.getApiLocation() == ApiParameterLocation.RESPONSE) {
            ToolParameter parameter = mapping.getToolParameter();
            parameter.setParameterSource(ParameterSource.API_RESPONSE);
            parameter.setExtractionPath(request.getResponseExtractionPath());
            toolParameterRepository.save(parameter);
        }

        ApiParameterMapping savedMapping = apiParameterMappingRepository.save(mapping);

        return mapToDto(savedMapping);
    }

    /**
     * Delete a parameter mapping.
     *
     * @param mappingId The ID of the mapping to delete
     */
    @Transactional
    public void deleteParameterMapping(String mappingId) {
        log.debug("Deleting API parameter mapping: {}", mappingId);

        ApiParameterMapping mapping = apiParameterMappingRepository.findById(mappingId)
                .orElseThrow(() -> new ResourceNotFoundException("API parameter mapping", mappingId));

        // If this was a response parameter, reset parameter source
        if (mapping.getApiLocation() == ApiParameterLocation.RESPONSE) {
            ToolParameter parameter = mapping.getToolParameter();
            parameter.setParameterSource(ParameterSource.USER_INPUT);
            parameter.setExtractionPath(null);
            toolParameterRepository.save(parameter);
        }

        apiParameterMappingRepository.delete(mapping);
    }

    /**
     * Generate API parameter mappings automatically based on tool parameters.
     *
     * @param toolId The ID of the API tool
     * @return List of created mappings
     */
    @Transactional
    public List<ApiParameterMappingDTO> generateParameterMappings(String toolId) {
        log.debug("Generating API parameter mappings for tool: {}", toolId);

        Tool tool = toolRepository.findById(toolId)
                .orElseThrow(() -> new ToolNotFoundException(toolId));

        if (tool.getToolType() != ToolType.API_TOOL) {
            throw new ToolTypeNotCompatibleException(toolId, "API_TOOL");
        }

        ApiToolMetadata metadata = apiToolMetadataRepository.findByToolId(toolId)
                .orElseThrow(() -> new ToolTypeNotCompatibleException(toolId, "API_TOOL missing metadata"));

        // Delete existing mappings
        List<ApiParameterMapping> existingMappings =
                apiParameterMappingRepository.findByApiToolMetadataId(metadata.getId());
        apiParameterMappingRepository.deleteAll(existingMappings);

        // Generate new mappings
        List<ApiParameterMapping> newMappings = new ArrayList<>();

        for (ToolParameter parameter : tool.getParameters()) {
            ApiParameterMapping mapping = new ApiParameterMapping();
            mapping.setId(UUID.randomUUID().toString());
            mapping.setApiToolMetadata(metadata);
            mapping.setToolParameter(parameter);

            // Determine API location and name based on parameter characteristics
            if (parameter.getName().toLowerCase().contains("header")) {
                mapping.setApiLocation(ApiParameterLocation.HEADER);
            } else if (parameter.getName().toLowerCase().contains("path")) {
                mapping.setApiLocation(ApiParameterLocation.PATH);
            } else if (parameter.getName().toLowerCase().contains("body")) {
                mapping.setApiLocation(ApiParameterLocation.BODY);
            } else {
                mapping.setApiLocation(ApiParameterLocation.QUERY);
            }

            mapping.setApiParameterName(parameter.getName());
            mapping.setRequiredForApi(parameter.isRequired());

            newMappings.add(mapping);
        }

        List<ApiParameterMapping> savedMappings = apiParameterMappingRepository.saveAll(newMappings);

        return savedMappings.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    /**
     * Map an ApiParameterMapping entity to an ApiParameterMappingDTO.
     *
     * @param mapping The entity to map
     * @return The mapped DTO
     */
    private ApiParameterMappingDTO mapToDto(ApiParameterMapping mapping) {
        return ApiParameterMappingDTO.builder()
                .id(mapping.getId())
                .toolParameterId(mapping.getToolParameter().getId())
                .apiLocation(mapping.getApiLocation())
                .apiParameterName(mapping.getApiParameterName())
                .requiredForApi(mapping.isRequiredForApi())
                .transformationExpression(mapping.getTransformationExpression())
                .responseExtractionPath(mapping.getResponseExtractionPath())
                .build();
    }
}