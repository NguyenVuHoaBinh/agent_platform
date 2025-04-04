package viettel.dac.toolserviceregistry.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import viettel.dac.toolserviceregistry.exception.*;
import viettel.dac.toolserviceregistry.mapper.ToolMapper;
import viettel.dac.toolserviceregistry.model.dto.ToolDTO;
import viettel.dac.toolserviceregistry.model.entity.*;
import viettel.dac.toolserviceregistry.model.enums.ToolEventType;
import viettel.dac.toolserviceregistry.model.enums.ToolType;
import viettel.dac.toolserviceregistry.model.event.ToolEvent;
import viettel.dac.toolserviceregistry.model.request.*;
import viettel.dac.toolserviceregistry.repository.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for tool command operations (create, update, delete).
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class ToolCommandService {
    private final ToolRepository toolRepository;
    private final ToolParameterRepository parameterRepository;
    private final ToolDependencyRepository dependencyRepository;
    private final ToolCategoryRepository categoryRepository;
    private final ToolExampleRepository exampleRepository;
    private final EventPublisher eventPublisher;
    private final ToolValidator toolValidator;
    private final DependencyValidator dependencyValidator;
    private final ObjectMapper objectMapper;
    private final ToolDependencyGraphService graphService;
    private final ToolMapper toolMapper;
    private final ApiToolMetadataRepository apiToolMetadataRepository;
    private final ApiToolService apiToolService;

    @Value("${kafka.topic.tool-events}")
    private String toolEventsTopic;

    /**
     * Creates a new tool.
     *
     * @param request The tool creation request
     * @return The ID of the created tool
     */
    public String createTool(CreateToolRequest request) {
        log.info("Creating tool: {}", request.getName());

        // Validate tool name uniqueness
        toolRepository.findByName(request.getName())
                .ifPresent(existing -> {
                    throw new DuplicateToolNameException(request.getName());
                });

        // Generate ID if not provided
        String toolId = request.getId() != null ?
                request.getId() : UUID.randomUUID().toString();

        // Create tool entity
        Tool tool = new Tool();
        tool.setId(toolId);
        tool.setName(request.getName());
        tool.setDescription(request.getDescription());
        tool.setToolType(request.getToolType()); // Set tool type
        tool.setActive(true);
        tool.setVersion(1);
        tool.setCreatedAt(LocalDateTime.now());
        tool.setUpdatedAt(LocalDateTime.now());

        // Add parameters
        if (request.getParameters() != null) {
            Set<String> paramNames = new HashSet<>();
            for (ToolParameterRequest paramReq : request.getParameters()) {
                // Check for duplicate parameter names
                if (!paramNames.add(paramReq.getName())) {
                    throw new DuplicateParameterNameException(paramReq.getName());
                }

                ToolParameter param = mapToToolParameter(paramReq);
                param.setId(UUID.randomUUID().toString());
                tool.addParameter(param);
            }
        }

        // Add categories
        if (request.getCategoryIds() != null && !request.getCategoryIds().isEmpty()) {
            Set<ToolCategory> categories = categoryRepository.findByIdIn(request.getCategoryIds());
            if (categories.size() != request.getCategoryIds().size()) {
                log.warn("Some category IDs were not found");
            }
            categories.forEach(tool::addCategory);
        }

        // Add examples
        if (request.getExamples() != null) {
            for (ToolExampleRequest exampleReq : request.getExamples()) {
                ToolExample example = mapToToolExample(exampleReq);
                example.setId(UUID.randomUUID().toString());
                tool.addExample(example);
            }
        }

        // Save tool first to establish ID
        Tool savedTool = toolRepository.save(tool);

        // Add API metadata if this is an API tool
        if (request.getToolType() == ToolType.API_TOOL && request.getApiMetadata() != null) {
            ApiToolMetadata apiMetadata = mapToApiToolMetadata(request.getApiMetadata(), savedTool);
            apiMetadata.setId(UUID.randomUUID().toString());

            // Add headers
            if (request.getApiMetadata().getHeaders() != null) {
                for (ApiHeaderRequest headerReq : request.getApiMetadata().getHeaders()) {
                    ApiHeader header = mapToApiHeader(headerReq);
                    header.setId(UUID.randomUUID().toString());
                    apiMetadata.addHeader(header);
                }
            }

            apiToolMetadataRepository.save(apiMetadata);
        }

        // Add dependencies (after tool is saved to have ID)
        if (request.getDependencies() != null && !request.getDependencies().isEmpty()) {
            // Validate dependencies to prevent cycles
            dependencyValidator.validateNoCycles(toolId, request.getDependencies());

            for (ToolDependencyRequest depReq : request.getDependencies()) {
                Tool dependencyTool = toolRepository.findById(depReq.getDependencyToolId())
                        .orElseThrow(() -> new DependencyToolNotFoundException(depReq.getDependencyToolId()));

                ToolDependency dependency = mapToToolDependency(depReq, savedTool, dependencyTool);
                dependency.setId(UUID.randomUUID().toString());

                // Add parameter mappings
                if (depReq.getParameterMappings() != null) {
                    // Validate parameter mappings
                    dependencyValidator.validateParameterMappings(
                            toolId,
                            depReq.getDependencyToolId(),
                            depReq.getParameterMappings());

                    for (ParameterMappingRequest mappingReq : depReq.getParameterMappings()) {
                        ParameterMapping mapping = mapToParameterMapping(mappingReq);
                        mapping.setId(UUID.randomUUID().toString());
                        dependency.addParameterMapping(mapping);
                    }
                }

                savedTool.addDependency(dependency);
            }

            // Save again with dependencies
            savedTool = toolRepository.save(savedTool);
        }

        // Publish tool created event
        publishToolEvent(savedTool, ToolEventType.TOOL_CREATED);

        return toolId;
    }

    /**
     * Updates an existing tool.
     *
     * @param id The ID of the tool to update
     * @param request The tool update request
     * @return The new version of the tool
     */
    public int updateTool(String id, UpdateToolRequest request) {
        log.info("Updating tool: {}", id);

        // Find existing tool
        Tool tool = toolRepository.findById(id)
                .orElseThrow(() -> new ToolNotFoundException(id));

        // Update basic properties
        tool.setName(request.getName());
        tool.setDescription(request.getDescription());
        tool.setToolType(request.getToolType()); // Update tool type
        tool.setUpdatedAt(LocalDateTime.now());
        tool.setVersion(tool.getVersion() + 1);

        // Update parameters (remove all and re-add)
        tool.getParameters().clear();
        if (request.getParameters() != null) {
            Set<String> paramNames = new HashSet<>();
            for (ToolParameterRequest paramReq : request.getParameters()) {
                // Check for duplicate parameter names
                if (!paramNames.add(paramReq.getName())) {
                    throw new DuplicateParameterNameException(paramReq.getName());
                }

                ToolParameter param = mapToToolParameter(paramReq);
                param.setId(paramReq.getId() != null ?
                        paramReq.getId() : UUID.randomUUID().toString());
                tool.addParameter(param);
            }
        }

        // Update categories (remove all and re-add)
        tool.getCategories().clear();
        if (request.getCategoryIds() != null && !request.getCategoryIds().isEmpty()) {
            Set<ToolCategory> categories = categoryRepository.findByIdIn(request.getCategoryIds());
            if (categories.size() != request.getCategoryIds().size()) {
                log.warn("Some category IDs were not found");
            }
            categories.forEach(tool::addCategory);
        }

        // Update examples (remove all and re-add)
        tool.getExamples().clear();
        if (request.getExamples() != null) {
            for (ToolExampleRequest exampleReq : request.getExamples()) {
                ToolExample example = mapToToolExample(exampleReq);
                example.setId(exampleReq.getId() != null ?
                        exampleReq.getId() : UUID.randomUUID().toString());
                tool.addExample(example);
            }
        }

        // Save tool with updated properties, parameters, categories, and examples
        Tool savedTool = toolRepository.save(tool);

        // Update API metadata if this is an API tool
        if (request.getToolType() == ToolType.API_TOOL && request.getApiMetadata() != null) {
            // Find existing API metadata or create new
            ApiToolMetadata apiMetadata = apiToolMetadataRepository.findByToolId(id)
                    .orElse(new ApiToolMetadata());

            // Update API metadata
            updateApiToolMetadata(apiMetadata, request.getApiMetadata(), savedTool);

            // Save API metadata
            apiToolMetadataRepository.save(apiMetadata);
        } else if (request.getToolType() != ToolType.API_TOOL) {
            // If tool type changed from API_TOOL to something else, remove API metadata
            apiToolMetadataRepository.findByToolId(id).ifPresent(apiToolMetadataRepository::delete);
        }

        // Update dependencies (remove all and re-add)
        tool.getDependencies().clear();
        toolRepository.save(tool); // Save to remove dependencies first

        if (request.getDependencies() != null && !request.getDependencies().isEmpty()) {
            // Validate dependencies to prevent cycles
            dependencyValidator.validateNoCycles(id, request.getDependencies());

            for (ToolDependencyRequest depReq : request.getDependencies()) {
                Tool dependencyTool = toolRepository.findById(depReq.getDependencyToolId())
                        .orElseThrow(() -> new DependencyToolNotFoundException(depReq.getDependencyToolId()));

                ToolDependency dependency = mapToToolDependency(depReq, savedTool, dependencyTool);
                dependency.setId(depReq.getId() != null ?
                        depReq.getId() : UUID.randomUUID().toString());

                // Add parameter mappings
                if (depReq.getParameterMappings() != null) {
                    // Validate parameter mappings
                    dependencyValidator.validateParameterMappings(
                            id,
                            depReq.getDependencyToolId(),
                            depReq.getParameterMappings());

                    for (ParameterMappingRequest mappingReq : depReq.getParameterMappings()) {
                        ParameterMapping mapping = mapToParameterMapping(mappingReq);
                        mapping.setId(mappingReq.getId() != null ?
                                mappingReq.getId() : UUID.randomUUID().toString());
                        dependency.addParameterMapping(mapping);
                    }
                }

                savedTool.addDependency(dependency);
            }

            // Save again with dependencies
            savedTool = toolRepository.save(savedTool);
        }

        // Publish tool updated event
        publishToolEvent(savedTool, ToolEventType.TOOL_UPDATED);

        return savedTool.getVersion();
    }

    /**
     * Deletes a tool.
     *
     * @param id The ID of the tool to delete
     */
    public void deleteTool(String id) {
        log.info("Deleting tool: {}", id);

        // Check for existing dependents
        long dependentCount = dependencyRepository.countByDependencyToolId(id);
        if (dependentCount > 0) {
            List<ToolDependency> dependents = dependencyRepository.findByDependencyToolId(id);
            List<String> dependentNames = dependents.stream()
                    .map(dep -> dep.getTool().getName())
                    .collect(Collectors.toList());

            throw new ToolHasDependentsException(id, dependentNames);
        }

        // Find existing tool
        Tool tool = toolRepository.findById(id)
                .orElseThrow(() -> new ToolNotFoundException(id));

        // Publish tool deleted event before deletion
        publishToolEvent(tool, ToolEventType.TOOL_DELETED);

        // Delete the tool
        toolRepository.delete(tool);
    }

    /**
     * Publishes a tool event to Kafka.
     *
     * @param tool The tool entity
     * @param eventType The type of event
     */
    public void publishToolEvent(Tool tool, ToolEventType eventType) {
        ToolEvent event = new ToolEvent();
        event.setEventId(UUID.randomUUID().toString());
        event.setEventType(eventType.name());
        event.setTimestamp(LocalDateTime.now());
        event.setToolId(tool.getId());
        event.setName(tool.getName());
        event.setDescription(tool.getDescription());
        event.setActive(tool.isActive());
        event.setVersion(tool.getVersion());
        event.setToolType(tool.getToolType());

        // Add API metadata if this is an API tool
        if (tool.getToolType() == ToolType.API_TOOL) {
            apiToolMetadataRepository.findByToolId(tool.getId())
                    .ifPresent(apiMetadata -> {
                        event.setApiMetadata(apiToolService.mapToApiToolMetadataDTO(apiMetadata));
                    });
        }
        eventPublisher.publish(toolEventsTopic, tool.getId(), event);
    }

    /**
     * Maps a ToolParameterRequest to a ToolParameter entity.
     *
     * @param request The parameter request
     * @return The parameter entity
     */
    private ToolParameter mapToToolParameter(ToolParameterRequest request) {
        ToolParameter parameter = new ToolParameter();
        parameter.setName(request.getName());
        parameter.setDescription(request.getDescription());
        parameter.setParameterType(request.getParameterType());
        parameter.setRequired(request.isRequired());
        parameter.setDefaultValue(request.getDefaultValue());
        parameter.setValidationPattern(request.getValidationPattern());
        parameter.setValidationMessage(request.getValidationMessage());
        parameter.setConditionalOn(request.getConditionalOn());
        parameter.setPriority(request.getPriority() != null ? request.getPriority() : 0);
        parameter.setExamples(request.getExamples());
        parameter.setSuggestionQuery(request.getSuggestionQuery());
        return parameter;
    }

    /**
     * Maps a ToolDependencyRequest to a ToolDependency entity.
     *
     * @param request The dependency request
     * @param tool The tool entity
     * @param dependencyTool The dependency tool entity
     * @return The dependency entity
     */
    private ToolDependency mapToToolDependency(
            ToolDependencyRequest request, Tool tool, Tool dependencyTool) {
        ToolDependency dependency = new ToolDependency();
        dependency.setTool(tool);
        dependency.setDependencyTool(dependencyTool);
        dependency.setDependencyType(request.getDependencyType());
        dependency.setDescription(request.getDescription());
        return dependency;
    }

    /**
     * Maps a ParameterMappingRequest to a ParameterMapping entity.
     *
     * @param request The parameter mapping request
     * @return The parameter mapping entity
     */
    private ParameterMapping mapToParameterMapping(ParameterMappingRequest request) {
        ParameterMapping mapping = new ParameterMapping();
        mapping.setSourceParameter(request.getSourceParameter());
        mapping.setTargetParameter(request.getTargetParameter());
        return mapping;
    }

    /**
     * Maps a ToolExampleRequest to a ToolExample entity.
     *
     * @param request The example request
     * @return The example entity
     */
    private ToolExample mapToToolExample(ToolExampleRequest request) {
        ToolExample example = new ToolExample();
        example.setInputText(request.getInputText());
        try {
            example.setOutputParameters(objectMapper.writeValueAsString(request.getOutputParameters()));
        } catch (Exception e) {
            log.error("Error serializing output parameters", e);
            example.setOutputParameters("{}");
        }
        return example;
    }

    /**
     * Activates or deactivates a tool.
     *
     * @param id The ID of the tool
     * @param active Whether the tool should be active
     * @return The updated tool version
     */
    @Transactional
    public int setToolActive(String id, boolean active) {
        log.info("Setting tool {} active state to: {}", id, active);

        // Find existing tool
        Tool tool = toolRepository.findById(id)
                .orElseThrow(() -> new ToolNotFoundException(id));

        // If already in the desired state, just return
        if (tool.isActive() == active) {
            return tool.getVersion();
        }

        // Update active status
        tool.setActive(active);
        tool.setUpdatedAt(LocalDateTime.now());
        tool.setVersion(tool.getVersion() + 1);

        // Save the updated tool
        Tool savedTool = toolRepository.save(tool);

        // Publish tool updated event
        publishToolEvent(savedTool, ToolEventType.TOOL_UPDATED);

        // Publish graph update event
        graphService.publishGraphUpdate(id);

        return savedTool.getVersion();
    }

    /**
     * Adds a new parameter to a tool.
     *
     * @param id The ID of the tool
     * @param parameterRequest The parameter to add
     * @return The updated tool version
     */
    @Transactional
    public int addToolParameter(String id, ToolParameterRequest parameterRequest) {
        log.info("Adding parameter {} to tool {}", parameterRequest.getName(), id);

        // Find existing tool
        Tool tool = toolRepository.findById(id)
                .orElseThrow(() -> new ToolNotFoundException(id));

        // Check for duplicate parameter name
        if (tool.getParameters().stream().anyMatch(p -> p.getName().equals(parameterRequest.getName()))) {
            throw new DuplicateParameterNameException(parameterRequest.getName());
        }

        // Validate parameter type
        toolValidator.validateParameterType(parameterRequest.getName(), parameterRequest.getParameterType());

        // Validate validation pattern if present
        if (parameterRequest.getValidationPattern() != null && !parameterRequest.getValidationPattern().isEmpty()) {
            toolValidator.validateValidationPattern(parameterRequest.getName(), parameterRequest.getValidationPattern());
        }

        // Create and add the parameter
        ToolParameter parameter = mapToToolParameter(parameterRequest);
        parameter.setId(UUID.randomUUID().toString());
        tool.addParameter(parameter);

        // Update tool metadata
        tool.setUpdatedAt(LocalDateTime.now());
        tool.setVersion(tool.getVersion() + 1);

        // Save the updated tool
        Tool savedTool = toolRepository.save(tool);

        // Publish tool updated event
        publishToolEvent(savedTool, ToolEventType.TOOL_UPDATED);

        return savedTool.getVersion();
    }

    /**
     * Removes a parameter from a tool.
     *
     * @param id The ID of the tool
     * @param parameterId The ID of the parameter to remove
     * @return The updated tool version
     */
    @Transactional
    public int removeToolParameter(String id, String parameterId) {
        log.info("Removing parameter {} from tool {}", parameterId, id);

        // Find existing tool
        Tool tool = toolRepository.findById(id)
                .orElseThrow(() -> new ToolNotFoundException(id));

        // Find the parameter
        ToolParameter parameter = tool.getParameters().stream()
                .filter(p -> p.getId().equals(parameterId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Parameter not found with ID: " + parameterId));

        // Check if parameter is referenced in parameter mappings
        List<ToolDependency> dependencies = dependencyRepository.findByToolId(id);
        for (ToolDependency dependency : dependencies) {
            if (dependency.getParameterMappings().stream()
                    .anyMatch(m -> m.getTargetParameter().equals(parameter.getName()))) {
                throw new IllegalStateException("Cannot remove parameter that is referenced in dependency mappings");
            }
        }

        // Remove the parameter
        tool.getParameters().remove(parameter);

        // Update tool metadata
        tool.setUpdatedAt(LocalDateTime.now());
        tool.setVersion(tool.getVersion() + 1);

        // Save the updated tool
        Tool savedTool = toolRepository.save(tool);

        // Delete the parameter entity
        parameterRepository.deleteById(parameterId);

        // Publish tool updated event
        publishToolEvent(savedTool, ToolEventType.TOOL_UPDATED);

        return savedTool.getVersion();
    }

    /**
     * Adds a new dependency to a tool.
     *
     * @param id The ID of the tool
     * @param dependencyRequest The dependency to add
     * @return The updated tool version
     */
    @Transactional
    public int addToolDependency(String id, ToolDependencyRequest dependencyRequest) {
        log.info("Adding dependency on {} to tool {}", dependencyRequest.getDependencyToolId(), id);

        // Find existing tool
        Tool tool = toolRepository.findById(id)
                .orElseThrow(() -> new ToolNotFoundException(id));

        // Find dependency tool
        Tool dependencyTool = toolRepository.findById(dependencyRequest.getDependencyToolId())
                .orElseThrow(() -> new DependencyToolNotFoundException(dependencyRequest.getDependencyToolId()));

        // Check for duplicate dependency
        if (tool.getDependencies().stream()
                .anyMatch(d -> d.getDependencyTool().getId().equals(dependencyRequest.getDependencyToolId()))) {
            throw new IllegalArgumentException("Tool already has a dependency on: " + dependencyRequest.getDependencyToolId());
        }

        // Validate no cycles
        dependencyValidator.validateNoCycles(id, List.of(dependencyRequest));

        // Create and add the dependency
        ToolDependency dependency = mapToToolDependency(dependencyRequest, tool, dependencyTool);
        dependency.setId(UUID.randomUUID().toString());

        // Add parameter mappings if provided
        if (dependencyRequest.getParameterMappings() != null) {
            // Validate parameter mappings
            dependencyValidator.validateParameterMappings(
                    id,
                    dependencyRequest.getDependencyToolId(),
                    dependencyRequest.getParameterMappings());

            for (ParameterMappingRequest mappingRequest : dependencyRequest.getParameterMappings()) {
                ParameterMapping mapping = mapToParameterMapping(mappingRequest);
                mapping.setId(UUID.randomUUID().toString());
                dependency.addParameterMapping(mapping);
            }
        }

        tool.addDependency(dependency);

        // Update tool metadata
        tool.setUpdatedAt(LocalDateTime.now());
        tool.setVersion(tool.getVersion() + 1);

        // Save the updated tool
        Tool savedTool = toolRepository.save(tool);

        // Publish tool updated event
        publishToolEvent(savedTool, ToolEventType.TOOL_UPDATED);

        // Publish graph update event
        graphService.publishGraphUpdate(id);

        return savedTool.getVersion();
    }

    /**
     * Removes a dependency from a tool.
     *
     * @param id The ID of the tool
     * @param dependencyId The ID of the dependency to remove
     * @return The updated tool version
     */
    @Transactional
    public int removeToolDependency(String id, String dependencyId) {
        log.info("Removing dependency {} from tool {}", dependencyId, id);

        // Find existing tool
        Tool tool = toolRepository.findById(id)
                .orElseThrow(() -> new ToolNotFoundException(id));

        // Find the dependency
        ToolDependency dependency = tool.getDependencies().stream()
                .filter(d -> d.getId().equals(dependencyId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Dependency not found with ID: " + dependencyId));

        // Remove the dependency
        tool.getDependencies().remove(dependency);

        // Update tool metadata
        tool.setUpdatedAt(LocalDateTime.now());
        tool.setVersion(tool.getVersion() + 1);

        // Save the updated tool
        Tool savedTool = toolRepository.save(tool);

        // Delete the dependency entity
        dependencyRepository.deleteById(dependencyId);

        // Publish tool updated event
        publishToolEvent(savedTool, ToolEventType.TOOL_UPDATED);

        // Publish graph update event
        graphService.publishGraphUpdate(id);

        return savedTool.getVersion();
    }

    /**
     * Gets a tool DTO by ID.
     *
     * @param id The ID of the tool
     * @return The tool DTO
     */
    @Transactional(readOnly = true)
    public ToolDTO getToolById(String id) {
        Tool tool = toolRepository.findById(id)
                .orElseThrow(() -> new ToolNotFoundException(id));

        return toolMapper.toDto(tool);
    }

    /**
     * Maps an ApiToolMetadataRequest to an ApiToolMetadata entity.
     *
     * @param request The API metadata request
     * @param tool The tool entity
     * @return The API metadata entity
     */
    private ApiToolMetadata mapToApiToolMetadata(ApiToolMetadataRequest request, Tool tool) {
        ApiToolMetadata metadata = new ApiToolMetadata();
        metadata.setTool(tool);
        updateApiToolMetadata(metadata, request, tool);
        return metadata;
    }

    /**
     * Updates an ApiToolMetadata entity from an ApiToolMetadataRequest.
     *
     * @param metadata The API metadata entity to update
     * @param request The API metadata request
     * @param tool The tool entity
     */
    private void updateApiToolMetadata(ApiToolMetadata metadata, ApiToolMetadataRequest request, Tool tool) {
        if (metadata.getId() == null) {
            metadata.setId(UUID.randomUUID().toString());
        }

        metadata.setTool(tool);
        metadata.setBaseUrl(request.getBaseUrl());
        metadata.setEndpointPath(request.getEndpointPath());
        metadata.setHttpMethod(request.getHttpMethod());
        metadata.setContentType(request.getContentType());
        metadata.setAuthenticationType(request.getAuthenticationType());
        metadata.setRequestTimeoutMs(request.getRequestTimeoutMs());
        metadata.setResponseFormat(request.getResponseFormat());
        metadata.setRateLimitRequests(request.getRateLimitRequests());
        metadata.setRateLimitPeriodSeconds(request.getRateLimitPeriodSeconds());
        metadata.setRetryCount(request.getRetryCount());
        metadata.setRetryDelayMs(request.getRetryDelayMs());

        // Update headers (remove all and re-add)
        metadata.getHeaders().clear();

        if (request.getHeaders() != null) {
            for (ApiHeaderRequest headerReq : request.getHeaders()) {
                ApiHeader header = mapToApiHeader(headerReq);
                header.setId(headerReq.getId() != null ?
                        headerReq.getId() : UUID.randomUUID().toString());
                metadata.addHeader(header);
            }
        }
    }

    /**
     * Maps an ApiHeaderRequest to an ApiHeader entity.
     *
     * @param request The API header request
     * @return The API header entity
     */
    private ApiHeader mapToApiHeader(ApiHeaderRequest request) {
        ApiHeader header = new ApiHeader();
        header.setName(request.getName());
        header.setValue(request.getValue());
        header.setRequired(request.isRequired());
        header.setSensitive(request.isSensitive());
        return header;
    }
}